package com.streamvault.app.custom

import android.content.Context
import android.widget.Toast
import com.streamvault.app.R
import com.streamvault.domain.model.LegacyProvider
import com.streamvault.domain.model.ProviderType
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.sync.AutomaticSyncActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForkCatalogSyncCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val providerRepository: ProviderRepository,
    private val channelRepository: ChannelRepository,
    private val movieRepository: MovieRepository
) {
    companion object {
        private val logger: Logger = Logger.getLogger("ForkCatalogSyncCoordinator")
        private const val PERIODIC_CHECK_INTERVAL_MILLIS = 60 * 60 * 1000L // 1 hour
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isSyncRunning = AtomicBoolean(false)
    private val maintenanceStarted = AtomicBoolean(false)
    private var periodicJob: Job? = null

    fun startSyncMaintenance() {
        if (!maintenanceStarted.compareAndSet(false, true)) return
        logger.info("Starting fork catalog sync maintenance")

        // 1. Initial delayed check after app boot to allow UI/playback to settle
        scope.launch {
            delay(ForkConfig.INITIAL_BACKGROUND_SYNC_DELAY_MILLIS)
            checkAndTriggerSync(isStartupCheck = true)
        }

        // 2. Periodic hourly check for daily sync if app stays open
        periodicJob = scope.launch {
            while (isActive) {
                delay(PERIODIC_CHECK_INTERVAL_MILLIS)
                checkAndTriggerSync(isStartupCheck = false)
            }
        }
    }

    fun onAppForegrounded() {
        if (!maintenanceStarted.get()) return
        scope.launch {
            delay(5_000L) // Wait 5 seconds after resume
            checkAndTriggerSync(isStartupCheck = false)
        }
    }

    /**
     * One-click manual refresh triggered from Settings.
     * Navigates immediately to Home so user can watch TV while this runs in background,
     * then shows a toast detailing newly added channels and movies.
     */
    fun triggerManualSync() {
        if (isSyncRunning.get()) {
            try {
                Toast.makeText(
                    context,
                    context.getString(R.string.fork_catalog_sync_in_progress),
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Throwable) {
                // Safe in headless unit test environments
            }
            return
        }

        scope.launch {
            executeManualSync()
        }
    }

    suspend fun executeManualSync(): Boolean {
        if (!isSyncRunning.compareAndSet(false, true)) return false

        try {
            val targetProvider = resolveTargetProvider()

            if (targetProvider == null) {
                logger.warning("No eligible provider found for manual refresh")
                return false
            }

            val providerId = targetProvider.id
            val initialChannels = channelRepository.getChannelCount(providerId).first()
            val initialMovies = movieRepository.getLibraryCount(providerId).first()

            try {
                val startedMsg = context.getString(R.string.fork_catalog_sync_started)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, startedMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Throwable) {}

            logger.info("Executing manual catalog refresh for provider id=$providerId")
            val result = providerRepository.refreshProviderData(
                providerId = providerId,
                force = true
            )

            when (result) {
                is Result.Success -> {
                    ForkPreferences.setLastDailySyncMillis(context, System.currentTimeMillis())
                    val finalChannels = channelRepository.getChannelCount(providerId).first()
                    val finalMovies = movieRepository.getLibraryCount(providerId).first()

                    val addedChannels = (finalChannels - initialChannels).coerceAtLeast(0)
                    val addedMovies = (finalMovies - initialMovies).coerceAtLeast(0)

                    try {
                        val toastMessage = if (addedChannels > 0 || addedMovies > 0) {
                            context.getString(
                                R.string.fork_catalog_sync_completed_added,
                                addedChannels,
                                addedMovies
                            )
                        } else {
                            context.getString(R.string.fork_catalog_sync_up_to_date)
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
                        }
                    } catch (_: Throwable) {}
                    return true
                }
                is Result.Error -> {
                    logger.warning("Manual catalog refresh failed: ${result.message}")
                    try {
                        val failedMsg = context.getString(R.string.fork_catalog_sync_failed)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, failedMsg, Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Throwable) {}
                    return false
                }
                is Result.Loading -> return false
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Exception during manual catalog refresh", e)
            return false
        } finally {
            isSyncRunning.set(false)
        }
    }

    suspend fun checkAndTriggerSync(isStartupCheck: Boolean) {
        if (isSyncRunning.get()) {
            logger.fine("Sync already running in background, skipping trigger")
            return
        }

        try {
            val targetProvider = resolveTargetProvider()

            if (targetProvider == null) {
                logger.fine("No eligible M3U provider found for background sync")
                return
            }

            val now = System.currentTimeMillis()
            val lastDailySync = ForkPreferences.getLastDailySyncMillis(context)
            val isDailyDue = (lastDailySync == 0L || (now - lastDailySync) >= ForkConfig.DAILY_SYNC_INTERVAL_MILLIS)

            if (!isDailyDue) {
                logger.fine("Catalog is up-to-date (lastSync=$lastDailySync), no sync needed")
                return
            }

            // Performance check: If user is actively playing media, defer daily sync
            if (AutomaticSyncActivity.hasPlayback) {
                logger.info("Playback active; deferring daily sync to prevent network/CPU contention")
                return
            }

            if (!isSyncRunning.compareAndSet(false, true)) {
                return
            }

            logger.info("Beginning daily background catalog sync")

            // Execute the silent catalog refresh
            val result = providerRepository.refreshProviderData(
                providerId = targetProvider.id,
                force = true
            )

            when (result) {
                is Result.Success -> {
                    logger.info("Daily background catalog sync completed successfully")
                    ForkPreferences.setLastDailySyncMillis(context, System.currentTimeMillis())
                }
                is Result.Error -> {
                    logger.warning("Daily background catalog sync failed: ${result.message}")
                }
                is Result.Loading -> Unit
            }
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Exception during background catalog sync", e)
        } finally {
            isSyncRunning.set(false)
        }
    }

    private suspend fun resolveTargetProvider(): LegacyProvider? {
        val providers = providerRepository.getProviders().first()
        val candidate = providerRepository.getActiveProvider().first()
            ?: providers.firstOrNull {
                it.type == ProviderType.M3U &&
                    (it.m3uUrl == ForkConfig.DEFAULT_M3U_URL ||
                        it.serverUrl == ForkConfig.DEFAULT_M3U_URL ||
                        it.name == ForkConfig.DEFAULT_M3U_NAME ||
                        it.m3uUrl.contains("rokon-iptv"))
            }
            ?: providers.firstOrNull { it.type == ProviderType.M3U }

        if (candidate != null &&
            (candidate.name == ForkConfig.DEFAULT_M3U_NAME || candidate.m3uUrl.contains("rokon-iptv")) &&
            (candidate.m3uUrl != ForkConfig.DEFAULT_M3U_URL || !candidate.m3uVodClassificationEnabled)
        ) {
            logger.info("Migrating provider ${candidate.id} URL to ${ForkConfig.DEFAULT_M3U_URL}")
            val updated = candidate.copy(
                m3uUrl = ForkConfig.DEFAULT_M3U_URL,
                serverUrl = ForkConfig.DEFAULT_M3U_URL,
                m3uVodClassificationEnabled = true
            )
            providerRepository.updateProvider(updated)
            return updated
        }
        return candidate
    }
}
