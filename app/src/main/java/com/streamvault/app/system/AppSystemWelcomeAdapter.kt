package com.streamvault.app.system

import android.content.Context
import com.streamvault.app.BuildConfig
import com.streamvault.app.custom.ForkConfig
import com.streamvault.app.custom.ForkPreferences
import com.streamvault.data.preferences.PreferencesRepository
import com.streamvault.data.sync.SyncProgressBus
import com.streamvault.feature.system.api.SystemWelcomePort
import com.streamvault.feature.system.api.WelcomeDevProviderConfig
import com.streamvault.feature.system.api.WelcomeSyncProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class AppSystemWelcomeAdapter private constructor(
    private val syncProgressBus: SyncProgressBus,
    private val context: Context?,
    private val preferencesRepository: PreferencesRepository?,
    @Suppress("UNUSED_PARAMETER") ignored: Unit,
) : SystemWelcomePort {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        syncProgressBus: SyncProgressBus,
        preferencesRepository: PreferencesRepository,
    ) : this(
        syncProgressBus = syncProgressBus,
        context = context,
        preferencesRepository = preferencesRepository,
        ignored = Unit,
    )

    internal constructor(
        syncProgressBus: SyncProgressBus,
        devProviderConfig: WelcomeDevProviderConfig,
    ) : this(
        syncProgressBus = syncProgressBus,
        context = null,
        preferencesRepository = null,
        ignored = Unit,
    ) {
        this.injectedDevProviderConfig = devProviderConfig
    }

    private var injectedDevProviderConfig: WelcomeDevProviderConfig? = null

    override val devProviderConfig: WelcomeDevProviderConfig
        get() = injectedDevProviderConfig ?: resolveDevProviderConfig(context)

    override suspend fun markDefaultProviderSeeded() {
        context?.let { ForkPreferences.setHasEverSeededDefaultProvider(it, true) }
    }

    companion object {
        internal fun resolveDevProviderConfig(context: Context?): WelcomeDevProviderConfig {
            val alreadySeeded = context?.let { ForkPreferences.hasEverSeededDefaultProvider(it) } ?: false
            val defaultXtreamServer = if (alreadySeeded) "" else ForkConfig.DEFAULT_XTREAM_URL
            val defaultXtreamUsername = if (alreadySeeded) "" else ForkConfig.DEFAULT_XTREAM_USERNAME
            val defaultXtreamPassword = if (alreadySeeded) "" else ForkConfig.DEFAULT_XTREAM_PASSWORD
            val defaultXtreamName = if (alreadySeeded) "" else ForkConfig.DEFAULT_XTREAM_NAME

            val xtreamServer = BuildConfig.XTREAM_DEV_SERVER.ifBlank { defaultXtreamServer }
            val xtreamUsername = BuildConfig.XTREAM_DEV_USERNAME.ifBlank { defaultXtreamUsername }
            val xtreamPassword = BuildConfig.XTREAM_DEV_PASSWORD.ifBlank { defaultXtreamPassword }
            val xtreamName = BuildConfig.XTREAM_DEV_NAME.ifBlank { defaultXtreamName }

            return WelcomeDevProviderConfig(
                xtreamServer = xtreamServer,
                xtreamUsername = xtreamUsername,
                xtreamPassword = xtreamPassword,
                xtreamName = xtreamName,
                m3uUrl = BuildConfig.M3U_DEV_URL,
                m3uName = BuildConfig.M3U_DEV_NAME,
                m3uVodClassificationEnabled = ForkConfig.DEFAULT_M3U_VOD_CLASSIFICATION_ENABLED,
            )
        }
    }

    override val dontAskProviderSetupAgain: Flow<Boolean> =
        preferencesRepository?.dontAskProviderSetupAgain ?: flowOf(false)

    override suspend fun setDontAskProviderSetupAgain(dontAskAgain: Boolean) {
        preferencesRepository?.setDontAskProviderSetupAgain(dontAskAgain)
    }

    override val syncProgress: Flow<WelcomeSyncProgress?> = syncProgressBus.aggregate.map { aggregate ->
        aggregate?.representative?.progress?.let { progress ->
            WelcomeSyncProgress(
                section = progress.section,
                current = progress.current,
                total = progress.total,
                currentLabel = progress.currentLabel,
                itemsIndexed = progress.itemsIndexed,
            )
        }
    }
}
