package com.streamvault.app.ui.components.shell

import com.google.common.truth.Truth.assertThat
import com.streamvault.app.navigation.Routes
import com.streamvault.core.ui.components.shell.UiDestination
import com.streamvault.domain.model.AppTopLevelDestination
import com.streamvault.domain.model.CatalogLayout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import org.junit.Test

class AppShellNavigationTest {

    @Test
    fun `destination construction is independent of Activity and repository state`() {
        val result = buildDestinationItems(
            configured = AppTopLevelDestination.defaultOrder,
            layout = CatalogLayout.SPLIT
        )

        assertThat(result.map { it.route })
            .containsExactly(
                Routes.HOME,
                Routes.LIVE_TV,
                Routes.SPORTS,
                Routes.MOVIES,
                Routes.SERIES,
                Routes.EPG,
                Routes.SEARCH,
                Routes.SETTINGS
            )
            .inOrder()
    }

    @Test
    fun selectedNavigationDestinationIdReturnsCurrentRoute() {
        val destinations = listOf(
            UiDestination(Routes.HOME, "Home", Icons.Default.Home)
        )

        assertThat(selectedNavigationDestinationId(Routes.HOME, destinations))
            .isEqualTo(Routes.HOME)
    }

    @Test
    fun splitCatalogPreservesConfiguredMovieAndSeriesDestinations() {
        val result = buildDestinationItems(
            configured = listOf(AppTopLevelDestination.MOVIES, AppTopLevelDestination.SERIES),
            layout = CatalogLayout.SPLIT
        )

        assertThat(result.map { it.route })
            .containsExactly(Routes.MOVIES, Routes.SERIES)
            .inOrder()
    }

    @Test
    fun unifiedCatalogReplacesMovieAndSeriesWithOneVodDestination() {
        val result = buildDestinationItems(
            configured = listOf(
                AppTopLevelDestination.HOME,
                AppTopLevelDestination.MOVIES,
                AppTopLevelDestination.SERIES
            ),
            layout = CatalogLayout.UNIFIED_VOD
        )

        assertThat(result.map { it.route })
            .containsExactly(Routes.HOME, Routes.VOD)
            .inOrder()
    }
}
