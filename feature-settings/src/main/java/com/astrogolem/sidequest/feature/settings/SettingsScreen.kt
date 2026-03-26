package com.astrogolem.sidequest.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.ProviderAvailability
import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.repo.ArchiveService
import com.astrogolem.sidequest.core.data.repo.SecurityService
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
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
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val providerMessage by viewModel.providerMessage.collectAsStateWithLifecycle()
    val providerDrafts by viewModel.providerDrafts.collectAsStateWithLifecycle()
    val editingProviders by viewModel.editingProviders.collectAsStateWithLifecycle()
    val storedProviderKeys by viewModel.storedProviderKeys.collectAsStateWithLifecycle()
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(
                title = "Privacy + Provider Setup",
                subtitle = "Sidequest works offline by default. Add a provider key only if you want enhanced extraction.",
            ) {
                Text("Exports are manual snapshot bundles. No sync service is active in v1.")
                Text("When an OpenAI key is configured, Sidequest can inspect the capture image plus OCR text to propose quests, dates, references, and facts.")
                Text(text = archiveMessage, modifier = Modifier.padding(top = 8.dp))
                Button(
                    onClick = { exportLauncher.launch("sidequest-export.zip") },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Export Snapshot")
                }
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Import Snapshot")
                }
                Text(text = "Biometric lock", modifier = Modifier.padding(top = 16.dp))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = viewModel::setBiometricEnabled,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(text = "Notifications", modifier = Modifier.padding(top = 16.dp))
                Text(
                    text = if (notificationsGranted) "Granted" else "Missing permission",
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text("Grant notifications")
                    }
                }
            }
        }
        if (providers.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "No providers available",
                    subtitle = "Enhanced extraction is optional. The local OCR pipeline still works without any key.",
                ) {}
            }
        }
        item {
            ScaffoldCard(
                title = "Device Readiness",
                subtitle = "Quick smoke-checks for critical Sidequest flows on this device.",
            ) {
                Text("Camera permission: ${if (hasPermission(context, Manifest.permission.CAMERA)) "granted" else "missing"}")
                Text("Notification permission: ${if (notificationsGranted) "granted" else "missing"}")
                Text("Biometric lock: ${if (biometricEnabled) "enabled" else "disabled"}")
                Text("Offline-first storage: local DB active")
            }
        }
        items(providers, key = { it.kind.name }) { provider ->
            val isEditing = editingProviders.contains(provider.kind)
            val storedKey = storedProviderKeys[provider.kind].orEmpty()
            ScaffoldCard(
                title = provider.kind.name,
                subtitle = when {
                    provider.enabled && provider.kind == ProviderKind.OPENAI -> "Active for AI analysis"
                    provider.enabled -> "Selected provider (local fallback)"
                    provider.configured -> "Key stored on device"
                    else -> "Not configured"
                },
            ) {
                Text(
                    text = if (provider.kind == ProviderKind.OPENAI) {
                        "Only one AI provider can be active. Keys stay stored locally even when the provider is disabled."
                    } else {
                        "You can already store and select this provider. In this build, capture analysis still falls back to the local pipeline unless OpenAI is the active provider."
                    },
                )
                Text(text = "AI provider active", modifier = Modifier.padding(top = 12.dp))
                Switch(
                    checked = provider.enabled,
                    onCheckedChange = { enabled -> viewModel.setProviderEnabled(provider.kind, enabled) },
                    enabled = provider.configured,
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = if (isEditing) providerDrafts[provider.kind].orEmpty() else viewModel.redactProviderKey(storedKey),
                    onValueChange = { value -> if (isEditing) viewModel.updateDraft(provider.kind, value) },
                    label = { Text("${provider.kind.name} API key") },
                    readOnly = !isEditing,
                    visualTransformation = if (isEditing) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = if (provider.configured) {
                        if (isEditing) {
                            "Pasted keys are hidden while editing. Saving keeps the key stored but redacts it on screen."
                        } else {
                            "A key is already stored on this device. Tap Edit to overwrite it."
                        }
                    } else {
                        "Leave this empty if you want to stay fully local-only."
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (isEditing) {
                    Button(
                        onClick = { viewModel.save(provider.kind) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text("Save key")
                    }
                    TextButton(
                        onClick = { viewModel.cancelEdit(provider.kind) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = { viewModel.beginEdit(provider.kind) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(if (provider.configured) "Edit key" else "Add key")
                    }
                }
                if (providerMessage.isNotBlank()) {
                    Text(
                        text = providerMessage,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

private fun hasPermission(context: android.content.Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val securityService: SecurityService,
    private val archiveService: ArchiveService,
) : ViewModel() {
    private val _providers = MutableStateFlow<List<ProviderAvailability>>(emptyList())
    val providers: StateFlow<List<ProviderAvailability>> = _providers
    private val _archiveMessage = MutableStateFlow("No archive action yet.")
    val archiveMessage: StateFlow<String> = _archiveMessage.asStateFlow()
    private val _providerMessage = MutableStateFlow("")
    val providerMessage: StateFlow<String> = _providerMessage.asStateFlow()
    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()
    private val _providerDrafts = MutableStateFlow<Map<ProviderKind, String>>(emptyMap())
    val providerDrafts: StateFlow<Map<ProviderKind, String>> = _providerDrafts.asStateFlow()
    private val _storedProviderKeys = MutableStateFlow<Map<ProviderKind, String>>(emptyMap())
    val storedProviderKeys: StateFlow<Map<ProviderKind, String>> = _storedProviderKeys.asStateFlow()
    private val _editingProviders = MutableStateFlow<Set<ProviderKind>>(emptySet())
    val editingProviders: StateFlow<Set<ProviderKind>> = _editingProviders.asStateFlow()

    init {
        refresh()
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
        _providerMessage.value = ""
    }

    fun save(kind: ProviderKind) {
        viewModelScope.launch {
            val key = _providerDrafts.value[kind].orEmpty().trim()
            if (key.isBlank()) {
                _providerMessage.value = "Enter an API key first or leave providers unused."
            } else {
                runCatching {
                    securityService.saveProviderKey(kind, key)
                }.fold(
                    onSuccess = {
                        _providerMessage.value = "${kind.name} key saved on this device."
                        _editingProviders.value = _editingProviders.value - kind
                        _providerDrafts.value = _providerDrafts.value.toMutableMap().apply {
                            remove(kind)
                        }
                    },
                    onFailure = { error ->
                        _providerMessage.value = error.message ?: "${kind.name} key could not be saved."
                    },
                )
            }
            refresh()
        }
    }

    fun setProviderEnabled(kind: ProviderKind, enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                securityService.setActiveProviderKind(if (enabled) kind else null)
            }.fold(
                onSuccess = {
                    _providerMessage.value = when {
                        !enabled -> "AI provider disabled. Sidequest is running local-only."
                        kind == ProviderKind.OPENAI -> "${kind.name} is now the active AI provider."
                        else -> "${kind.name} is selected. This build still uses local-only analysis unless OpenAI is active."
                    }
                },
                onFailure = { error ->
                    _providerMessage.value = error.message ?: "Provider switch failed."
                },
            )
            refresh()
        }
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            _archiveMessage.value = archiveService.exportSnapshot(uri)
                .fold(
                    onSuccess = { "Snapshot exported." },
                    onFailure = { it.message ?: "Export failed." },
                )
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _archiveMessage.value = when (val result = archiveService.validateImport(uri)) {
                is com.astrogolem.sidequest.core.data.model.ArchiveValidationResult.Invalid -> result.message
                is com.astrogolem.sidequest.core.data.model.ArchiveValidationResult.Valid -> {
                    archiveService.importSnapshot(uri)
                        .fold(
                            onSuccess = { "Snapshot imported." },
                            onFailure = { it.message ?: "Import failed." },
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

    private fun refresh() {
        viewModelScope.launch {
            val availabilities = securityService.getProviderAvailability()
            _providers.value = availabilities
            _storedProviderKeys.value = availabilities.associate { availability ->
                availability.kind to securityService.getProviderKey(availability.kind).orEmpty()
            }
            _biometricEnabled.value = securityService.isBiometricLockEnabled()
        }
    }

    fun redactProviderKey(value: String): String {
        if (value.isBlank()) return ""
        val suffix = value.takeLast(minOf(4, value.length))
        val mask = "*".repeat((value.length - suffix.length).coerceAtLeast(6))
        return "$mask$suffix"
    }
}
