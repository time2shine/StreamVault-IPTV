@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.streamvault.feature.system.presentation.welcome

import com.streamvault.core.ui.layout.AdaptiveRow

import androidx.compose.foundation.layout.FlowRow

import com.streamvault.core.ui.layout.isPortraitLayout

import com.streamvault.core.ui.design.browseBackdrop
import com.streamvault.core.ui.design.BrowseShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.streamvault.feature.system.R
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.streamvault.core.ui.components.shell.StatusPill
import com.streamvault.core.ui.design.BrowseColors as AppColors
import com.streamvault.core.ui.interaction.TvButton
import com.streamvault.domain.repository.ProviderRepository
import com.streamvault.domain.sync.Section
import com.streamvault.domain.usecase.M3uProviderSetupCommand
import com.streamvault.domain.usecase.ValidateAndAddProvider
import com.streamvault.domain.usecase.ValidateAndAddProviderResult
import com.streamvault.domain.usecase.XtreamProviderSetupCommand
import com.streamvault.feature.system.api.SystemWelcomePort
import com.streamvault.feature.system.api.WelcomeSyncProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val validateAndAddProvider: ValidateAndAddProvider,
    private val welcomePort: SystemWelcomePort,
) : ViewModel() {

    private val _hasProviders = MutableStateFlow<Boolean?>(null)
    val hasProviders: StateFlow<Boolean?> = _hasProviders.asStateFlow()

    private val _dontAskAgainAtStartup = MutableStateFlow<Boolean?>(null)
    val dontAskAgainAtStartup: StateFlow<Boolean?> = _dontAskAgainAtStartup.asStateFlow()

    private val _dontAskProviderSetupAgain = MutableStateFlow(false)
    val dontAskProviderSetupAgain: StateFlow<Boolean> = _dontAskProviderSetupAgain.asStateFlow()

    private val _isOfflineError = MutableStateFlow(false)
    val isOfflineError: StateFlow<Boolean> = _isOfflineError.asStateFlow()

    private val _isBootstrapping = MutableStateFlow(false)
    val isBootstrapping: StateFlow<Boolean> = _isBootstrapping.asStateFlow()

    private val acceptingProgress = MutableStateFlow(true)

    val syncProgress: StateFlow<WelcomeSyncProgress?> =
        combine(welcomePort.syncProgress, acceptingProgress) { progress, accept ->
            if (accept) progress else null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val savedPreference = welcomePort.dontAskProviderSetupAgain.first()
            _dontAskProviderSetupAgain.value = savedPreference
            _dontAskAgainAtStartup.value = savedPreference
        }
        viewModelScope.launch {
            val active = runCatching { providerRepository.getActiveProvider().first() }.getOrNull()
            if (active != null) {
                _hasProviders.value = true
                _isBootstrapping.value = false
                _isOfflineError.value = false
                acceptingProgress.value = false
                return@launch
            }

            val existing = runCatching { providerRepository.getProviders().first() }.getOrNull().orEmpty()
            if (existing.isNotEmpty()) {
                val activated = runCatching { providerRepository.setActiveProvider(existing.first().id) }.getOrNull()
                if (activated is com.streamvault.domain.model.Result.Success) {
                    _hasProviders.value = true
                    _isBootstrapping.value = false
                    _isOfflineError.value = false
                    acceptingProgress.value = false
                    return@launch
                }
            }

            startBootstrap()
        }
        viewModelScope.launch {
            runCatching {
                providerRepository.getActiveProvider()
                    .filterNotNull()
                    .collect {
                        _hasProviders.value = true
                        _isOfflineError.value = false
                        _isBootstrapping.value = false
                        acceptingProgress.value = false
                    }
            }
        }
        viewModelScope.launch {
            runCatching {
                providerRepository.getProviders()
                    .collect { providers ->
                        if (providers.isNotEmpty()) {
                            val active = runCatching { providerRepository.getActiveProvider().first() }.getOrNull()
                            if (active != null) {
                                _hasProviders.value = true
                                _isOfflineError.value = false
                                _isBootstrapping.value = false
                                acceptingProgress.value = false
                            } else {
                                val activated = runCatching { providerRepository.setActiveProvider(providers.first().id) }.getOrNull()
                                if (activated is com.streamvault.domain.model.Result.Success) {
                                    _hasProviders.value = true
                                    _isOfflineError.value = false
                                    _isBootstrapping.value = false
                                    acceptingProgress.value = false
                                }
                            }
                        }
                    }
            }
        }
    }

    fun retryBootstrap() {
        startBootstrap()
    }

    fun setDontAskProviderSetupAgain(dontAskAgain: Boolean) {
        _dontAskProviderSetupAgain.value = dontAskAgain
        viewModelScope.launch {
            welcomePort.setDontAskProviderSetupAgain(dontAskAgain)
        }
    }

    fun continueAfterSaving(onContinue: () -> Unit) {
        viewModelScope.launch {
            welcomePort.setDontAskProviderSetupAgain(dontAskProviderSetupAgain.value)
            onContinue()
        }
    }

    private fun startBootstrap() {
        viewModelScope.launch {
            val active = runCatching { providerRepository.getActiveProvider().first() }.getOrNull()
            if (active != null) {
                _hasProviders.value = true
                _isBootstrapping.value = false
                _isOfflineError.value = false
                acceptingProgress.value = false
                return@launch
            }

            val existing = runCatching { providerRepository.getProviders().first() }.getOrNull().orEmpty()
            if (existing.isNotEmpty()) {
                val activated = runCatching { providerRepository.setActiveProvider(existing.first().id) }.getOrNull()
                if (activated is com.streamvault.domain.model.Result.Success) {
                    _hasProviders.value = true
                    _isBootstrapping.value = false
                    _isOfflineError.value = false
                    acceptingProgress.value = false
                    return@launch
                }
            }

            val devConfig = welcomePort.devProviderConfig
            val xtreamServer = devConfig.xtreamServer
            val xtreamUser = devConfig.xtreamUsername
            val xtreamPass = devConfig.xtreamPassword

            if (xtreamServer.isNotBlank() && xtreamUser.isNotBlank() && xtreamPass.isNotBlank()) {
                _isBootstrapping.value = true
                _isOfflineError.value = false
                acceptingProgress.value = true

                val result = runCatching {
                    validateAndAddProvider.loginXtream(
                        XtreamProviderSetupCommand(
                            serverUrl = xtreamServer,
                            username = xtreamUser,
                            password = xtreamPass,
                            name = devConfig.xtreamName.ifBlank { "Rokon IPTV" },
                            xtreamFastSyncEnabled = true
                        )
                    )
                }.getOrNull()

                when (result) {
                    is ValidateAndAddProviderResult.Success,
                    is ValidateAndAddProviderResult.SavedWithWarning -> {
                        val providerId = if (result is ValidateAndAddProviderResult.Success) result.provider.id else (result as ValidateAndAddProviderResult.SavedWithWarning).provider.id
                        var activated = runCatching { providerRepository.setActiveProvider(providerId) }.getOrNull() is com.streamvault.domain.model.Result.Success
                        var attempts = 0
                        while (!activated && attempts < 10) {
                            kotlinx.coroutines.delay(500L)
                            activated = runCatching { providerRepository.setActiveProvider(providerId) }.getOrNull() is com.streamvault.domain.model.Result.Success
                            attempts++
                        }
                        welcomePort.markDefaultProviderSeeded()
                        _isBootstrapping.value = false
                        _isOfflineError.value = false
                        _hasProviders.value = true
                        acceptingProgress.value = false
                    }
                    is ValidateAndAddProviderResult.Error,
                    is ValidateAndAddProviderResult.ValidationError,
                    is ValidateAndAddProviderResult.VerificationInconclusive,
                    is ValidateAndAddProviderResult.TransportConsentRequired,
                    null -> {
                        val currentlyActive = runCatching { providerRepository.getActiveProvider().first() }.getOrNull()
                        val has = currentlyActive != null
                        _isBootstrapping.value = false
                        _hasProviders.value = has
                        if (!has) {
                            _isOfflineError.value = true
                        }
                    }
                }
                return@launch
            }

            val m3uUrl = devConfig.m3uUrl
            if (m3uUrl.isNotBlank()) {
                _isBootstrapping.value = true
                _isOfflineError.value = false
                acceptingProgress.value = true

                val result = runCatching {
                    validateAndAddProvider.addM3u(
                        M3uProviderSetupCommand(
                            url = m3uUrl,
                            name = devConfig.m3uName.ifBlank { "Dev M3U (seeded)" },
                            m3uVodClassificationEnabled = devConfig.m3uVodClassificationEnabled
                        )
                    )
                }.getOrNull()

                when (result) {
                    is ValidateAndAddProviderResult.Success,
                    is ValidateAndAddProviderResult.SavedWithWarning -> {
                        val providerId = if (result is ValidateAndAddProviderResult.Success) result.provider.id else (result as ValidateAndAddProviderResult.SavedWithWarning).provider.id
                        var activated = runCatching { providerRepository.setActiveProvider(providerId) }.getOrNull() is com.streamvault.domain.model.Result.Success
                        var attempts = 0
                        while (!activated && attempts < 10) {
                            kotlinx.coroutines.delay(500L)
                            activated = runCatching { providerRepository.setActiveProvider(providerId) }.getOrNull() is com.streamvault.domain.model.Result.Success
                            attempts++
                        }
                        welcomePort.markDefaultProviderSeeded()
                        _isBootstrapping.value = false
                        _isOfflineError.value = false
                        _hasProviders.value = true
                        acceptingProgress.value = false
                    }
                    is ValidateAndAddProviderResult.Error,
                    is ValidateAndAddProviderResult.ValidationError,
                    is ValidateAndAddProviderResult.VerificationInconclusive,
                    is ValidateAndAddProviderResult.TransportConsentRequired,
                    null -> {
                        val currentlyActive = runCatching { providerRepository.getActiveProvider().first() }.getOrNull()
                        val has = currentlyActive != null
                        _isBootstrapping.value = false
                        _hasProviders.value = has
                        if (!has) {
                            _isOfflineError.value = true
                        }
                    }
                }
                return@launch
            }

            _isBootstrapping.value = false
            _isOfflineError.value = false
            _hasProviders.value = false
            acceptingProgress.value = false
        }
    }
}

