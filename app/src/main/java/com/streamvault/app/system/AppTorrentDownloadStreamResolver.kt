package com.streamvault.app.system

import android.content.Context
import com.streamvault.domain.manager.TorrentDownloadStreamResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppTorrentDownloadStreamResolver @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TorrentDownloadStreamResolver {
    override suspend fun resolve(magnetUri: String): String =
        error("Torrent streaming is not supported")
}
