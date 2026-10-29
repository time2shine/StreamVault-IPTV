package com.streamvault.app.custom

/**
 * Custom configuration constants for this forked build.
 * Kept isolated from upstream code to avoid merge conflicts.
 */
object ForkConfig {
    /** Xtream Codes Edge API endpoint hosted on Cloudflare Worker */
    const val DEFAULT_XTREAM_URL = "https://streamvault-api.allmybooks1991.workers.dev"
    const val DEFAULT_XTREAM_USERNAME = "rokon"
    const val DEFAULT_XTREAM_PASSWORD = "123"
    const val DEFAULT_XTREAM_NAME = "Rokon IPTV"

    /** Default EPG source URL */
    const val DEFAULT_EPG_URL = "https://epgshare01.online/epgshare01/epg_ripper_IN1.xml.gz"
    const val DEFAULT_EPG_NAME = "India & Sports Guide"

    /** Legacy playlist kept for reference or fallback */
    const val DEFAULT_M3U_URL = "https://tinyurl.com/rokon-iptv3"
    const val DEFAULT_M3U_NAME = "Rokon IPTV"
    const val DEFAULT_M3U_VOD_CLASSIFICATION_ENABLED = true

    /** Kept for backward compatibility if referenced elsewhere */
    const val BOOTSTRAP_M3U_URL = DEFAULT_M3U_URL
    const val BOOTSTRAP_M3U_NAME = DEFAULT_M3U_NAME
    const val BOOTSTRAP_M3U_VOD_CLASSIFICATION_ENABLED = DEFAULT_M3U_VOD_CLASSIFICATION_ENABLED
    const val FULL_M3U_URL = DEFAULT_M3U_URL
    const val FULL_M3U_NAME = DEFAULT_M3U_NAME
    const val FULL_M3U_VOD_CLASSIFICATION_ENABLED = DEFAULT_M3U_VOD_CLASSIFICATION_ENABLED

    /** Interval for daily background catalog sync: 24 hours */
    const val DAILY_SYNC_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L

    /** Initial delay after startup before running background sync (ensures snappy initial UI/playback) */
    const val INITIAL_BACKGROUND_SYNC_DELAY_MILLIS = 8_000L
}