@Composable
fun WelcomeScreen(
    onNavigateToHome: () -> Unit,
    startupReady: Boolean = true,
    onNavigateToSetup: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val hasProviders by viewModel.hasProviders.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val isOfflineError by viewModel.isOfflineError.collectAsStateWithLifecycle()
    val isBootstrapping by viewModel.isBootstrapping.collectAsStateWithLifecycle()
    val dontAskAgainAtStartup by viewModel.dontAskAgainAtStartup.collectAsStateWithLifecycle()
    val dontAskProviderSetupAgain by viewModel.dontAskProviderSetupAgain.collectAsStateWithLifecycle()

    LaunchedEffect(hasProviders, startupReady, dontAskAgainAtStartup, isOfflineError) {
        when (hasProviders) {
            true -> if (startupReady) onNavigateToHome()
            false -> if (!isOfflineError && startupReady && dontAskAgainAtStartup == true) onNavigateToHome()
            null -> Unit
        }
    }

    val displayedProviderState = if (
        dontAskAgainAtStartup == null ||
        (hasProviders == false && dontAskAgainAtStartup == true)
    ) null else hasProviders

    WelcomeContent(
        hasProviders = displayedProviderState,
        isOfflineError = isOfflineError,
        isBootstrapping = isBootstrapping,
        syncProgress = syncProgress,
        onRetry = viewModel::retryBootstrap,
        onNavigateToHome = { viewModel.continueAfterSaving(onNavigateToHome) },
        onNavigateToSetup = { viewModel.continueAfterSaving(onNavigateToSetup) },
        dontAskAgain = dontAskProviderSetupAgain,
        onDontAskAgainChanged = viewModel::setDontAskProviderSetupAgain,
    )
}

