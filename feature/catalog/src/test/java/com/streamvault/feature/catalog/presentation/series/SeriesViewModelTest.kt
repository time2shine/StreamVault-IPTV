package com.streamvault.feature.catalog.presentation.series

import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.ContentType
import com.streamvault.domain.model.LegacyProvider
import com.streamvault.domain.model.PlaybackHistory
import com.streamvault.domain.model.ProviderType
import com.streamvault.domain.model.Series
import com.streamvault.domain.repository.PlaybackHistoryRepository
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.SeriesRepository
import com.streamvault.feature.catalog.presentation.CatalogMainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SeriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = CatalogMainDispatcherRule()

    private val providerRepository: ProviderRepository = mock()
    private val seriesRepository: SeriesRepository = mock()
    private val playbackHistoryRepository: PlaybackHistoryRepository = mock()

    private val testProvider = LegacyProvider(
        id = 1L,
        name = "Test Provider",
        type = ProviderType.XTREAM_CODES,
        serverUrl = "http://iptv.example.com"
    )

    @Test
    fun `shelves are built with sorted categories and preview items`() {
        runBlocking {
            whenever(providerRepository.getActiveProvider()).thenReturn(flowOf(testProvider))
            whenever(playbackHistoryRepository.getRecentlyWatchedByProvider(eq(1L), any())).thenReturn(flowOf(emptyList()))

            val catHindi = Category(id = 10L, name = "Hindi Series", type = ContentType.SERIES, count = 20)
            val catBangla = Category(id = 20L, name = "Bangla Series", type = ContentType.SERIES, count = 15)
            val catEnglish = Category(id = 30L, name = "English Series", type = ContentType.SERIES, count = 30)

            whenever(seriesRepository.getCategories(1L)).thenReturn(flowOf(listOf(catHindi, catBangla, catEnglish)))
            whenever(seriesRepository.getCategoryItemCounts(1L)).thenReturn(flowOf(mapOf(10L to 20, 20L to 15, 30L to 30)))

            val seriesB = createSeries(id = 101L, name = "Bangla Show 1", categoryId = 20L)
            val seriesE = createSeries(id = 201L, name = "English Show 1", categoryId = 30L)
            val seriesH = createSeries(id = 301L, name = "Hindi Show 1", categoryId = 10L)

            whenever(seriesRepository.getSeriesByCategoryPreview(1L, 20L, 30)).thenReturn(flowOf(listOf(seriesB)))
            whenever(seriesRepository.getSeriesByCategoryPreview(1L, 30L, 30)).thenReturn(flowOf(listOf(seriesE)))
            whenever(seriesRepository.getSeriesByCategoryPreview(1L, 10L, 30)).thenReturn(flowOf(listOf(seriesH)))

            val viewModel = SeriesViewModel(providerRepository, seriesRepository, playbackHistoryRepository)

            val state = viewModel.uiState.first { !it.isLoading && it.shelves.isNotEmpty() }
            assertThat(state.shelves).hasSize(3)
            // Expected ordering rank: Bangla (20) -> English (30) -> Hindi (40)
            assertThat(state.shelves[0].name).isEqualTo("Bangla Series")
            assertThat(state.shelves[1].name).isEqualTo("English Series")
            assertThat(state.shelves[2].name).isEqualTo("Hindi Series")
        }
    }

    @Test
    fun `playback history is segregated into continue watching and watch history`() {
        runBlocking {
            whenever(providerRepository.getActiveProvider()).thenReturn(flowOf(testProvider))
            whenever(seriesRepository.getCategories(1L)).thenReturn(flowOf(emptyList()))

            val inProgressEpisode = PlaybackHistory(
                id = 1L,
                contentId = 501L,
                contentType = ContentType.SERIES_EPISODE,
                providerId = 1L,
                title = "Awesome Series",
                streamUrl = "http://stream/1",
                resumePositionMs = 600_000L,
                totalDurationMs = 1_800_000L,
                seasonNumber = 1,
                episodeNumber = 3
            )
            val completedEpisode = PlaybackHistory(
                id = 2L,
                contentId = 502L,
                contentType = ContentType.SERIES_EPISODE,
                providerId = 1L,
                title = "Completed Series",
                streamUrl = "http://stream/2",
                resumePositionMs = 0L,
                totalDurationMs = 1_800_000L,
                seasonNumber = 2,
                episodeNumber = 1
            )

            whenever(playbackHistoryRepository.getRecentlyWatchedByProvider(eq(1L), any()))
                .thenReturn(flowOf(listOf(inProgressEpisode, completedEpisode)))

            val viewModel = SeriesViewModel(providerRepository, seriesRepository, playbackHistoryRepository)

            val state = viewModel.uiState.first { it.continueWatching.isNotEmpty() || it.watchHistory.isNotEmpty() }
            assertThat(state.continueWatching).containsExactly(inProgressEpisode)
            assertThat(state.watchHistory).containsExactly(completedEpisode)
        }
    }

    @Test
    fun `category selection and clearing operates properly`() {
        runBlocking {
            whenever(providerRepository.getActiveProvider()).thenReturn(flowOf(testProvider))
            whenever(playbackHistoryRepository.getRecentlyWatchedByProvider(eq(1L), any())).thenReturn(flowOf(emptyList()))

            val cat = Category(id = 55L, name = "K-Drama", type = ContentType.SERIES, count = 10)
            whenever(seriesRepository.getCategories(1L)).thenReturn(flowOf(listOf(cat)))
            whenever(seriesRepository.getCategoryItemCounts(1L)).thenReturn(flowOf(mapOf(55L to 10)))

            val s1 = createSeries(id = 801L, name = "Crash Landing on You", categoryId = 55L)
            val s2 = createSeries(id = 802L, name = "Vincenzo", categoryId = 55L)

            whenever(seriesRepository.getSeriesByCategoryPreview(1L, 55L, 30)).thenReturn(flowOf(listOf(s1, s2)))
            whenever(seriesRepository.getSeriesByCategoryPage(1L, 55L, 48, 0)).thenReturn(flowOf(listOf(s1, s2)))

            val viewModel = SeriesViewModel(providerRepository, seriesRepository, playbackHistoryRepository)
            viewModel.uiState.first { it.shelves.isNotEmpty() }

            viewModel.selectCategory("K-Drama")
            val gridState = viewModel.uiState.first { it.selectedCategory == "K-Drama" && !it.isLoadingSelectedCategory }
            assertThat(gridState.selectedCategorySeries).containsExactly(s1, s2)

            viewModel.selectCategory(null)
            val clearedState = viewModel.uiState.first { it.selectedCategory == null }
            assertThat(clearedState.selectedCategorySeries).isEmpty()
        }
    }

    private fun createSeries(id: Long, name: String, categoryId: Long): Series = Series(
        id = id,
        name = name,
        categoryId = categoryId,
        providerId = 1L
    )
}
