package com.astrogolem.sidequest.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.data.model.CaptureDetailModel
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@Composable
fun CaptureRoute(
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraError by rememberSaveable { mutableStateOf<String?>(null) }
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            setEnabledUseCases(CameraControllerUseCases)
        }
    }

    DisposableEffect(controller, lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
        }
    }

    val hasCameraPermission = rememberCameraPermission()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            viewModel.import(uri)
        }
    }

    CaptureScreen(
        state = state,
        controller = controller,
        hasCameraPermission = hasCameraPermission.hasPermission,
        onRequestPermission = hasCameraPermission.requestPermission,
        pendingCaptureUri = pendingCaptureUri,
        cameraError = cameraError,
        onImport = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onCapture = {
            val outputFile = File(context.cacheDir, "camera-${System.currentTimeMillis()}.jpg")
            val outputUri = Uri.fromFile(outputFile)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            controller.takePicture(
                outputOptions,
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        pendingCaptureUri = outputUri.toString()
                        cameraError = null
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cameraError = exception.message ?: "Capture failed."
                    }
                },
            )
        },
        onRetake = {
            pendingCaptureUri?.let { uri ->
                runCatching { File(requireNotNull(Uri.parse(uri).path)).delete() }
            }
            pendingCaptureUri = null
            cameraError = null
        },
        onSave = {
            pendingCaptureUri?.let { uri ->
                viewModel.saveCameraCapture(Uri.parse(uri))
                pendingCaptureUri = null
                cameraError = null
            }
        },
    )
}

private const val CameraControllerUseCases = CameraController.IMAGE_CAPTURE

@Composable
private fun CaptureScreen(
    state: CaptureUiState,
    controller: LifecycleCameraController,
    hasCameraPermission: Boolean,
    onRequestPermission: () -> Unit,
    pendingCaptureUri: String?,
    cameraError: String?,
    onImport: () -> Unit,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            ScaffoldCard(
                title = "Snap your chaos into a system",
                subtitle = "Use the live camera for notes, receipts and whiteboards, then save only the captures worth processing.",
            ) {
                when {
                    !hasCameraPermission -> {
                        Text("Camera access is required for the live Sidequest capture flow.")
                        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text("Grant Camera Access")
                        }
                    }

                    pendingCaptureUri != null -> {
                        PreviewImage(uri = Uri.parse(pendingCaptureUri))
                        Button(onClick = onSave, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), enabled = !state.isSaving) {
                            Text(if (state.isSaving) "Saving..." else "Save Capture")
                        }
                        Button(onClick = onRetake, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), enabled = !state.isSaving) {
                            Text("Retake")
                        }
                    }

                    else -> {
                        CameraPreview(controller = controller)
                        Button(onClick = onCapture, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Text("Capture")
                        }
                    }
                }

                Button(onClick = onImport, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Import Image")
                }
                if (cameraError != null) {
                    Text(
                        text = cameraError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Text(
                    text = state.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
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

@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                this.controller = controller
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(24.dp)),
    )
}

@Composable
private fun PreviewImage(uri: Uri) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            imageView.setImageURI(uri)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(24.dp)),
    )
}

@Composable
private fun rememberCameraPermission(): CameraPermissionState {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    return remember(hasPermission, launcher) {
        CameraPermissionState(
            hasPermission = hasPermission,
            requestPermission = { launcher.launch(Manifest.permission.CAMERA) },
        )
    }
}

private data class CameraPermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit,
)

@Composable
fun CaptureDetailRoute(
    onOpenMission: (String) -> Unit,
    viewModel: CaptureDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    detail?.let { capture ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                ScaffoldCard(
                    title = capture.sourceLabel.replaceFirstChar { it.uppercase() },
                    subtitle = "Status: ${capture.status.name.lowercase()}",
                ) {
                    PreviewImage(uri = Uri.fromFile(File(capture.imagePath)))
                    if (capture.summary.isNotBlank()) {
                        Text("Summary: ${capture.summary}", modifier = Modifier.padding(top = 12.dp))
                    }
                    if (capture.ocrText.isNotBlank()) {
                        Text("OCR", modifier = Modifier.padding(top = 12.dp))
                        Text(capture.ocrText)
                    }
                    if (capture.linkedMissions.isNotEmpty()) {
                        Text("Linked missions", modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }

            items(capture.candidates, key = { it.id }) { candidate ->
                ScaffoldCard(
                    title = candidate.title,
                    subtitle = "Confidence ${(candidate.confidence * 100).toInt()}%",
                ) {
                    Text(candidate.body)
                }
            }

            items(capture.linkedMissions, key = { it.id }) { mission ->
                ScaffoldCard(
                    title = mission.title,
                    subtitle = "Mission | ${mission.status.name.lowercase()}",
                ) {
                    Button(
                        onClick = { onOpenMission(mission.id) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text("Open mission")
                    }
                }
            }
        }
    }
}

@HiltViewModel
class CaptureDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    captureRepository: CaptureRepository,
) : ViewModel() {
    private val captureId: String = checkNotNull(savedStateHandle["captureId"])

    val detail: StateFlow<CaptureDetailModel?> =
        captureRepository.observeCaptureDetail(captureId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
