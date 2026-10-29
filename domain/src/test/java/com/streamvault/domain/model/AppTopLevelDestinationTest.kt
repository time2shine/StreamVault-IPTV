package com.streamvault.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppTopLevelDestinationTest {

    @Test
    fun `purged dead endpoints return null from storage`() {
        assertThat(AppTopLevelDestination.fromStorage("plugins")).isNull()
        assertThat(AppTopLevelDestination.fromStorage("addons")).isNull()
        assertThat(AppTopLevelDestination.fromStorage("downloads")).isNull()
        assertThat(AppTopLevelDestination.fromStorage("extensions")).isNull()
    }

    @Test
    fun `valid entries normalize and always include settings`() {
        assertThat(
            AppTopLevelDestination.normalizeForStorage(
                listOf(
                    AppTopLevelDestination.HOME,
                    AppTopLevelDestination.LIVE_TV
                )
            )
        ).containsExactly(
            AppTopLevelDestination.HOME,
            AppTopLevelDestination.LIVE_TV,
            AppTopLevelDestination.SETTINGS
        ).inOrder()
    }
}
