package com.streamvault.feature.system.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SystemRoutePatternsTest {
    @Test
    fun systemRoutesPreserveCompatibilityStrings() {
        assertThat(SystemRoutePatterns.WELCOME).isEqualTo("welcome")
    }
}
