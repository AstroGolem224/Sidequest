package com.astrogolem.sidequest.feature.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard

@Composable
fun CaptureRoute(
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            viewModel.import(uri)
        }
    }

    CaptureScreen(
        state = state,
        onImport = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

@Composable
private fun CaptureScreen(
    state: CaptureUiState,
    onImport: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(
                title = "Snap your chaos into a system",
                subtitle = "Import a note, receipt, desk photo or whiteboard and Sidequest will extract candidate missions.",
            ) {
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Text("Import Image")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (state.isImporting) "Processing latest capture..." else state.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        items(state.captures, key = { it.id }) { capture ->
            ScaffoldCard(
                title = capture.sourceLabel.replaceFirstChar { it.uppercase() },
                subtitle = "Status: ${capture.status.name.lowercase()}",
            ) {
                Text(text = capture.id, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
