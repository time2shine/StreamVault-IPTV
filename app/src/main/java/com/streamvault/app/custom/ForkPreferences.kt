package com.streamvault.app.custom

import android.content.Context
import android.content.SharedPreferences

/**
 * Tracks persistence flags specific to custom fork features.
 */
object ForkPreferences {
    private const val PREFS_NAME = "streamvault_custom_fork_prefs"
    private const val KEY_DEFAULT_PROVIDER_SEEDED = "default_m3u_provider_seeded"
    private const val KEY_FULL_LIBRARY_LOADED = "full_library_loaded"
    private const val KEY_LAST_DAILY_SYNC_MILLIS = "last_daily_sync_millis"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasEverSeededDefaultProvider(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEFAULT_PROVIDER_SEEDED, false)
    }

    fun setHasEverSeededDefaultProvider(context: Context, seeded: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEFAULT_PROVIDER_SEEDED, seeded).apply()
    }

    fun isFullLibraryLoaded(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FULL_LIBRARY_LOADED, false)
    }

    fun setFullLibraryLoaded(context: Context, loaded: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FULL_LIBRARY_LOADED, loaded).apply()
    }

    fun getLastDailySyncMillis(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_DAILY_SYNC_MILLIS, 0L)
    }

    fun setLastDailySyncMillis(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_DAILY_SYNC_MILLIS, timestamp).apply()
    }
}
