package com.astrogolem.sidequest.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.astrogolem.sidequest.core.data.model.ProviderAvailability
import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.repo.ArchiveService
import com.astrogolem.sidequest.core.data.repo.SecurityService
import com.astrogolem.sidequest.core.ui.components.EmptyStateCard
import com.astrogolem.sidequest.core.ui.components.ErrorStateCard
import com.astrogolem.sidequest.core.ui.components.InlineSupportText
import com.astrogolem.sidequest.core.ui.components.LoadingStateCard
import com.astrogolem.sidequest.core.ui.components.PreferenceSwitchRow
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.components.StateShellAction
import com.astrogolem.sidequest.core.ui.components.SplitActionRow
import com.astrogolem.sidequest.core.ui.text.UiText
import com.astrogolem.sidequest.core.ui.text.resolve
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.paletteFor
import com.astrogolem.sidequest.core.ui.theme.SidequestSpacing
import com.astrogolem.sidequest.core.ui.theme.ThemePreset
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val archiveMessage by viewModel.archiveMessage.collectAsStateWithLifecycle()
    val notesFolderMessage by viewModel.notesFolderMessage.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val providerMessage by viewModel.providerMessage.collectAsStateWithLifecycle()
    val providerDrafts by viewModel.providerDrafts.collectAsStateWithLifecycle()
    val editingProviders by viewModel.editingProviders.collectAsStateWithLifecycle()
    val storedProviderKeys by viewModel.storedProviderKeys.collectAsStateWithLifecycle()
    val providersLoaded by viewModel.providersLoaded.collectAsStateWithLifecycle()
    val providersLoadError by viewModel.providersLoadError.collectAsStateWithLifecycle()
    val themePreset by viewModel.themePreset.collectAsStateWithLifecycle()
    val notesSaveFolderUri by viewModel.notesSaveFolderUri.collectAsStateWithLifecycle()
    val archiveMessageText = archiveMessage?.resolve(context).orEmpty()
    val notesFolderMessageText = notesFolderMessage?.resolve(context).orEmpty()
    val providerMessageText = providerMessage?.resolve(context).orEmpty()
    val providersLoadErrorText = providersLoadError?.resolve(context)
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri != null) {
            viewModel.export(uri)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsGranted = granted
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.import(uri)
        }
    }
    val notesFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    IntentFlags.ReadWrite,
                )
            }
            viewModel.setNotesSaveFolder(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(SidequestSpacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SidequestSpacing.SectionGap),
    ) {
        item {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_appearance_title),
                subtitle = stringResource(R.string.settings_section_appearance_subtitle),
            )
        }
        item {
            ScaffoldCard(
                title = stringResource(R.string.settings_theme_title),
                subtitle = stringResource(R.string.settings_theme_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                    ThemePreset.entries.forEach { preset ->
                        val palette = paletteFor(preset)
                        val presetLabel = stringResource(themePresetLabelRes(preset))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap),
                        ) {
                            Button(
                                onClick = { viewModel.setThemePreset(preset) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    if (themePreset == preset) {
                                        stringResource(R.string.settings_theme_active_format, presetLabel)
                                    } else {
                                        presetLabel
                                    },
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(SidequestSpacing.TightItemGap)) {
                                androidx.compose.material3.Surface(
                                    color = palette.bgPrimary,
                                    modifier = Modifier.size(SidequestSpacing.ThemeSwatchSize),
                                ) {}
                                androidx.compose.material3.Surface(
                                    color = palette.accentPrimary,
                                    modifier = Modifier.size(SidequestSpacing.ThemeSwatchSize),
                                ) {}
                                androidx.compose.material3.Surface(
                                    color = palette.accentSecondary,
                                    modifier = Modifier.size(SidequestSpacing.ThemeSwatchSize),
                                ) {}
                            }
                        }
                    }
                }
            }
        }
        item {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_privacy_title),
                subtitle = stringResource(R.string.settings_section_privacy_subtitle),
            )
        }
        item {
            ScaffoldCard(
                title = stringResource(R.string.settings_security_title),
                subtitle = stringResource(R.string.settings_security_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                    InlineSupportText(stringResource(R.string.settings_security_local_storage_note))
                    PreferenceSwitchRow(
                        title = stringResource(R.string.settings_biometric_title),
                        supportingText = stringResource(R.string.settings_biometric_supporting_text),
                        checked = biometricEnabled,
                        onCheckedChange = viewModel::setBiometricEnabled,
                    )
                }
            }
        }
        item {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_alerts_title),
                subtitle = stringResource(R.string.settings_section_alerts_subtitle),
            )
        }
        item {
            ScaffoldCard(
                title = stringResource(R.string.settings_notifications_title),
                subtitle = stringResource(R.string.settings_notifications_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                    Text(
                        text = stringResource(
                            R.string.settings_notifications_status_format,
                            stringResource(
                                if (notificationsGranted) {
                                    R.string.settings_status_granted
                                } else {
                                    R.string.settings_status_missing
                                },
                            ),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    InlineSupportText(
                        stringResource(
                            if (notificationsGranted) {
                                R.string.settings_notifications_enabled_help
                            } else {
                                R.string.settings_notifications_missing_help
                            },
                        ),
                    )
                    if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_grant_notifications))
                        }
                    }
                }
            }
        }
        item {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_notes_title),
                subtitle = stringResource(R.string.settings_section_notes_subtitle),
            )
        }
        item {
            ScaffoldCard(
                title = stringResource(R.string.settings_notes_folder_title),
                subtitle = stringResource(R.string.settings_notes_folder_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                    InlineSupportText(stringResource(R.string.settings_notes_folder_scope_note))
                    Text(
                        text = notesSaveFolderUri
                            ?.let(::folderLabelFromTreeUri)
                            ?.let { stringResource(R.string.settings_notes_folder_current_format, it) }
                            ?: stringResource(R.string.settings_notes_folder_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (notesFolderMessageText.isNotBlank()) {
                        InlineSupportText(notesFolderMessageText)
                    }
                    SplitActionRow {
                        Button(
                            onClick = { notesFolderLauncher.launch(null) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(
                                    if (notesSaveFolderUri == null) {
                                        R.string.settings_notes_folder_choose
                                    } else {
                                        R.string.settings_notes_folder_change
                                    },
                                ),
                            )
                        }
                        TextButton(
                            onClick = viewModel::clearNotesSaveFolder,
                            enabled = notesSaveFolderUri != null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.settings_notes_folder_clear))
                        }
                    }
                }
            }
        }
        item {
            ScaffoldCard(
                title = stringResource(R.string.settings_backup_title),
                subtitle = stringResource(R.string.settings_backup_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                    InlineSupportText(stringResource(R.string.settings_backup_scope_note))
                    InlineSupportText(stringResource(R.string.settings_backup_restore_note))
                    if (archiveMessageText.isNotBlank()) {
                        InlineSupportText(archiveMessageText)
                    }
                    SplitActionRow {
                        Button(
                            onClick = { exportLauncher.launch("sidequest-export.zip") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.settings_export_button))
                        }
                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.settings_import_button))
                        }
                    }
                }
            }
        }
        item {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_ai_title),
                subtitle = stringResource(R.string.settings_section_ai_subtitle),
            )
        }
        item {
            ScaffoldCard(
                title = stringResource(R.string.settings_ai_title),
                subtitle = stringResource(R.string.settings_ai_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                    InlineSupportText(stringResource(R.string.settings_provider_single_active_note))
                    InlineSupportText(stringResource(R.string.settings_ai_remote_processing_note))
                    if (providerMessageText.isNotBlank()) {
                        InlineSupportText(providerMessageText)
                    }
                }
            }
        }
        if (!providersLoaded) {
            item {
                LoadingStateCard(
                    title = stringResource(R.string.settings_providers_loading_title),
                    subtitle = stringResource(R.string.settings_providers_loading_subtitle),
                )
            }
        } else if (providersLoadErrorText != null) {
            item {
                ErrorStateCard(
                    title = stringResource(R.string.settings_providers_error_title),
                    subtitle = providersLoadErrorText,
                    supportingLines = listOf(stringResource(R.string.settings_providers_error_help)),
                    primaryAction = StateShellAction(
                        label = stringResource(R.string.settings_retry_provider_load),
                        onClick = viewModel::refreshProviders,
                    ),
                )
            }
        } else if (providers.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.settings_providers_empty_title),
                    subtitle = stringResource(R.string.settings_providers_empty_subtitle),
                    primaryAction = StateShellAction(
                        label = stringResource(R.string.settings_retry_provider_load),
                        onClick = viewModel::refreshProviders,
                    ),
                )
            }
        }
        items(providers, key = { it.kind.name }) { provider ->
            val isEditing = editingProviders.contains(provider.kind)
            val storedKey = storedProviderKeys[provider.kind].orEmpty()
            val providerName = providerDisplayName(provider.kind)
            val providerStatus = stringResource(
                when {
                    provider.enabled -> R.string.settings_status_provider_active
                    provider.configured -> R.string.settings_status_provider_ready
                    else -> R.string.settings_status_not_configured
                },
            )
            val providerToggleSupport = stringResource(
                when {
                    provider.enabled -> R.string.settings_provider_toggle_enabled_help
                    provider.configured -> R.string.settings_provider_toggle_inactive_help
                    else -> R.string.settings_provider_toggle_unconfigured_help
                },
                providerName,
            )
            ScaffoldCard(
                title = providerName,
                subtitle = providerStatus,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.ItemGap)) {
                    PreferenceSwitchRow(
                        title = stringResource(R.string.settings_provider_toggle_title, providerName),
                        supportingText = providerToggleSupport,
                        checked = provider.enabled,
                        onCheckedChange = { enabled -> viewModel.setProviderEnabled(provider.kind, enabled) },
                        enabled = provider.configured,
                    )
                    OutlinedTextField(
                        value = if (isEditing) providerDrafts[provider.kind].orEmpty() else viewModel.redactProviderKey(storedKey),
                        onValueChange = { value -> if (isEditing) viewModel.updateDraft(provider.kind, value) },
                        label = { Text(stringResource(R.string.settings_provider_api_key_label, providerName)) },
                        readOnly = !isEditing,
                        visualTransformation = if (isEditing) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    InlineSupportText(
                        text = if (provider.configured) {
                            if (isEditing) {
                                stringResource(R.string.settings_provider_editing_help)
                            } else {
                                stringResource(R.string.settings_provider_existing_help)
                            }
                        } else {
                            stringResource(R.string.settings_provider_local_only_help, providerName)
                        },
                    )
                    if (isEditing) {
                        SplitActionRow {
                            Button(
                                onClick = { viewModel.save(provider.kind) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.settings_provider_save_key))
                            }
                            TextButton(
                                onClick = { viewModel.cancelEdit(provider.kind) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.settings_provider_cancel))
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.beginEdit(provider.kind) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(
                                    if (provider.configured) {
                                        R.string.settings_provider_edit_key
                                    } else {
                                        R.string.settings_provider_add_key
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
        item {
            SettingsSectionHeader(
                title = stringResource(R.string.settings_section_support_title),
                subtitle = stringResource(R.string.settings_section_support_subtitle),
            )
        }
        item {
            ScaffoldCard(
                title = stringResource(R.string.settings_device_readiness_title),
                subtitle = stringResource(R.string.settings_device_readiness_subtitle),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SidequestSpacing.TightItemGap)) {
                    Text(
                        stringResource(
                            R.string.settings_device_camera_status,
                            stringResource(
                                if (hasPermission(context, Manifest.permission.CAMERA)) {
                                    R.string.settings_status_granted
                                } else {
                                    R.string.settings_status_missing
                                },
                            ),
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.settings_device_notifications_status,
                            stringResource(
                                if (notificationsGranted) {
                                    R.string.settings_status_granted
                                } else {
                                    R.string.settings_status_missing
                                },
                            ),
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.settings_device_biometric_status,
                            stringResource(
                                if (biometricEnabled) {
                                    R.string.settings_status_enabled
                                } else {
                                    R.string.settings_status_disabled
                                },
                            ),
                        ),
                    )
                    Text(stringResource(R.string.settings_device_storage_status))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SidequestSpacing.Xxs),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = AccentPrimary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@StringRes
private fun themePresetLabelRes(preset: ThemePreset): Int = when (preset) {
    ThemePreset.SOLAR -> R.string.settings_theme_preset_solar
    ThemePreset.AETHER -> R.string.settings_theme_preset_aether
    ThemePreset.FROST -> R.string.settings_theme_preset_frost
    ThemePreset.HEARTH -> R.string.settings_theme_preset_hearth
    ThemePreset.CYBER -> R.string.settings_theme_preset_cyber
}

private fun providerDisplayName(kind: ProviderKind): String = when (kind) {
    ProviderKind.OPENAI -> "OpenAI"
    ProviderKind.ANTHROPIC -> "Anthropic"
    ProviderKind.NIM -> "NIM"
    ProviderKind.OPENROUTER -> "OpenRouter"
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun folderLabelFromTreeUri(rawUri: String): String {
    val treeUri = Uri.parse(rawUri)
    return DocumentsContract.getTreeDocumentId(treeUri)
        .substringAfterLast(':')
        .ifBlank { rawUri }
}

private object IntentFlags {
    const val ReadWrite = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securityService: SecurityService,
    private val archiveService: ArchiveService,
) : ViewModel() {
    private val _providers = MutableStateFlow<List<ProviderAvailability>>(emptyList())
    val providers: StateFlow<List<ProviderAvailability>> = _providers
    private val _archiveMessage = MutableStateFlow<UiText?>(null)
    val archiveMessage: StateFlow<UiText?> = _archiveMessage.asStateFlow()
    private val _notesFolderMessage = MutableStateFlow<UiText?>(null)
    val notesFolderMessage: StateFlow<UiText?> = _notesFolderMessage.asStateFlow()
    private val _providerMessage = MutableStateFlow<UiText?>(null)
    val providerMessage: StateFlow<UiText?> = _providerMessage.asStateFlow()
    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()
    private val _providerDrafts = MutableStateFlow<Map<ProviderKind, String>>(emptyMap())
    val providerDrafts: StateFlow<Map<ProviderKind, String>> = _providerDrafts.asStateFlow()
    private val _storedProviderKeys = MutableStateFlow<Map<ProviderKind, String>>(emptyMap())
    val storedProviderKeys: StateFlow<Map<ProviderKind, String>> = _storedProviderKeys.asStateFlow()
    private val _providersLoaded = MutableStateFlow(false)
    val providersLoaded: StateFlow<Boolean> = _providersLoaded.asStateFlow()
    private val _providersLoadError = MutableStateFlow<UiText?>(null)
    val providersLoadError: StateFlow<UiText?> = _providersLoadError.asStateFlow()
    private val _editingProviders = MutableStateFlow<Set<ProviderKind>>(emptySet())
    val editingProviders: StateFlow<Set<ProviderKind>> = _editingProviders.asStateFlow()
    private val _themePreset = MutableStateFlow(ThemePreset.CYBER)
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()
    private val _notesSaveFolderUri = MutableStateFlow<String?>(null)
    val notesSaveFolderUri: StateFlow<String?> = _notesSaveFolderUri.asStateFlow()

    init {
        refreshProviders()
        viewModelScope.launch {
            securityService.observeUserPreferences().collect { preferences ->
                _biometricEnabled.value = preferences.biometricLockEnabled
                _themePreset.value = ThemePreset.entries.firstOrNull { it.name == preferences.themePresetName } ?: ThemePreset.CYBER
                _notesSaveFolderUri.value = preferences.notesSaveFolderUri
            }
        }
    }

    fun setNotesSaveFolder(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                securityService.setNotesSaveFolderUri(uri.toString())
            }.fold(
                onSuccess = {
                    _notesFolderMessage.value = UiText.resource(R.string.settings_notes_folder_updated)
                    _notesSaveFolderUri.value = uri.toString()
                },
                onFailure = { error ->
                    _notesFolderMessage.value = errorOrResource(
                        message = error.message,
                        fallback = R.string.settings_notes_folder_store_failed,
                    )
                },
            )
        }
    }

    fun clearNotesSaveFolder() {
        viewModelScope.launch {
            runCatching {
                securityService.setNotesSaveFolderUri(null)
            }.fold(
                onSuccess = {
                    _notesFolderMessage.value = UiText.resource(R.string.settings_notes_folder_cleared)
                    _notesSaveFolderUri.value = null
                },
                onFailure = { error ->
                    _notesFolderMessage.value = errorOrResource(
                        message = error.message,
                        fallback = R.string.settings_notes_folder_clear_failed,
                    )
                },
            )
        }
    }

    fun updateDraft(kind: ProviderKind, value: String) {
        _providerDrafts.value = _providerDrafts.value.toMutableMap().apply {
            this[kind] = value
        }
    }

    fun beginEdit(kind: ProviderKind) {
        _editingProviders.value = _editingProviders.value + kind
        _providerDrafts.value = _providerDrafts.value.toMutableMap().apply {
            this[kind] = ""
        }
    }

    fun cancelEdit(kind: ProviderKind) {
        _editingProviders.value = _editingProviders.value - kind
        _providerDrafts.value = _providerDrafts.value.toMutableMap().apply {
            remove(kind)
        }
        _providerMessage.value = null
    }

    fun save(kind: ProviderKind) {
        viewModelScope.launch {
            val key = _providerDrafts.value[kind].orEmpty().trim()
            if (key.isBlank()) {
                _providerMessage.value = UiText.resource(R.string.settings_provider_blank_key)
            } else {
                val providerName = providerDisplayName(kind)
                runCatching {
                    securityService.saveProviderKey(kind, key)
                }.fold(
                    onSuccess = {
                        _providerMessage.value = UiText.resource(
                            R.string.settings_provider_key_saved,
                            providerName,
                        )
                        _editingProviders.value = _editingProviders.value - kind
                        _providerDrafts.value = _providerDrafts.value.toMutableMap().apply {
                            remove(kind)
                        }
                    },
                    onFailure = { error ->
                        _providerMessage.value = errorOrResource(
                            message = error.message,
                            fallback = R.string.settings_provider_key_save_failed,
                            providerName,
                        )
                    },
                )
            }
            refreshProviders()
        }
    }

    fun setProviderEnabled(kind: ProviderKind, enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                securityService.setActiveProviderKind(if (enabled) kind else null)
            }.fold(
                onSuccess = {
                    val providerName = providerDisplayName(kind)
                    _providerMessage.value = when {
                        !enabled -> UiText.resource(R.string.settings_provider_disabled)
                        else -> UiText.resource(R.string.settings_provider_enabled, providerName)
                    }
                },
                onFailure = { error ->
                    _providerMessage.value = errorOrResource(
                        message = error.message,
                        fallback = R.string.settings_provider_switch_failed,
                    )
                },
            )
            refreshProviders()
        }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            _archiveMessage.value = archiveService.exportSnapshot(uri)
                .fold(
                    onSuccess = { UiText.resource(R.string.settings_snapshot_exported) },
                    onFailure = {
                        errorOrResource(
                            message = it.message,
                            fallback = R.string.settings_snapshot_export_failed,
                        )
                    },
                )
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _archiveMessage.value = when (val result = archiveService.validateImport(uri)) {
                is com.astrogolem.sidequest.core.data.model.ArchiveValidationResult.Invalid -> UiText.Dynamic(result.message)
                is com.astrogolem.sidequest.core.data.model.ArchiveValidationResult.Valid -> {
                    archiveService.importSnapshot(uri)
                        .fold(
                            onSuccess = { UiText.resource(R.string.settings_snapshot_imported) },
                            onFailure = {
                                errorOrResource(
                                    message = it.message,
                                    fallback = R.string.settings_snapshot_import_failed,
                                )
                            },
                        )
                }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityService.setBiometricLockEnabled(enabled)
            _biometricEnabled.value = enabled
        }
    }

    fun setThemePreset(preset: ThemePreset) {
        viewModelScope.launch {
            securityService.setThemePreset(preset.name)
            _themePreset.value = preset
        }
    }

    fun refreshProviders() {
        viewModelScope.launch {
            _providersLoadError.value = null
            runCatching {
                val availabilities = securityService.getProviderAvailability()
                val storedKeys = availabilities.associate { availability ->
                    availability.kind to securityService.getProviderKey(availability.kind).orEmpty()
                }
                availabilities to storedKeys
            }.fold(
                onSuccess = { (availabilities, storedKeys) ->
                    _providers.value = availabilities
                    _storedProviderKeys.value = storedKeys
                    _providersLoaded.value = true
                },
                onFailure = { error ->
                    _providersLoaded.value = true
                    _providersLoadError.value = errorOrResource(
                        message = error.message,
                        fallback = R.string.settings_providers_load_failed,
                    )
                },
            )
        }
    }

    fun redactProviderKey(value: String): String {
        if (value.isBlank()) return ""
        val suffix = value.takeLast(minOf(4, value.length))
        val mask = "*".repeat((value.length - suffix.length).coerceAtLeast(6))
        return "$mask$suffix"
    }
}

private fun errorOrResource(
    message: String?,
    @StringRes fallback: Int,
    vararg args: Any,
): UiText {
    return if (message.isNullOrBlank()) {
        UiText.resource(fallback, *args)
    } else {
        UiText.Dynamic(message)
    }
}
