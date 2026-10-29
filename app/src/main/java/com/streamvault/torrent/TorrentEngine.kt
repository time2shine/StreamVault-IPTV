package com.streamvault.torrent

import android.content.Context
import android.os.StatFs
import com.streamvault.domain.model.TorrentCacheClearResult
import com.streamvault.domain.model.TorrentCacheStatus
import java.io.Closeable
import java.io.OutputStream

internal data class PreparedTorrent(
    val token: String,
    val outputUrl: String,
    val displayName: String
)

internal data class TorrentStatusSnapshot(
    val activeCount: Int,
    val downloadBytesPerSecond: Long,
    val uploadBytesPerSecond: Long,
    val peers: Int,
    val label: String,
    val activeSessions: List<String>
) {
    val summary: String
        get() = "$label · 0 peers"
}

internal data class TorrentFileInfo(
    val name: String,
    val size: Long,
    val contentType: String
)

internal interface TorrentFileStreamer {
    fun fileInfo(token: String): TorrentFileInfo
    fun stream(token: String, start: Long, endInclusive: Long, output: OutputStream)
}

/**
 * Lightweight safe stub replacing native libtorrent engine.
 * Eliminates ~23MB native .so binaries and FrostWire background P2P threads on Android TV.
 */
internal class TorrentEngine(
    context: Context,
    private val preferences: TorrentPreferences
) : Closeable, TorrentFileStreamer {
    private val appContext = context.applicationContext
    @Volatile private var loopbackBaseUrl: String? = null

    fun setLoopbackBaseUrl(url: String) {
        loopbackBaseUrl = url
    }

    fun isIdle(): Boolean = true

    fun expireIdleSessions() {}

    fun statusSnapshot(): TorrentStatusSnapshot = TorrentStatusSnapshot(
        activeCount = 0,
        downloadBytesPerSecond = 0L,
        uploadBytesPerSecond = 0L,
        peers = 0,
        label = "Idle",
        activeSessions = emptyList()
    )

    fun prepare(rawMagnet: String, forDownload: Boolean = false): PreparedTorrent {
        throw UnsupportedOperationException("BitTorrent P2P playback is disabled in this TV build")
    }

    fun stop(outputUrl: String) {}

    fun cacheStatus(): TorrentCacheStatus {
        val root = TorrentStorageLocations.torrentDirectory(
            appContext,
            preferences.storageLocationIndex
        )
        return TorrentStorageLocations.withCacheLock {
            TorrentCacheStatus(
                cachedBytes = 0L,
                cacheLimitBytes = TorrentCachePolicy.MAX_CACHE_BYTES,
                availableStorageBytes = runCatching { StatFs(root.absolutePath).availableBytes }.getOrNull(),
                activeSessionCount = 0
            )
        }
    }

    fun clearDownloadedData(): TorrentCacheClearResult {
        val root = TorrentStorageLocations.torrentDirectory(
            appContext,
            preferences.storageLocationIndex
        )
        return TorrentStorageLocations.withCacheLock {
            if (TorrentStorageLocations.clearSessionDirectories(root)) {
                TorrentCacheClearResult.CLEARED
            } else {
                TorrentCacheClearResult.FAILED
            }
        }
    }

    override fun fileInfo(token: String): TorrentFileInfo {
        throw UnsupportedOperationException("BitTorrent P2P playback is disabled")
    }

    override fun stream(token: String, start: Long, endInclusive: Long, output: OutputStream) {
        throw UnsupportedOperationException("BitTorrent P2P playback is disabled")
    }

    override fun close() {}
}

internal data class ByteRange(val start: Long, val endInclusive: Long)

internal class StreamHttpException(val statusCode: Int, override val message: String) : Exception(message)

