package com.streamvault.core.navigation

/**
 * Tracks the most recently played channel and movie IDs across the app session.
 * Used for focus/cursor restoration when returning from player back to catalog or library grids.
 */
object LastPlayedMediaTracker {
    @Volatile
    var lastPlayedChannelId: Long? = null

    @Volatile
    var lastPlayedMovieId: Long? = null

    fun recordChannelPlayed(channelId: Long) {
        if (channelId > 0L) {
            lastPlayedChannelId = channelId
        }
    }

    fun recordMoviePlayed(movieId: Long) {
        if (movieId > 0L) {
            lastPlayedMovieId = movieId
        }
    }

    fun clear() {
        lastPlayedChannelId = null
        lastPlayedMovieId = null
    }
}
