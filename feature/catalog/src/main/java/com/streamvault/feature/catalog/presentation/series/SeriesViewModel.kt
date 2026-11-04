package com.streamvault.feature.catalog.presentation.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamvault.data.sync.ProviderSyncCommands
import com.streamvault.data.sync.ProviderSyncStateSource
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.model.Series
import com.streamvault.domain.model.SyncState
import com.streamvault.domain.repository.PlaybackHistoryRepository
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import javax.inject.Inject

data class SeriesShelf(
    val categoryId: Long,
    val name: String,
    val series: List<Series>,
    val totalCount: Int
)

data class SeriesUiState(
    val isLoading: Boolean = false,
    val hasProviders: Boolean = true,
    val isSyncing: Boolean = false,
    val loadedCount: Int = 0,
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val continueWatching: List<PlaybackHistory> = emptyList(),
    val watchHistory: List<PlaybackHistory> = emptyList(),
    val shelves: List<SeriesShelf> = emptyList(),
    val selectedCategory: String? = null,
    val selectedCategoryShelf: SeriesShelf? = null,
    val selectedCategorySeries: List<Series> = emptyList(),
    val isLoadingSelectedCategory: Boolean = false,
    val canLoadMoreSelectedCategory: Boolean = false,
    val selectedCategoryTotalCount: Int = 0,
    val selectedCategoryPage: Int = 1,
    val searchQuery: String = "",
    val searchResults: List<Series> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val seriesRepository: SeriesRepository,
    private val playbackHistoryRepository: PlaybackHistoryRepository,
    private val syncManager: ProviderSyncStateSource? = null,
    private val syncCommands: ProviderSyncCommands? = null
) : ViewModel() {

    companion object {
        private val logger = Logger.getLogger("SeriesViewModel")
        private const val PREVIEW_ROW_LIMIT = 30
        private const val GRID_PAGE_SIZE = 48
    }

    private val _uiState = MutableStateFlow(SeriesUiState(isLoading = true))
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private var activeProviderId: Long = 0L
    private var loadShelvesJob: Job? = null
    private var searchJob: Job? = null
    private var isInitialSyncPending: Boolean = false
    private var initialSyncTriggered: Boolean = false

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
                                isSyncing = false,
                                hasProviders = allProviders.isNotEmpty(),
                                shelves = emptyList(),
                                continueWatching = emptyList(),
                                watchHistory = emptyList()
                            )
                        }
                        return@collectLatest
                    }

                    if (activeProviderId != provider.id) {
                        activeProviderId = provider.id
                        initialSyncTriggered = false
                        isInitialSyncPending = false
                    }
                    val isSyncActive = syncManager?.currentSyncState(provider.id)?.isSyncing == true
                    _uiState.update {
                        it.copy(
                            hasProviders = true,
                            isSyncing = isSyncActive || isInitialSyncPending,
                            isLoading = it.shelves.isEmpty()
                        )
                    }

                    // Launch concurrent loaders for history, sync state, library count, and series shelves
                    launch { observeHistory(provider.id) }
                    launch { observeSyncState(provider.id) }
                    launch { observeLibraryCount(provider.id) }
                    launch { loadSeriesShelves(provider.id) }
                }
        }
    }

    private suspend fun observeLibraryCount(providerId: Long) {
        seriesRepository.getLibraryCount(providerId).collectLatest { count ->
            _uiState.update { current ->
                current.copy(loadedCount = count)
            }
        }
    }

    private suspend fun observeSyncState(providerId: Long) {
        val syncFlow = syncManager?.syncStateForProvider(providerId) ?: return
        syncFlow.collectLatest { syncState ->
            val syncing = syncState.isSyncing
            _uiState.update { current ->
                val shouldKeepLoading = current.shelves.isEmpty() && (syncing || isInitialSyncPending)
                current.copy(
                    isSyncing = syncing || isInitialSyncPending,
                    isLoading = if (current.shelves.isNotEmpty()) false else shouldKeepLoading
                )
            }
        }
    }

    fun resetPreviewRowsForScreenEntry() {
        if (activeProviderId > 0L && _uiState.value.shelves.isEmpty()) {
            loadSeriesShelves(activeProviderId)
        }
    }

    fun refresh() {
        if (activeProviderId > 0L) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, isSyncing = true, errorMessage = null) }
                launch(Dispatchers.IO) {
                    val result = runCatching {
                        syncCommands?.processQueuedXtreamIndexJobs(
                            providerId = activeProviderId,
                            section = ContentType.SERIES,
                            force = true
                        ) ?: providerRepository.refreshProviderData(activeProviderId, force = true)
                    }.getOrNull()

                    val count = runCatching { seriesRepository.getLibraryCount(activeProviderId).first() }.getOrDefault(0)
                    if (result is com.streamvault.domain.model.Result.Error && count == 0) {
                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
                                isSyncing = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
                loadSeriesShelves(activeProviderId)
            }
        }
    }

    private fun loadSeriesShelves(providerId: Long) {
        loadShelvesJob?.cancel()
        loadShelvesJob = viewModelScope.launch {
            try {
                // Eagerly trigger VOD indexing if library is currently empty
                val libraryCount = runCatching { seriesRepository.getLibraryCount(providerId).first() }.getOrDefault(0)

                if (libraryCount == 0 && !initialSyncTriggered) {
                    initialSyncTriggered = true
                    isInitialSyncPending = true
                    _uiState.update { it.copy(isLoading = true, isSyncing = true, errorMessage = null) }
                    launch(Dispatchers.IO) {
                        val result = runCatching {
                            syncCommands?.processQueuedXtreamIndexJobs(
                                providerId = providerId,
                                section = ContentType.SERIES,
                                force = true
                            ) ?: providerRepository.refreshProviderData(providerId, force = true)
                        }.getOrNull()

                        isInitialSyncPending = false
                        val active = syncManager?.currentSyncState(providerId)?.isSyncing == true
                        val count = runCatching { seriesRepository.getLibraryCount(providerId).first() }.getOrDefault(0)

                        if (result is com.streamvault.domain.model.Result.Error && count == 0) {
                            _uiState.update { current ->
                                current.copy(
                                    isLoading = false,
                                    isSyncing = false,
                                    errorMessage = result.message
                                )
                            }
                        } else {
                            _uiState.update { current ->
                                current.copy(
                                    isLoading = current.shelves.isEmpty() && (active || isInitialSyncPending),
                                    isSyncing = active || isInitialSyncPending
                                )
                            }
                        }
                    }
                }

                seriesRepository.getCategories(providerId).collectLatest { allCategories ->
                    val isSyncActive = syncManager?.currentSyncState(providerId)?.isSyncing == true
                    val currentCount = runCatching { seriesRepository.getLibraryCount(providerId).first() }.getOrDefault(0)

                    if (allCategories.isEmpty()) {
                        val stillLoading = isSyncActive || isInitialSyncPending || (currentCount == 0 && isInitialSyncPending)
                        _uiState.update {
                            it.copy(
                                isLoading = stillLoading,
                                isSyncing = isSyncActive || isInitialSyncPending,
                                shelves = emptyList(),
                                errorMessage = null
                            )
                        }
                        return@collectLatest
                    }

                    val counts = try {
                        seriesRepository.getCategoryItemCounts(providerId).first()
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        emptyMap()
                    }

                    val sortedCategories = allCategories.sortedWith(
                        compareBy<Category> { categoryOrderRank(it.name) }
                            .thenBy { it.providerOrder }
                            .thenBy { it.name }
                    )

                    val builtShelves = withContext(Dispatchers.IO) {
                        sortedCategories.mapNotNull { category ->
                            val catId = category.id
                            val rawSeries = try {
                                seriesRepository.getSeriesByCategoryPreview(providerId, catId, PREVIEW_ROW_LIMIT).first()
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                emptyList()
                            }
                            if (rawSeries.isEmpty()) return@mapNotNull null

                            val sortedSeries = sortCategorySeries(rawSeries)
                            val totalCount = counts[catId] ?: sortedSeries.size

                            SeriesShelf(
                                categoryId = catId,
                                name = category.name,
                                series = sortedSeries,
                                totalCount = totalCount
                            )
                        }
                    }

                    val activeNow = syncManager?.currentSyncState(providerId)?.isSyncing == true
                    val stillLoading = builtShelves.isEmpty() && (activeNow || isInitialSyncPending || (currentCount == 0 && isInitialSyncPending))

                    _uiState.update {
                        it.copy(
                            shelves = builtShelves,
                            isLoading = stillLoading,
                            isSyncing = activeNow || isInitialSyncPending,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.warning("Error loading series shelves: ${e.message}")
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private suspend fun observeHistory(providerId: Long) {
        playbackHistoryRepository.getRecentlyWatchedByProvider(providerId, limit = 50)
            .collectLatest { rawHistory ->
                val seriesHistory = rawHistory.filter {
                    (it.contentType == ContentType.SERIES || it.contentType == ContentType.SERIES_EPISODE) && it.contentId > 0L
                }.distinctBy { it.contentId }

                val continueList = seriesHistory.filter { history ->
                    history.resumePositionMs > 0 &&
                        (history.totalDurationMs <= 0 || history.resumePositionMs < (history.totalDurationMs * 0.95).toLong())
                }.take(20)

                val continueIds = continueList.map { it.contentId }.toSet()
                val recentHistoryList = seriesHistory.filter { it.contentId !in continueIds }.take(20)

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
                    selectedCategorySeries = emptyList(),
                    isLoadingSelectedCategory = false,
                    canLoadMoreSelectedCategory = false,
                    selectedCategoryPage = 1
                )
            }
            return
        }

        val shelf = _uiState.value.shelves.firstOrNull { it.name == categoryName }
        _uiState.update {
            it.copy(
                selectedCategory = categoryName,
                selectedCategoryShelf = shelf,
                selectedCategorySeries = shelf?.series.orEmpty(),
                isLoadingSelectedCategory = true,
                selectedCategoryPage = 1
            )
        }

        val catId = shelf?.categoryId ?: return
        viewModelScope.launch {
            try {
                val paged = withContext(Dispatchers.IO) {
                    seriesRepository.getSeriesByCategoryPage(activeProviderId, catId, GRID_PAGE_SIZE, 0).first()
                }
                val sorted = sortCategorySeries(paged)
                _uiState.update {
                    it.copy(
                        selectedCategorySeries = sorted,
                        isLoadingSelectedCategory = false,
                        canLoadMoreSelectedCategory = paged.size >= GRID_PAGE_SIZE,
                        selectedCategoryPage = 1,
                        selectedCategoryTotalCount = shelf.totalCount
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoadingSelectedCategory = false) }
            }
        }
    }

    fun loadMoreSelectedCategory() {
        val state = _uiState.value
        val shelf = state.selectedCategoryShelf ?: return
        if (state.isLoadingSelectedCategory || !state.canLoadMoreSelectedCategory) return

        val nextPage = state.selectedCategoryPage + 1
        val offset = state.selectedCategorySeries.size

        _uiState.update { it.copy(isLoadingSelectedCategory = true) }
        viewModelScope.launch {
            try {
                val nextChunk = withContext(Dispatchers.IO) {
                    seriesRepository.getSeriesByCategoryPage(activeProviderId, shelf.categoryId, GRID_PAGE_SIZE, offset).first()
                }
                val sortedChunk = sortCategorySeries(nextChunk)
                _uiState.update {
                    it.copy(
                        selectedCategorySeries = (it.selectedCategorySeries + sortedChunk).distinctBy { s -> s.id },
                        selectedCategoryPage = nextPage,
                        canLoadMoreSelectedCategory = nextChunk.size >= GRID_PAGE_SIZE,
                        isLoadingSelectedCategory = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
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
                    seriesRepository.searchSeries(activeProviderId, query).first()
                }
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun verifyPin(pin: String): Boolean = true
    fun unlockCategory(category: Category) {}
    fun saveReorder() {}
    fun exitCategoryReorderMode() {}

    // --- Helpers ---

    private fun categoryOrderRank(name: String): Int {
        val lower = name.trim().lowercase()
        return when {
            lower.contains("featured") || lower.contains("popular") || lower.contains("trending") -> 10
            lower.contains("bangla") || lower.contains("bengali") -> 20
            lower.contains("english") || lower.contains("us") || lower.contains("uk") || lower.contains("hollywood") -> 30
            lower.contains("hindi dubbed") || lower.contains("dubbed") -> 50
            lower.contains("hindi") || lower.contains("bollywood") || lower.contains("web series") -> 40
            lower.contains("korean") || lower.contains("k-drama") || lower.contains("kdrama") || lower.contains("asian") -> 60
            lower.contains("anime") || lower.contains("animation") || lower.contains("cartoon") -> 70
            lower == "others" || lower == "other" -> 90
            else -> 80
        }
    }

    private fun extractReleaseYear(series: Series): Int {
        series.releaseDate?.take(4)?.toIntOrNull()?.let { return it }
        val match = Regex("""\b(19\d\d|20\d\d)\b""").find(series.name)
        return match?.value?.toIntOrNull() ?: 0
    }

    private fun sortCategorySeries(seriesList: List<Series>): List<Series> {
        val nowSec = System.currentTimeMillis() / 1000L
        val thirtyDaysSec = 30L * 24 * 3600L
        val cutoffSec = nowSec - thirtyDaysSec

        return seriesList.sortedWith(
            compareByDescending<Series> { it.isNew }
                .thenByDescending { extractReleaseYear(it) }
                .thenByDescending { it.rating }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle }
        )
    }
}
