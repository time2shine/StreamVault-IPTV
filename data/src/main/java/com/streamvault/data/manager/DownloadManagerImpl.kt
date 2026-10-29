package com.streamvault.data.manager

import com.streamvault.domain.model.DownloadItem
import com.streamvault.domain.model.DownloadRequest
import com.streamvault.domain.model.DownloadStorageConfig
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.DownloadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManagerImpl @Inject constructor() : DownloadManager {

    override fun observeAllDownloads(): Flow<List<DownloadItem>> = flowOf(emptyList())

    override fun observeDownload(id: String): Flow<DownloadItem?> = flowOf(null)

    override fun observeStorageState(): Flow<DownloadStorageConfig> =
        flowOf(DownloadStorageConfig())

    override suspend fun enqueueDownload(request: DownloadRequest): Result<DownloadItem> =
        Result.error("Downloads are not supported")

    override suspend fun resumeDownload(id: String): Result<Unit> = Result.success(Unit)

    override suspend fun recoverInterruptedDownloads(): Result<Int> = Result.success(0)

    override suspend fun pauseDownloadForForegroundServiceTimeout(id: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun cancelDownload(id: String): Result<Unit> = Result.success(Unit)

    override fun onPlaybackStarted() = Unit

    override fun onPlaybackStopped() = Unit

    override suspend fun deleteDownload(id: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateStorageConfig(
        treeUri: String?,
        displayName: String?
    ): Result<DownloadStorageConfig> =
        Result.success(DownloadStorageConfig(treeUri = treeUri, displayName = displayName))
}
