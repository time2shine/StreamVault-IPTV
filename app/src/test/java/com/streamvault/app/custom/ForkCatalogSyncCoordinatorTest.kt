package com.streamvault.app.custom

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.manager.ProviderCredentials
import com.streamvault.domain.model.ChannelLogoSourcePolicy
import com.streamvault.domain.model.GuideSourcePolicy
import com.streamvault.domain.model.LegacyProvider
import com.streamvault.domain.model.Program
import com.streamvault.domain.model.ProviderEpgSyncMode
import com.streamvault.domain.model.ProviderType
import com.streamvault.domain.model.Result
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.ProviderDeleteOutcome
import com.streamvault.domain.repository.ProviderDeleteProgress
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.ProviderSetupRequest
import com.streamvault.domain.sync.AutomaticSyncActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ForkCatalogSyncCoordinatorTest {

    private val context: Context = mock()
    private val preferences: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private val channelRepository: ChannelRepository = mock()
    private val movieRepository: MovieRepository = mock()

    private var storedLastSync = 0L

    @Before
    fun setUp() {
        storedLastSync = 0L

        whenever(context.getSharedPreferences(any(), any())).thenReturn(preferences)
        whenever(preferences.getLong(any(), any())).thenAnswer { storedLastSync }
        whenever(preferences.edit()).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenAnswer { invocation ->
            storedLastSync = invocation.getArgument(1)
            editor
        }

        whenever(channelRepository.getChannelCount(any())).thenReturn(flowOf(553))
        whenever(movieRepository.getLibraryCount(any())).thenReturn(flowOf(19344))
        whenever(context.getString(any<Int>())).thenReturn("Sync status")
        whenever(context.getString(any<Int>(), any<Any>(), any<Any>())).thenReturn("Sync complete")
    }

    @Test
    fun `manual sync triggers refresh on active provider and records completion`() = runTest {
        val provider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_M3U_NAME,
            type = ProviderType.M3U,
            serverUrl = ForkConfig.DEFAULT_M3U_URL,
            m3uUrl = ForkConfig.DEFAULT_M3U_URL,
            m3uVodClassificationEnabled = true
        )

        val repository = TestProviderRepository(providersList = listOf(provider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository)
        val success = coordinator.executeManualSync()

        assertThat(success).isTrue()
        assertThat(repository.refreshedProviderId).isEqualTo(42L)
        assertThat(repository.refreshedForce).isTrue()
        assertThat(storedLastSync).isGreaterThan(0L)
    }

    @Test
    fun `manual sync automatically migrates legacy provider URL to latest default URL`() = runTest {
        val legacyProvider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_M3U_NAME,
            type = ProviderType.M3U,
            serverUrl = "https://tinyurl.com/rokon-iptv2",
            m3uUrl = "https://tinyurl.com/rokon-iptv2",
            m3uVodClassificationEnabled = false
        )

        val repository = TestProviderRepository(providersList = listOf(legacyProvider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository)
        val success = coordinator.executeManualSync()

        assertThat(success).isTrue()
        assertThat(repository.updatedProvider?.m3uUrl).isEqualTo(ForkConfig.DEFAULT_M3U_URL)
        assertThat(repository.updatedProvider?.m3uVodClassificationEnabled).isTrue()
    }

    @Test
    fun `when daily sync is not due, no background sync is triggered`() = runTest {
        storedLastSync = System.currentTimeMillis() // just synced

        val fullProvider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_M3U_NAME,
            type = ProviderType.M3U,
            serverUrl = ForkConfig.DEFAULT_M3U_URL,
            m3uUrl = ForkConfig.DEFAULT_M3U_URL,
            m3uVodClassificationEnabled = true
        )

        val repository = TestProviderRepository(providersList = listOf(fullProvider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository)
        coordinator.checkAndTriggerSync(isStartupCheck = false)

        assertThat(repository.refreshedProviderId).isNull()
    }

    @Test
    fun `daily sync triggers when more than 24 hours have passed`() = runTest {
        storedLastSync = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago

        val fullProvider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_M3U_NAME,
            type = ProviderType.M3U,
            serverUrl = ForkConfig.DEFAULT_M3U_URL,
            m3uUrl = ForkConfig.DEFAULT_M3U_URL,
            m3uVodClassificationEnabled = true
        )

        val repository = TestProviderRepository(providersList = listOf(fullProvider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository)
        coordinator.checkAndTriggerSync(isStartupCheck = false)

        assertThat(repository.refreshedProviderId).isEqualTo(42L)
        assertThat(repository.refreshedForce).isTrue()
        assertThat(storedLastSync).isGreaterThan(System.currentTimeMillis() - 5000L)
    }

    @Test
    fun `daily sync is deferred when media playback is actively running`() = runTest {
        storedLastSync = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago

        val player = Any()
        AutomaticSyncActivity.started(player)
        try {
            val fullProvider = LegacyProvider(
                id = 42L,
                name = ForkConfig.DEFAULT_M3U_NAME,
                type = ProviderType.M3U,
                serverUrl = ForkConfig.DEFAULT_M3U_URL,
                m3uUrl = ForkConfig.DEFAULT_M3U_URL,
                m3uVodClassificationEnabled = true
            )

            val repository = TestProviderRepository(providersList = listOf(fullProvider))
            val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository)
            coordinator.checkAndTriggerSync(isStartupCheck = false)

            assertThat(repository.refreshedProviderId).isNull()
        } finally {
            AutomaticSyncActivity.stopped(player)
        }
    }

    private class TestProviderRepository(
        var providersList: List<LegacyProvider> = emptyList(),
        var refreshResult: Result<Unit> = Result.success(Unit)
    ) : ProviderRepository {
        var updatedProvider: LegacyProvider? = null
        var refreshedProviderId: Long? = null
        var refreshedForce: Boolean? = null

        override fun getProviders(): Flow<List<LegacyProvider>> = flowOf(providersList)
        override fun getActiveProvider(): Flow<LegacyProvider?> = flowOf(providersList.firstOrNull())
        override suspend fun getProvider(id: Long): LegacyProvider? = providersList.firstOrNull { it.id == id }
        override suspend fun addProvider(provider: LegacyProvider): Result<Long> = Result.success(1L)
        override suspend fun updateProvider(provider: LegacyProvider): Result<Unit> {
            updatedProvider = provider
            return Result.success(Unit)
        }
        override suspend fun deleteProvider(id: Long, onProgress: ((ProviderDeleteProgress) -> Unit)?): Result<ProviderDeleteOutcome> = error("unused")
        override suspend fun getAllProviderCredentials(): List<ProviderCredentials> = emptyList()
        override suspend fun updateProviderPassword(serverUrl: String, username: String, cleartextPassword: String): Boolean = false
        override suspend fun setActiveProvider(id: Long): Result<Unit> = Result.success(Unit)
        override suspend fun setupProvider(request: ProviderSetupRequest, onProgress: ((String) -> Unit)?, onCode: ((String) -> Unit)?): Result<LegacyProvider> = error("unused")
        override suspend fun refreshProviderData(
            providerId: Long,
            force: Boolean,
            movieFastSyncOverride: Boolean?,
            epgSyncModeOverride: ProviderEpgSyncMode?,
            onProgress: ((String) -> Unit)?
        ): Result<Unit> {
            refreshedProviderId = providerId
            refreshedForce = force
            return refreshResult
        }
        override suspend fun getProgramsForLiveStream(
            providerId: Long,
            streamId: Long,
            epgChannelId: String?,
            limit: Int
        ): Result<List<Program>> = Result.success(emptyList())

        override suspend fun buildCatchUpUrl(
            providerId: Long,
            streamId: Long,
            start: Long,
            end: Long
        ): String? = null
    }
}
