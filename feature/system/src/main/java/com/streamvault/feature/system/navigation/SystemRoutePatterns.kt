package com.streamvault.feature.system.navigation

object SystemRoutePatterns {
    const val WELCOME = "welcome"
}

internal fun systemGraphRoutes(): List<String> = listOf(
    SystemRoutePatterns.WELCOME,
)
