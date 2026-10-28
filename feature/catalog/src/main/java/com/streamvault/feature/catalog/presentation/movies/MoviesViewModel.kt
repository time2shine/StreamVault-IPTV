package com.streamvault.feature.catalog.presentation.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.PlaybackHistoryRepository
import com.streamvault.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import javax.inject.Inject

data class MovieShelf(
    val categoryId: Long,
    val name: String,
    val movies: List<Movie>,
    val totalCount: Int,
    val isKidsSongs: Boolean = false,
    val isMovieChannels: Boolean = false
)

data class MoviesUiState(
    val isLoading: Boolean = false,
    val hasProviders: Boolean = true,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val continueWatching: List<PlaybackHistory> = emptyList(),
    val watchHistory: List<PlaybackHistory> = emptyList(),
    val shelves: List<MovieShelf> = emptyList(),
    val selectedCategory: String? = null,
    val selectedCategoryShelf: MovieShelf? = null,
    val selectedCategoryMovies: List<Movie> = emptyList(),
    val isLoadingSelectedCategory: Boolean = false,
    val canLoadMoreSelectedCategory: Boolean = false,
    val selectedCategoryTotalCount: Int = 0,
    val selectedCategoryPage: Int = 1,
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val movieRepository: MovieRepository,
    private val playbackHistoryRepository: PlaybackHistoryRepository
) : ViewModel() {

    companion object {
        private val logger = Logger.getLogger("MoviesViewModel")
        private const val PREVIEW_ROW_LIMIT = 30
        private const val GRID_PAGE_SIZE = 48
    }

    private val _uiState = MutableStateFlow(MoviesUiState(isLoading = true))
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private var activeProviderId: Long = 0L
    private var loadShelvesJob: Job? = null
    private var searchJob: Job? = null

    init {
        observeActiveProvider()
    }

    private fun observeActiveProvider() {
        viewModelScope.launch {
            providerRepository.getActiveProvider()
                .distinctUntilChanged()
                .collectLatest { provider ->
                    if (provider == null) {
                        val allProviders = providerRepository.getProviders().first()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasProviders = allProviders.isNotEmpty(),
                                shelves = emptyList(),
                                continueWatching = emptyList(),
                                watchHistory = emptyList()
                            )
                        }
                        return@collectLatest
                    }

                    activeProviderId = provider.id
                    _uiState.update { it.copy(hasProviders = true, isLoading = it.shelves.isEmpty()) }

                    // Launch concurrent loaders for history and movie shelves
                    launch { observeHistory(provider.id) }
                    launch { loadMovieShelves(provider.id) }
                }
        }
    }

    fun resetPreviewRowsForScreenEntry() {
        if (activeProviderId > 0L && _uiState.value.shelves.isEmpty()) {
            loadMovieShelves(activeProviderId)
        }
    }

    private fun loadMovieShelves(providerId: Long) {
        loadShelvesJob?.cancel()
        loadShelvesJob = viewModelScope.launch {
            try {
                movieRepository.getCategories(providerId).collectLatest { allCategories ->
                    if (allCategories.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, shelves = emptyList()) }
                        return@collectLatest
                    }

                    val counts = try {
                        movieRepository.getCategoryItemCounts(providerId).first()
                    } catch (e: Exception) {
                        emptyMap()
                    }

                    // Sort categories according to user's strict requirement:
                    // 1. Kids songs -> 2. Bangla -> 3. English -> 4. Hindi -> 5. Hindi Dubbed -> 6. Others -> 7. Movie Channels
                    val sortedCategories = allCategories.sortedWith(
                        compareBy<com.streamvault.domain.model.Category> { categoryOrderRank(it.name) }
                            .thenBy { it.providerOrder }
                            .thenBy { it.name }
                    )

                    // Build preview shelves concurrently
                    val builtShelves = withContext(Dispatchers.IO) {
                        sortedCategories.mapNotNull { category ->
                            val catId = category.id
                            val rawMovies = try {
                                movieRepository.getMoviesByCategoryPreview(providerId, catId, PREVIEW_ROW_LIMIT).first()
                            } catch (e: Exception) {
                                emptyList()
                            }
                            if (rawMovies.isEmpty()) return@mapNotNull null

                            val isKids = isKidsCategory(category.name)
                            val isChannels = isMovieChannelsCategory(category.name)
                            val sortedMovies = sortCategoryMovies(rawMovies, isChannels)
                            val totalCount = counts[catId] ?: sortedMovies.size

                            MovieShelf(
                                categoryId = catId,
                                name = category.name,
                                movies = sortedMovies,
                                totalCount = totalCount,
                                isKidsSongs = isKids,
                                isMovieChannels = isChannels
                            )
                        }
                    }

                    _uiState.update {
                        it.copy(
                            shelves = builtShelves,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warning("Error loading movie shelves: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun observeHistory(providerId: Long) {
        playbackHistoryRepository.getRecentlyWatchedByProvider(providerId, limit = 50)
            .collectLatest { rawHistory ->
                val movieHistory = rawHistory.filter { it.contentType == ContentType.MOVIE && it.contentId > 0L }
                    .distinctBy { it.contentId }

                // Exclude kids songs and movie channels from history so loops/tests don't pollute
                val cleanHistory = movieHistory.filterNot { history ->
                    isExcludedFromHistory(history.title)
                }

                val continueList = cleanHistory.filter { history ->
                    history.resumePositionMs > 0 &&
                        (history.totalDurationMs <= 0 || history.resumePositionMs < (history.totalDurationMs * 0.95).toLong())
                }.take(20)

                val continueIds = continueList.map { it.contentId }.toSet()
                val recentHistoryList = cleanHistory.filter { it.contentId !in continueIds }.take(20)

                _uiState.update {
                    it.copy(
                        continueWatching = continueList,
                        watchHistory = recentHistoryList
                    )
                }
            }
    }

    fun selectCategory(categoryName: String?) {
        if (categoryName == null) {
            _uiState.update {
                it.copy(
                    selectedCategory = null,
                    selectedCategoryShelf = null,
                    selectedCategoryMovies = emptyList(),
                    selectedCategoryPage = 1,
                    canLoadMoreSelectedCategory = false
                )
            }
            return
        }

        val shelf = _uiState.value.shelves.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
        _uiState.update {
            it.copy(
                selectedCategory = categoryName,
                selectedCategoryShelf = shelf,
                isLoadingSelectedCategory = true,
                selectedCategoryPage = 1
            )
        }

        val catId = shelf?.categoryId ?: return
        viewModelScope.launch {
            try {
                val paged = withContext(Dispatchers.IO) {
                    movieRepository.getMoviesByCategoryPage(activeProviderId, catId, GRID_PAGE_SIZE, 0).first()
                }
                val isChannels = shelf.isMovieChannels
                val sorted = sortCategoryMovies(paged, isChannels)
                _uiState.update {
                    it.copy(
                        selectedCategoryMovies = sorted,
                        isLoadingSelectedCategory = false,
                        canLoadMoreSelectedCategory = paged.size >= GRID_PAGE_SIZE,
                        selectedCategoryPage = 1,
                        selectedCategoryTotalCount = shelf.totalCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSelectedCategory = false) }
            }
        }
    }

    fun loadMoreSelectedCategory() {
        val state = _uiState.value
        val shelf = state.selectedCategoryShelf ?: return
        if (state.isLoadingSelectedCategory || !state.canLoadMoreSelectedCategory) return

        val nextPage = state.selectedCategoryPage + 1
        val offset = state.selectedCategoryMovies.size

        _uiState.update { it.copy(isLoadingSelectedCategory = true) }
        viewModelScope.launch {
            try {
                val nextChunk = withContext(Dispatchers.IO) {
                    movieRepository.getMoviesByCategoryPage(activeProviderId, shelf.categoryId, GRID_PAGE_SIZE, offset).first()
                }
                val isChannels = shelf.isMovieChannels
                val sortedChunk = sortCategoryMovies(nextChunk, isChannels)
                _uiState.update {
                    it.copy(
                        selectedCategoryMovies = (it.selectedCategoryMovies + sortedChunk).distinctBy { m -> m.id },
                        selectedCategoryPage = nextPage,
                        canLoadMoreSelectedCategory = nextChunk.size >= GRID_PAGE_SIZE,
                        isLoadingSelectedCategory = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingSelectedCategory = false) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = withContext(Dispatchers.IO) {
                    movieRepository.searchMovies(activeProviderId, query).first()
                }
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun verifyPin(pin: String): Boolean = true
    fun unlockCategory(category: com.streamvault.domain.model.Category) {}
    fun saveReorder() {}
    fun exitCategoryReorderMode() {}

    // --- Helpers ---

    private fun categoryOrderRank(name: String): Int {
        val lower = name.trim().lowercase()
        return when {
            lower.contains("kids") || lower.contains("song") -> 10
            lower == "bangla" || lower == "bengali" -> 20
            lower == "english" -> 30
            lower == "hindi" -> 40
            lower == "hindi dubbed" || lower == "dubbed" -> 50
            lower == "others" || lower == "other" -> 60
            lower.contains("channel") -> 80
            else -> 70
        }
    }

    private fun isKidsCategory(name: String): Boolean {
        val lower = name.trim().lowercase()
        return lower.contains("kids") || lower.contains("song")
    }

    private fun isMovieChannelsCategory(name: String): Boolean {
        val lower = name.trim().lowercase()
        return lower.contains("channel")
    }

    private fun isExcludedFromHistory(title: String): Boolean {
        val lower = title.lowercase()
        return lower.contains("kids") || lower.contains("song") ||
            lower.contains("channel") || lower.startsWith("mov |") || lower.startsWith("mov|")
    }

    private fun extractReleaseYear(movie: Movie): Int {
        movie.year?.toIntOrNull()?.let { return it }
        val match = Regex("""\b(19\d\d|20\d\d)\b""").find(movie.name)
        return match?.value?.toIntOrNull() ?: 0
    }

    private fun movieChannelTier(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("bangla") || lower.contains("bengali") || lower.contains("jalsha") || lower.contains("toffee") -> 0
            lower.contains("hindi") || lower.contains("india") || lower.contains("b4u") || lower.contains("zee") ||
                lower.contains("star") || lower.contains("colors") || lower.contains("sony") || lower.contains("cinema") ||
                lower.contains("bollywood") || lower.contains("abzy") || lower.contains("goldmines") -> 1
            else -> 2
        }
    }

    private fun sortCategoryMovies(movies: List<Movie>, isMovieChannel: Boolean): List<Movie> {
        if (isMovieChannel) {
            return movies.map { movie ->
                val stripped = movie.name.replace(Regex("""^(?i)MOV\s*\|\s*"""), "").trim()
                if (stripped != movie.name) movie.copy(name = stripped) else movie
            }.sortedWith(
                compareBy<Movie> { movieChannelTier(it.name) }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }

        val nowSec = System.currentTimeMillis() / 1000L
        val thirtyDaysSec = 30L * 24 * 3600L
        val cutoffSec = nowSec - thirtyDaysSec
        val cutoffMillis = System.currentTimeMillis() - (30L * 24 * 3600 * 1000L)

        return movies.sortedWith { a, b ->
            val aAdded = a.addedAt
            val bAdded = b.addedAt
            val aIsRecent = (aAdded in cutoffSec..100_000_000_000L) || (aAdded >= cutoffMillis)
            val bIsRecent = (bAdded in cutoffSec..100_000_000_000L) || (bAdded >= cutoffMillis)

            if (aIsRecent != bIsRecent) {
                return@sortedWith if (aIsRecent) -1 else 1
            }
            if (aIsRecent && bIsRecent) {
                val cmp = bAdded.compareTo(aAdded)
                if (cmp != 0) return@sortedWith cmp
            }

            val aYear = extractReleaseYear(a)
            val bYear = extractReleaseYear(b)
            if (aYear != bYear) {
                return@sortedWith bYear.compareTo(aYear)
            }

            a.name.compareTo(b.name, ignoreCase = true)
        }
    }
}
