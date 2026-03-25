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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.data.model.CaptureDetailModel
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.model.ExtractionStatus
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.theme.AccentCyan
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun CaptureRoute(
    onOpenCapture: (String) -> Unit,
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
        onOpenCapture = onOpenCapture,
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
    onOpenCapture: (String) -> Unit,
) {
    val latestCapture = state.captures.firstOrNull()
    val latestCaptureDone = latestCapture?.status?.name == "DONE"

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !hasCameraPermission -> TacticalCaptureFallback(
                title = "Visual sensors offline",
                body = "Grant camera access to restore live scanning and HUD targeting.",
                modifier = Modifier.fillMaxSize(),
            )

            pendingCaptureUri != null -> PreviewImage(
                uri = Uri.parse(pendingCaptureUri),
                modifier = Modifier.fillMaxSize(),
            )

            else -> CameraPreview(
                controller = controller,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.58f)),
                    ),
                ),
        )

        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopStart)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Visual sensors offline", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Grant camera access to restore the live lens. Until then, Sidequest can only work from imported images.",
                    color = TextSecondary,
                )
            }
        }

        Box(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            LensActionBar(
                pendingCapture = pendingCaptureUri != null,
                hasReadyCapture = latestCaptureDone,
                onCapture = onCapture,
                onImport = onImport,
                onRetake = onRetake,
                onSave = onSave,
                onOpenCapture = { latestCapture?.id?.let(onOpenCapture) },
            )
        }

        if (!hasCameraPermission) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 116.dp)
                    .fillMaxWidth(),
            ) {
                Text("Restore Vision")
            }
        }
    }
}

@Composable
private fun TacticalCaptureFallback(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(BgGlow.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.92f)))),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(20.dp),
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 74.dp),
        )
    }
}

@Composable
private fun LensActionBar(
    pendingCapture: Boolean,
    hasReadyCapture: Boolean,
    onCapture: () -> Unit,
    onImport: () -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    onOpenCapture: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(268.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0x6605070A),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            OutlinedIconButton(
                onClick = if (pendingCapture) onRetake else onImport,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    painter = painterResource(
                        id = if (pendingCapture) android.R.drawable.ic_menu_revert else android.R.drawable.ic_menu_gallery,
                    ),
                    contentDescription = if (pendingCapture) "Retake capture" else "Photo library",
                    modifier = Modifier.size(18.dp),
                )
            }
            FilledIconButton(
                onClick = if (pendingCapture) onSave else onCapture,
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = AccentCyan,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(
                    painter = painterResource(
                        id = if (pendingCapture) android.R.drawable.ic_menu_save else android.R.drawable.ic_menu_camera,
                    ),
                    contentDescription = if (pendingCapture) "Save capture" else "Capture image",
                    modifier = Modifier.size(28.dp),
                )
            }
            OutlinedIconButton(
                onClick = onOpenCapture,
                enabled = hasReadyCapture && !pendingCapture,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_view),
                    contentDescription = "Open latest intel",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                this.controller = controller
                scaleType = PreviewView.ScaleType.FILL_CENTER
                alpha = 0.96f
            }
        },
        modifier = Modifier
            .then(modifier),
    )
}

@Composable
private fun PreviewImage(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            imageView.setImageURI(uri)
        },
        modifier = modifier,
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
    val clipboardManager = LocalClipboardManager.current
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

            extractionSections(capture).forEach { (kind, candidates) ->
                item(key = "section-$kind") {
                    ScaffoldCard(
                        title = sectionTitle(kind, candidates.size),
                        subtitle = sectionSubtitle(kind),
                    ) {
                        Text("These items stay attached to the capture unless you act on them.")
                    }
                }
                items(candidates, key = { it.id }) { candidate ->
                    CaptureDetailCandidateCard(
                        candidate = candidate,
                        onPrimaryAction = { viewModel.promoteCandidate(candidate.id) },
                        onCopy = { clipboardManager.setText(AnnotatedString(candidate.body)) },
                    )
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
    private val missionRepository: MissionRepository,
) : ViewModel() {
    private val captureId: String = checkNotNull(savedStateHandle["captureId"])

    val detail: StateFlow<CaptureDetailModel?> =
        captureRepository.observeCaptureDetail(captureId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun promoteCandidate(candidateId: String) {
        viewModelScope.launch {
            missionRepository.promoteCandidate(candidateId)
        }
    }
}

@Composable
private fun CaptureDetailCandidateCard(
    candidate: ExtractionCandidate,
    onPrimaryAction: () -> Unit,
    onCopy: () -> Unit,
) {
    ScaffoldCard(
        title = candidate.title,
        subtitle = buildCandidateSubtitle(candidate),
    ) {
        AssistChip(
            onClick = {},
            label = { Text(candidate.kind.name.lowercase()) },
            modifier = Modifier.padding(bottom = 12.dp),
        )
        candidate.dueAt?.let { dueAt ->
            Text(
                text = "Detected date: ${formatTimestamp(dueAt)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(candidate.body)
        if (candidate.status == ExtractionStatus.CANDIDATE) {
            when (candidate.kind) {
                ExtractionKind.TASK -> {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text("Create Mission")
                    }
                }
                ExtractionKind.DATE -> {
                    Button(
                        onClick = onPrimaryAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text("Create Reminder")
                    }
                }
                ExtractionKind.REFERENCE,
                ExtractionKind.FACT,
                -> {
                    Button(
                        onClick = onCopy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    ) {
                        Text("Copy")
                    }
                }
            }
        }
    }
}

private fun extractionSections(capture: CaptureDetailModel): List<Pair<ExtractionKind, List<ExtractionCandidate>>> {
    val order = listOf(
        ExtractionKind.TASK,
        ExtractionKind.DATE,
        ExtractionKind.REFERENCE,
        ExtractionKind.FACT,
    )
    val grouped = capture.candidates.groupBy { it.kind }
    return order.mapNotNull { kind ->
        grouped[kind]?.takeIf { it.isNotEmpty() }?.let { kind to it }
    }
}

private fun sectionTitle(kind: ExtractionKind, count: Int): String {
    val label = when (kind) {
        ExtractionKind.TASK -> "Tasks"
        ExtractionKind.DATE -> "Dates"
        ExtractionKind.REFERENCE -> "References"
        ExtractionKind.FACT -> "Facts"
    }
    return "$label ($count)"
}

private fun sectionSubtitle(kind: ExtractionKind): String {
    return when (kind) {
        ExtractionKind.TASK -> "Actionable lines that can become missions."
        ExtractionKind.DATE -> "Recognized deadlines or time anchors you can turn into reminders."
        ExtractionKind.REFERENCE -> "IDs, codes and reference values kept for lookup."
        ExtractionKind.FACT -> "Context captured for search and recall."
    }
}

private fun buildCandidateSubtitle(candidate: ExtractionCandidate): String {
    val base = "${candidate.kind.name.lowercase()} | ${(candidate.confidence * 100).toInt()}% confidence"
    return candidate.dueAt?.let { "$base | ${formatTimestamp(it)}" } ?: base
}

private fun formatTimestamp(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}

private fun intelTitle(capture: CaptureSummary): String {
    return when (capture.sourceLabel.lowercase()) {
        "camera" -> "Field Scan"
        "import" -> "Recovered Intel"
        else -> capture.sourceLabel.replaceFirstChar { it.uppercase() }
    }
}