@Composable
internal fun WelcomeContent(
    hasProviders: Boolean?,
    isOfflineError: Boolean = false,
    isBootstrapping: Boolean = false,
    syncProgress: WelcomeSyncProgress?,
    onRetry: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToSetup: () -> Unit,
    dontAskAgain: Boolean = false,
    onDontAskAgainChanged: (Boolean) -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .browseBackdrop()
        )

        when {
            isOfflineError -> WelcomeOfflineCard(
                onRetry = onRetry,
                onNavigateToSetup = onNavigateToSetup,
                onNavigateToHome = onNavigateToHome,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )

            hasProviders == false && !isBootstrapping -> WelcomeStartCard(
                dontAskAgain = dontAskAgain,
                onDontAskAgainChanged = onDontAskAgainChanged,
                onNavigateToHome = onNavigateToHome,
                onNavigateToSetup = onNavigateToSetup,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )

            else -> WelcomeLoadingCard(
                syncProgress = syncProgress,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
        }
    }
}

@Composable
private fun WelcomeOfflineCard(
    onRetry: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onOpenNetworkSettings: () -> Unit = {
        val opened = runCatching {
            context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }.getOrDefault(false)

        if (!opened) {
            runCatching {
                context.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    Surface(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(),
        shape = BrowseShapes.Hero,
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (isPortraitLayout()) 20.dp else 40.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StatusPill(
                label = stringResource(R.string.welcome_offline_title),
                containerColor = AppColors.Warning
            )
            Text(
                text = stringResource(R.string.welcome_offline_title),
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.welcome_offline_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            AdaptiveRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvButton(
                    onClick = onRetry,
                    modifier = if (isPortraitLayout()) Modifier.fillMaxWidth() else Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.welcome_action_retry))
                }
                TvButton(
                    onClick = onOpenNetworkSettings,
                    modifier = if (isPortraitLayout()) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    colors = ButtonDefaults.colors(
                        containerColor = AppColors.SurfaceElevated,
                        focusedContainerColor = Color.White,
                        contentColor = AppColors.TextPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.welcome_action_network_settings))
                }
            }
            AdaptiveRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvButton(
                    onClick = onNavigateToSetup,
                    modifier = if (isPortraitLayout()) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = AppColors.SurfaceElevated,
                        contentColor = AppColors.TextSecondary,
                        focusedContentColor = AppColors.TextPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.welcome_action_manual_setup))
                }
                TvButton(
                    onClick = onNavigateToHome,
                    modifier = if (isPortraitLayout()) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    colors = ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = AppColors.SurfaceElevated,
                        contentColor = AppColors.TextSecondary,
                        focusedContentColor = AppColors.TextPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.welcome_setup_later))
                }
            }
        }
    }
}

