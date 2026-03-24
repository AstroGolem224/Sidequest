package com.astrogolem.sidequest.feature.settings

import android.net.Uri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val archiveMessage by viewModel.archiveMessage.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val drafts = remember { mutableStateMapOf<ProviderKind, String>() }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            viewModel.export(uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.validateImport(uri)
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
                Text(text = archiveMessage, modifier = Modifier.padding(top = 8.dp))
                Button(
                    onClick = { exportLauncher.launch("sidequest-export.json") },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Export Snapshot")
                }
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Validate Import")
                }
                Text(text = "Biometric lock", modifier = Modifier.padding(top = 16.dp))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = viewModel::setBiometricEnabled,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        items(providers, key = { it.kind.name }) { provider ->
            ScaffoldCard(
                title = provider.kind.name,
                subtitle = if (provider.configured) "Configured" else "Not configured",
            ) {
                OutlinedTextField(
                    value = drafts[provider.kind].orEmpty(),
                    onValueChange = { drafts[provider.kind] = it },
                    label = { Text("${provider.kind.name} API key") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.save(provider.kind, drafts[provider.kind].orEmpty()) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Save key")
                }
            }
        }
    }
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
    private val _biometricEnabled = MutableStateFlow(false)
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    init {
        refresh()
    }

    fun save(kind: ProviderKind, key: String) {
        viewModelScope.launch {
            if (key.isNotBlank()) {
                securityService.saveProviderKey(kind, key)
            }
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

    fun validateImport(uri: Uri) {
        viewModelScope.launch {
            _archiveMessage.value = when (val result = archiveService.validateImport(uri)) {
                is com.astrogolem.sidequest.core.data.model.ArchiveValidationResult.Valid -> "Import archive is valid."
                is com.astrogolem.sidequest.core.data.model.ArchiveValidationResult.Invalid -> result.message
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
            _providers.value = securityService.getProviderAvailability()
            _biometricEnabled.value = securityService.isBiometricLockEnabled()
        }
    }
}
