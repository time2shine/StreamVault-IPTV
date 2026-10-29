package com.streamvault.app.custom

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.manager.ProviderCredentials
import com.streamvault.domain.model.ChannelLogoSourcePolicy
import com.streamvault.domain.model.EpgSource
import com.streamvault.domain.model.GuideSourcePolicy
import com.streamvault.domain.model.LegacyProvider
import com.streamvault.domain.model.Program
import com.streamvault.domain.model.ProviderEpgSourceAssignment
import com.streamvault.domain.model.ProviderEpgSyncMode
import com.streamvault.domain.model.ProviderType
import com.streamvault.domain.model.Result
import com.streamvault.domain.model.XmltvTimezonePolicy
import com.streamvault.domain.repository.ChannelRepository
import com.streamvault.domain.repository.EpgSourceRepository
import com.streamvault.domain.repository.MovieRepository
import com.streamvault.domain.repository.ProviderDeleteOutcome
import com.streamvault.domain.repository.ProviderDeleteProgress
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.repository.ProviderSetupRequest
import com.streamvault.domain.sync.AutomaticSyncActivity
import com.streamvault.domain.usecase.ValidateAndAddProvider
import com.streamvault.domain.usecase.ValidateAndAddProviderResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ForkCatalogSyncCoordinatorTest {

    private val context: Context = mock()
    private val preferences: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private val channelRepository: ChannelRepository = mock()
    private val movieRepository: MovieRepository = mock()
    private val validateAndAddProvider: ValidateAndAddProvider = mock()
    private val epgSourceRepository: EpgSourceRepository = mock()

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

        whenever(epgSourceRepository.getAllSources()).thenReturn(flowOf(emptyList()))
        whenever(epgSourceRepository.getAssignmentsForProvider(any())).thenReturn(flowOf(emptyList()))
        runBlocking {
            whenever(epgSourceRepository.addSource(any(), any(), any(), anyOrNull())).thenReturn(
                Result.success(EpgSource(id = 1L, name = ForkConfig.DEFAULT_EPG_NAME, url = ForkConfig.DEFAULT_EPG_URL))
            )
            whenever(epgSourceRepository.assignSourceToProvider(any(), any(), any())).thenReturn(Result.success(Unit))
            whenever(epgSourceRepository.getSourceById(any())).thenReturn(
                EpgSource(id = 1L, name = ForkConfig.DEFAULT_EPG_NAME, url = ForkConfig.DEFAULT_EPG_URL, lastRefreshAt = 0L)
            )
            whenever(epgSourceRepository.refreshSource(any())).thenReturn(Result.success(Unit))
        }
    }

    @Test
    fun `manual sync triggers refresh on active provider and records completion`() = runTest {
        val provider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_XTREAM_NAME,
            type = ProviderType.XTREAM_CODES,
            serverUrl = ForkConfig.DEFAULT_XTREAM_URL,
            username = ForkConfig.DEFAULT_XTREAM_USERNAME,
            password = ForkConfig.DEFAULT_XTREAM_PASSWORD
        )

        val repository = TestProviderRepository(providersList = listOf(provider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository, validateAndAddProvider, epgSourceRepository)
        val success = coordinator.executeManualSync()

        assertThat(success).isTrue()
        assertThat(repository.refreshedProviderId).isEqualTo(42L)
        assertThat(repository.refreshedForce).isTrue()
        assertThat(storedLastSync).isGreaterThan(0L)
    }

    @Test
    fun `manual sync automatically migrates legacy provider to Xtream Codes API`() = runTest {
        val legacyProvider = LegacyProvider(
            id = 42L,
            name = "Rokon IPTV",
            type = ProviderType.M3U,
            serverUrl = "https://tinyurl.com/rokon-iptv2",
            m3uUrl = "https://tinyurl.com/rokon-iptv2",
            m3uVodClassificationEnabled = false
        )
        val xtreamProvider = LegacyProvider(
            id = 99L,
            name = ForkConfig.DEFAULT_XTREAM_NAME,
            type = ProviderType.XTREAM_CODES,
            serverUrl = ForkConfig.DEFAULT_XTREAM_URL,
            username = ForkConfig.DEFAULT_XTREAM_USERNAME,
            password = ForkConfig.DEFAULT_XTREAM_PASSWORD
        )

        val repository = TestProviderRepository(providersList = listOf(legacyProvider))
        whenever(validateAndAddProvider.loginXtream(any(), anyOrNull())).thenReturn(
            ValidateAndAddProviderResult.Success(xtreamProvider)
        )

        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository, validateAndAddProvider, epgSourceRepository)
        val success = coordinator.executeManualSync()

        assertThat(success).isTrue()
        assertThat(repository.deletedProviderId).isEqualTo(42L)
        assertThat(repository.activeProviderId).isEqualTo(99L)
        assertThat(repository.refreshedProviderId).isEqualTo(99L)
    }

    @Test
    fun `when daily sync is not due, no background sync is triggered`() = runTest {
        storedLastSync = System.currentTimeMillis() // just synced

        val fullProvider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_XTREAM_NAME,
            type = ProviderType.XTREAM_CODES,
            serverUrl = ForkConfig.DEFAULT_XTREAM_URL,
            username = ForkConfig.DEFAULT_XTREAM_USERNAME,
            password = ForkConfig.DEFAULT_XTREAM_PASSWORD
        )

        val repository = TestProviderRepository(providersList = listOf(fullProvider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository, validateAndAddProvider, epgSourceRepository)
        coordinator.checkAndTriggerSync(isStartupCheck = false)

        assertThat(repository.refreshedProviderId).isNull()
    }

    @Test
    fun `daily sync triggers when more than 24 hours have passed`() = runTest {
        storedLastSync = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago

        val fullProvider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_XTREAM_NAME,
            type = ProviderType.XTREAM_CODES,
            serverUrl = ForkConfig.DEFAULT_XTREAM_URL,
            username = ForkConfig.DEFAULT_XTREAM_USERNAME,
            password = ForkConfig.DEFAULT_XTREAM_PASSWORD
        )

        val repository = TestProviderRepository(providersList = listOf(fullProvider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository, validateAndAddProvider, epgSourceRepository)
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
                name = ForkConfig.DEFAULT_XTREAM_NAME,
                type = ProviderType.XTREAM_CODES,
                serverUrl = ForkConfig.DEFAULT_XTREAM_URL,
                username = ForkConfig.DEFAULT_XTREAM_USERNAME,
                password = ForkConfig.DEFAULT_XTREAM_PASSWORD
            )

            val repository = TestProviderRepository(providersList = listOf(fullProvider))
            val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository, validateAndAddProvider, epgSourceRepository)
            coordinator.checkAndTriggerSync(isStartupCheck = false)

            assertThat(repository.refreshedProviderId).isNull()
        } finally {
            AutomaticSyncActivity.stopped(player)
        }
    }

    @Test
    fun `ensureDefaultEpgSource provisions and assigns default EPG source when missing`() = runTest {
        val provider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_XTREAM_NAME,
            type = ProviderType.XTREAM_CODES,
            serverUrl = ForkConfig.DEFAULT_XTREAM_URL,
            username = ForkConfig.DEFAULT_XTREAM_USERNAME,
            password = ForkConfig.DEFAULT_XTREAM_PASSWORD
        )

        val repository = TestProviderRepository(providersList = listOf(provider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository, validateAndAddProvider, epgSourceRepository)
        coordinator.ensureDefaultEpgSource(42L)

        org.mockito.kotlin.verify(epgSourceRepository).addSource(
            name = org.mockito.kotlin.eq(ForkConfig.DEFAULT_EPG_NAME),
            url = org.mockito.kotlin.eq(ForkConfig.DEFAULT_EPG_URL),
            timezonePolicy = org.mockito.kotlin.eq(XmltvTimezonePolicy.REQUIRE_OFFSET),
            timezoneId = org.mockito.kotlin.isNull()
        )
        org.mockito.kotlin.verify(epgSourceRepository).assignSourceToProvider(
            providerId = org.mockito.kotlin.eq(42L),
            epgSourceId = org.mockito.kotlin.eq(1L),
            priority = org.mockito.kotlin.eq(0)
        )
        org.mockito.kotlin.verify(epgSourceRepository).refreshSource(1L)
    }

    @Test
    fun `ensureDefaultEpgSource does not re-add existing EPG source`() = runTest {
        val existingSource = EpgSource(
            id = 77L,
            name = ForkConfig.DEFAULT_EPG_NAME,
            url = ForkConfig.DEFAULT_EPG_URL,
            lastRefreshAt = System.currentTimeMillis()
        )
        val assignment = ProviderEpgSourceAssignment(id = 1L, providerId = 42L, epgSourceId = 77L)

        whenever(epgSourceRepository.getAllSources()).thenReturn(flowOf(listOf(existingSource)))
        whenever(epgSourceRepository.getAssignmentsForProvider(42L)).thenReturn(flowOf(listOf(assignment)))
        whenever(epgSourceRepository.getSourceById(77L)).thenReturn(existingSource)

        val provider = LegacyProvider(
            id = 42L,
            name = ForkConfig.DEFAULT_XTREAM_NAME,
            type = ProviderType.XTREAM_CODES,
            serverUrl = ForkConfig.DEFAULT_XTREAM_URL,
            username = ForkConfig.DEFAULT_XTREAM_USERNAME,
            password = ForkConfig.DEFAULT_XTREAM_PASSWORD
        )

        val repository = TestProviderRepository(providersList = listOf(provider))
        val coordinator = ForkCatalogSyncCoordinator(context, repository, channelRepository, movieRepository, validateAndAddProvider, epgSourceRepository)
        coordinator.ensureDefaultEpgSource(42L)

        org.mockito.kotlin.verify(epgSourceRepository, org.mockito.kotlin.never()).addSource(any(), any(), any(), anyOrNull())
        org.mockito.kotlin.verify(epgSourceRepository, org.mockito.kotlin.never()).assignSourceToProvider(any(), any(), any())
        org.mockito.kotlin.verify(epgSourceRepository, org.mockito.kotlin.never()).refreshSource(any())
    }

    private class TestProviderRepository(
        var providersList: List<LegacyProvider> = emptyList(),
        var refreshResult: Result<Unit> = Result.success(Unit)
    ) : ProviderRepository {
        var updatedProvider: LegacyProvider? = null
        var refreshedProviderId: Long? = null
        var refreshedForce: Boolean? = null
        var deletedProviderId: Long? = null
        var activeProviderId: Long? = null

        override fun getProviders(): Flow<List<LegacyProvider>> = flowOf(providersList)
        override fun getActiveProvider(): Flow<LegacyProvider?> = flowOf(providersList.firstOrNull())
        override suspend fun getProvider(id: Long): LegacyProvider? = providersList.firstOrNull { it.id == id }
        override suspend fun addProvider(provider: LegacyProvider): Result<Long> = Result.success(1L)
        override suspend fun updateProvider(provider: LegacyProvider): Result<Unit> {
            updatedProvider = provider
            return Result.success(Unit)
        }
        override suspend fun deleteProvider(id: Long, onProgress: ((ProviderDeleteProgress) -> Unit)?): Result<ProviderDeleteOutcome> {
            deletedProviderId = id
            providersList = providersList.filterNot { it.id == id }
            return Result.success(ProviderDeleteOutcome(id, 0, false))
        }
        override suspend fun getAllProviderCredentials(): List<ProviderCredentials> = emptyList()
        override suspend fun updateProviderPassword(serverUrl: String, username: String, cleartextPassword: String): Boolean = false
        override suspend fun setActiveProvider(id: Long): Result<Unit> {
            activeProviderId = id
            return Result.success(Unit)
        }
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