@Composable
private fun WelcomeLoadingCard(
    syncProgress: WelcomeSyncProgress?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = BrowseShapes.Hero,
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.9f))
    ) {
        Column(
        modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            val representativeProgress = syncProgress
            val pillLabel = if (representativeProgress != null) {
                stringResource(sectionLabelRes(representativeProgress.section))
            } else {
                stringResource(R.string.app_name)
            }
            val pillColor = if (representativeProgress != null) {
                sectionColor(representativeProgress.section)
            } else {
                AppColors.BrandMuted
            }
            StatusPill(
                label = pillLabel,
                containerColor = pillColor
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (representativeProgress == null) {
                CircularProgressIndicator(color = AppColors.Brand)
                Spacer(modifier = Modifier.height(18.dp))
            }
            Text(
                text = stringResource(R.string.welcome_loading_title),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val subtitle = if (representativeProgress != null && representativeProgress.currentLabel.isNotBlank()) {
                representativeProgress.currentLabel
            } else {
                stringResource(R.string.welcome_loading_subtitle)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary
            )
            if (representativeProgress != null) {
                Spacer(modifier = Modifier.height(14.dp))
                if (representativeProgress.total > 0) {
                    LinearProgressIndicator(
                        progress = { representativeProgress.current.toFloat() / representativeProgress.total.toFloat() },
                        modifier = Modifier.width(260.dp),
                        color = AppColors.Brand,
                        trackColor = AppColors.BrandMuted
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.width(260.dp),
                        color = AppColors.Brand,
                        trackColor = AppColors.BrandMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        R.string.sync_items_indexed_format,
                        representativeProgress.itemsIndexed
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun WelcomeStartCard(
    dontAskAgain: Boolean,
    onDontAskAgainChanged: (Boolean) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(),
        shape = BrowseShapes.Hero,
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (isPortraitLayout()) 20.dp else 40.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StatusPill(
                label = stringResource(R.string.app_name),
                containerColor = AppColors.BrandMuted
            )
            Text(
                text = stringResource(R.string.welcome_tagline),
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            AdaptiveRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvButton(
                    onClick = onNavigateToSetup,
                    modifier = if (isPortraitLayout()) Modifier.fillMaxWidth() else Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.welcome_setup_provider))
                }
                TvButton(
                    onClick = onNavigateToHome,
                    modifier = if (isPortraitLayout()) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    colors = ButtonDefaults.colors(
                        containerColor = AppColors.SurfaceElevated,
                        focusedContainerColor = Color.White,
                        contentColor = AppColors.TextPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.welcome_setup_later))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = dontAskAgain,
                        role = Role.Checkbox,
                        onValueChange = onDontAskAgainChanged,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = dontAskAgain,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppColors.Brand,
                        uncheckedColor = AppColors.TextSecondary,
                        checkmarkColor = Color.White,
                    ),
                )
                Text(
                    text = stringResource(R.string.welcome_dont_ask_again),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.TextSecondary,
                )
            }
        }
    }
}

private fun sectionColor(section: Section): Color = when (section) {
    Section.LIVE -> AppColors.Brand
    Section.VOD -> AppColors.Success
    Section.SERIES -> AppColors.Warning
}

private fun sectionLabelRes(section: Section): Int = when (section) {
    Section.LIVE -> R.string.sync_section_live
    Section.VOD -> R.string.sync_section_vod
    Section.SERIES -> R.string.sync_section_series
}
