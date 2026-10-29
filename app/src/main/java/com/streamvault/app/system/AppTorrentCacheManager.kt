package com.streamvault.app.system

import android.content.Context
import com.streamvault.domain.manager.TorrentCacheManager
import com.streamvault.domain.model.TorrentCacheClearResult
import com.streamvault.domain.model.TorrentCacheStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppTorrentCacheManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TorrentCacheManager {
    override suspend fun getStatus(): TorrentCacheStatus =
        TorrentCacheStatus(
            cachedBytes = 0L,
            cacheLimitBytes = 0L,
            availableStorageBytes = null,
            activeSessionCount = 0
        )

    override suspend fun clearCache(): TorrentCacheClearResult =
        TorrentCacheClearResult.CLEARED
}
