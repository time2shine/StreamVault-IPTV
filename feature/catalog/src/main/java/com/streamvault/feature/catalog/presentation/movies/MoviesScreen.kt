package com.streamvault.feature.catalog.presentation.movies

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streamvault.core.navigation.AppDestination
import com.streamvault.core.ui.components.shell.AppMessageState
import com.streamvault.core.ui.components.SearchInput
import com.streamvault.domain.model.Movie
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.feature.catalog.R
import com.streamvault.feature.catalog.api.CatalogNavigationChrome
import com.streamvault.feature.catalog.api.CatalogScaffoldContent
import com.streamvault.feature.catalog.presentation.components.CategoryRow
import com.streamvault.feature.catalog.presentation.components.ContinueWatchingRow
import com.streamvault.feature.catalog.presentation.components.MovieCard

@Composable
fun MoviesScreen(
    onMovieClick: (Movie) -> Unit,
    onContinueWatchingPlay: (PlaybackHistory) -> Unit,
    scaffold: CatalogScaffoldContent,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.selectedCategory != null) {
        viewModel.selectCategory(null)
    }

    scaffold(
        AppDestination.Movies,
        stringResource(R.string.nav_movies),
        null,
        CatalogNavigationChrome.TopBar,
        true,
        true,
        false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading && uiState.shelves.isEmpty() -> {
                    LoadingContent()
                }
                !uiState.hasProviders -> {
                    NoProvidersContent()
                }
                uiState.errorMessage != null && uiState.shelves.isEmpty() -> {
                    ErrorContent(message = uiState.errorMessage ?: "")
                }
                uiState.selectedCategory != null -> {
                    CategoryGridContent(
                        uiState = uiState,
                        onMovieClick = onMovieClick,
                        onBack = { viewModel.selectCategory(null) },
                        onLoadMore = viewModel::loadMoreSelectedCategory
                    )
                }
                else -> {
                    MovieShelvesContent(
                        uiState = uiState,
                        onMovieClick = onMovieClick,
                        onContinueWatchingPlay = onContinueWatchingPlay,
                        onSelectCategory = viewModel::selectCategory
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieShelvesContent(
    uiState: MoviesUiState,
    onMovieClick: (Movie) -> Unit,
    onContinueWatchingPlay: (PlaybackHistory) -> Unit,
    onSelectCategory: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Continue Watching
        if (uiState.continueWatching.isNotEmpty()) {
            item(key = "row_continue_watching") {
                ContinueWatchingRow(
                    title = stringResource(R.string.continue_watching_title),
                    items = uiState.continueWatching,
                    onItemClick = onContinueWatchingPlay
                )
            }
        }

        // 2. Watch History
        if (uiState.watchHistory.isNotEmpty()) {
            item(key = "row_watch_history") {
                ContinueWatchingRow(
                    title = stringResource(R.string.movies_watch_history_title),
                    items = uiState.watchHistory,
                    onItemClick = onContinueWatchingPlay
                )
            }
        }

        // 3. Category Shelves (strictly sorted: Kids songs, Bangla, English, Hindi, Hindi Dubbed, Others, Movie Channels)
        items(
            count = uiState.shelves.size,
            key = { index -> "shelf_${uiState.shelves[index].categoryId}_${uiState.shelves[index].name}" },
            contentType = { "shelf_row" }
        ) { index ->
            val shelf = uiState.shelves[index]
            val isKidsOrChannel = shelf.isKidsSongs || shelf.isMovieChannels
            CategoryRow(
                title = shelf.name,
                items = shelf.movies,
                onSeeAll = { onSelectCategory(shelf.name) },
                keySelector = { movie -> "${shelf.categoryId}_${movie.id}_${movie.streamId}" },
                contentTypeSelector = { "movie_card" }
            ) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) },
                    showRating = !isKidsOrChannel,
                    showNewBadge = if (isKidsOrChannel) false else null
                )
            }
        }
    }
}

@Composable
private fun CategoryGridContent(
    uiState: MoviesUiState,
    onMovieClick: (Movie) -> Unit,
    onBack: () -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()

    // Detect when user scrolls near the end to trigger pagination
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 10
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiState.canLoadMoreSelectedCategory && !uiState.isLoadingSelectedCategory) {
            onLoadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Grid Header with Back Button and Category Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = uiState.selectedCategory.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (uiState.selectedCategoryTotalCount > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${uiState.selectedCategoryTotalCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Movies Grid
        val isGridKidsOrChannel = uiState.selectedCategoryShelf?.let { it.isKidsSongs || it.isMovieChannels } ?: false
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = 136.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = uiState.selectedCategoryMovies,
                key = { movie -> "grid_${movie.id}_${movie.streamId}" }
            ) { movie ->
                val isItemKidsOrChannel = isGridKidsOrChannel ||
                    movie.categoryName?.contains("kids", ignoreCase = true) == true ||
                    movie.categoryName?.contains("channel", ignoreCase = true) == true ||
                    movie.name.startsWith("mov |", ignoreCase = true) ||
                    movie.name.startsWith("mov|", ignoreCase = true)
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) },
                    showRating = !isItemKidsOrChannel,
                    showNewBadge = if (isItemKidsOrChannel) false else null
                )
            }

            if (uiState.isLoadingSelectedCategory) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(
                text = stringResource(R.string.movies_loading),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun NoProvidersContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AppMessageState(
            title = stringResource(R.string.home_add_first_provider),
            subtitle = stringResource(R.string.home_add_first_provider_subtitle)
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AppMessageState(
            title = stringResource(R.string.home_error_load_failed),
            subtitle = message
        )
    }
}
