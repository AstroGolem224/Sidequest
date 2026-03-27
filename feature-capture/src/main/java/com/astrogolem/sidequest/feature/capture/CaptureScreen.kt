package com.astrogolem.sidequest.feature.capture

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.ui.components.GlassCard
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.data.model.CaptureDetailModel
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.model.ExtractionStatus
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.ProcessingOrchestrator
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentCyan
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.BgPanel
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.CardStrokeStrong
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
        onSetAiFirstEnabled = viewModel::setAiFirstEnabled,
        onConsumeActionFeedback = viewModel::clearActionFeedback,
        onDismissReview = viewModel::dismissReviewDrawer,
        onPromoteCandidate = viewModel::promoteCandidate,
        onDismissCandidate = viewModel::dismissCandidate,
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
    onSetAiFirstEnabled: (Boolean) -> Unit,
    onConsumeActionFeedback: () -> Unit,
    onDismissReview: () -> Unit,
    onPromoteCandidate: (String) -> Unit,
    onDismissCandidate: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
) {
    val latestCapture = state.captures.firstOrNull()
    val latestCaptureDone = latestCapture?.status?.name == "DONE"
    LaunchedEffect(state.actionFeedback) {
        if (state.actionFeedback != null) {
            delay(1800)
            onConsumeActionFeedback()
        }
    }

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

        LensScanlineOverlay(modifier = Modifier.fillMaxSize())

        AiFirstToggle(
            enabled = state.aiFirstEnabled,
            onCheckedChange = onSetAiFirstEnabled,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 156.dp),
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

        AnimatedVisibility(
            visible = latestCaptureDone && pendingCaptureUri == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopEnd)
                .padding(top = 18.dp, end = 16.dp),
        ) {
            OutlinedButton(onClick = { latestCapture?.id?.let(onOpenCapture) }) {
                Icon(
                    imageVector = SidequestIcons.Intel,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Text("Open Details", modifier = Modifier.padding(start = 8.dp))
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

        if (state.showReviewDrawer) {
            val reviewCapture = state.reviewCapture
            if (reviewCapture != null) {
                PostScanReviewDrawer(
                    reviewCapture = reviewCapture,
                    reviewCandidates = state.reviewCandidates,
                    onDismissDrawer = onDismissReview,
                    onPromoteCandidate = onPromoteCandidate,
                    onDismissCandidate = onDismissCandidate,
                    onOpenCapture = { onOpenCapture(reviewCapture.id) },
                )
            }
        } else if (cameraError != null || state.actionFeedback != null || state.isSaving || state.lastMessage != DefaultCaptureMessage) {
            CaptureStatusBanner(
                message = cameraError ?: state.actionFeedback ?: state.lastMessage,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 108.dp),
            )
        }
    }
}

@Composable
private fun AiFirstToggle(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = "AI",
            style = MaterialTheme.typography.titleSmall,
            color = AccentSecondary,
        )
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun CaptureStatusBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = BgPanel.copy(alpha = 0.94f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardStrokeStrong.copy(alpha = 0.4f)),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AccentSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PostScanReviewDrawer(
    reviewCapture: CaptureDetailModel,
    reviewCandidates: List<ExtractionCandidate>,
    onDismissDrawer: () -> Unit,
    onPromoteCandidate: (String) -> Unit,
    onDismissCandidate: (String) -> Unit,
    onOpenCapture: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissDrawer,
        containerColor = BgPanel.copy(alpha = 0.98f),
        scrimColor = Color.Black.copy(alpha = 0.42f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = CardStrokeStrong,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                when (reviewCapture.status) {
                    CaptureProcessingStatus.PROCESSING -> ReviewDrawerHeader(
                        title = "Analyzing objective",
                        subtitle = "OCR and classification are running in the background. Keep the drawer open or jump into intel when it completes.",
                    )
                    CaptureProcessingStatus.DONE -> ReviewDrawerHeader(
                        title = if (reviewCandidates.isEmpty()) "Intel archived" else "Review suggested quests",
                        subtitle = if (reviewCandidates.isEmpty()) {
                            "This scan produced search-worthy intel but no strong quest candidates."
                        } else {
                            "Confirm or veto the fresh mission suggestions before they enter your quest log."
                        },
                    )
                    CaptureProcessingStatus.FAILED -> ReviewDrawerHeader(
                        title = "Scan analysis failed",
                        subtitle = "The source image is still stored locally. Open the intel detail to inspect OCR and retry from the source.",
                    )
                    CaptureProcessingStatus.PENDING -> ReviewDrawerHeader(
                        title = "Capture queued",
                        subtitle = "The image is saved locally and waiting for the background worker.",
                    )
                }
            }

            when (reviewCapture.status) {
                CaptureProcessingStatus.PROCESSING,
                CaptureProcessingStatus.PENDING,
                -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = AccentPrimary,
                            )
                            Text(
                                text = "Analyzing Objective...",
                                style = MaterialTheme.typography.titleSmall,
                                color = AccentSecondary,
                            )
                        }
                    }
                }

                CaptureProcessingStatus.DONE -> {
                    if (reviewCandidates.isEmpty()) {
                        item {
                            GlassCard(
                                title = "No quests deployed",
                                subtitle = "The scan stayed in memory mode. Dates, facts, and references are still preserved inside the intel view.",
                            ) {
                                Text(
                                    text = reviewCapture.summary.ifBlank { "Open intel to inspect OCR, dates, and linked facts." },
                                    color = TextSecondary,
                                )
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = reviewCandidates,
                            key = { _, candidate -> candidate.id },
                        ) { index, candidate ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = index * 70)) +
                                    slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(durationMillis = 220, delayMillis = index * 70),
                                    ),
                            ) {
                                FreshQuestCandidateCard(
                                    candidate = candidate,
                                    onPromote = { onPromoteCandidate(candidate.id) },
                                    onDismiss = { onDismissCandidate(candidate.id) },
                                )
                            }
                        }
                    }
                }

                CaptureProcessingStatus.FAILED -> {
                    item {
                        GlassCard(
                            title = "Manual recovery",
                            subtitle = "Use the intel detail to inspect what was saved and decide whether to retake or re-import the source.",
                        ) {
                            Text(
                                text = "Sidequest kept the original image. Nothing was silently discarded.",
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismissDrawer,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Later")
                    }
                    Button(
                        onClick = onOpenCapture,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (reviewCapture.status == CaptureProcessingStatus.DONE) "Open Details" else "Open Source")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDrawerHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, color = TextSecondary)
    }
}

@Composable
private fun FreshQuestCandidateCard(
    candidate: ExtractionCandidate,
    onPromote: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by rememberSaveable(candidate.id) { mutableStateOf(false) }
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, CardStrokeStrong.copy(alpha = 0.4f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = candidate.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) SidequestIcons.Minus else SidequestIcons.Plus,
                        contentDescription = if (expanded) "Collapse quest" else "Expand quest",
                        tint = AccentSecondary,
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${(candidate.confidence * 100).toInt()}% confidence",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(candidate.body, color = TextSecondary)
                    Text(
                        text = candidate.reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentPrimary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = onPromote,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Deploy Quest")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LensScanlineOverlay(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "lens-scanlines")
    val sweepProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
        ),
        label = "scanline-sweep",
    )
    val density = LocalDensity.current
    val lineSpacing = with(density) { 4.dp.toPx() }
    val beamHeight = with(density) { 120.dp.toPx() }

    Canvas(modifier = modifier) {
        var y = 0f
        while (y <= size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.055f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += lineSpacing
        }

        val beamTop = ((size.height + beamHeight) * sweepProgress) - beamHeight
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    AccentCyan.copy(alpha = 0.04f),
                    AccentCyan.copy(alpha = 0.14f),
                    Color.Transparent,
                ),
                startY = beamTop,
                endY = beamTop + beamHeight,
            ),
            topLeft = Offset(0f, beamTop),
            size = Size(size.width, beamHeight),
        )
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
                    imageVector = if (pendingCapture) SidequestIcons.Retake else SidequestIcons.Gallery,
                    contentDescription = if (pendingCapture) "Retake capture" else "Photo library",
                    modifier = Modifier.size(18.dp),
                )
            }
            val transition = rememberInfiniteTransition(label = "lens-trigger")
            val pulseRadius by transition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400),
                ),
                label = "lens-trigger-radius",
            )
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.26f,
                targetValue = 0.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400),
                ),
                label = "lens-trigger-alpha",
            )
            Box(
                modifier = Modifier.size(88.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(88.dp * pulseRadius)) {
                    drawCircle(color = AccentCyan.copy(alpha = pulseAlpha))
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
                        imageVector = if (pendingCapture) SidequestIcons.Save else SidequestIcons.Camera,
                        contentDescription = if (pendingCapture) "Save capture" else "Capture image",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            OutlinedIconButton(
                onClick = onOpenCapture,
                enabled = hasReadyCapture && !pendingCapture,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = SidequestIcons.Intel,
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
private fun ZoomableCaptureImage(
    imagePath: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap = remember(imagePath) { loadBitmapWithOrientation(context, imagePath) }
    var scale by remember(imagePath) { mutableStateOf(1f) }
    var offset by remember(imagePath) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(imagePath) { mutableStateOf(IntSize.Zero) }

    if (bitmap == null) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            color = BgPanel.copy(alpha = 0.78f),
        ) {
            Text(
                text = "image preview unavailable",
                color = TextSecondary,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val aspectRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .onSizeChanged { containerSize = it }
                .pointerInput(imagePath) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 5f)
                        val maxX = (containerSize.width * (nextScale - 1f)) / 2f
                        val maxY = (containerSize.height * (nextScale - 1f)) / 2f
                        val nextOffset = if (nextScale <= 1.02f) {
                            Offset.Zero
                        } else {
                            Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                            )
                        }
                        scale = nextScale
                        offset = nextOffset
                    }
                },
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Capture image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
        Text(
            text = "pinch to zoom and drag to inspect the full scan",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun loadBitmapWithOrientation(
    context: android.content.Context,
    imagePath: String,
): Bitmap? {
    val imageUri = resolveStoredImageUri(imagePath)
    val bytes = openStoredImageInputStream(context, imageUri)?.use { it.readBytes() } ?: return null
    val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val exif = runCatching { ExifInterface(java.io.ByteArrayInputStream(bytes)) }.getOrNull() ?: return source
    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
        return source
    }
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.preScale(-1f, 1f)
            matrix.postRotate(270f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.preScale(-1f, 1f)
            matrix.postRotate(90f)
        }
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun resolveStoredImageUri(reference: String): Uri {
    val parsed = Uri.parse(reference)
    return if (parsed.scheme.isNullOrBlank()) Uri.fromFile(File(reference)) else parsed
}

private fun openStoredImageInputStream(
    context: android.content.Context,
    uri: Uri,
) = when (uri.scheme) {
    "file" -> uri.path?.let { path -> File(path).takeIf(File::exists)?.inputStream() }
    else -> context.contentResolver.openInputStream(uri)
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
    onDeleted: () -> Unit,
    viewModel: CaptureDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val feedback by viewModel.feedback.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(1800)
            viewModel.clearFeedback()
        }
    }
    detail?.let { capture ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item {
                    ScaffoldCard(
                        title = capture.sourceLabel.replaceFirstChar { it.uppercase() },
                        subtitle = buildString {
                            append("Status: ${capture.status.name.lowercase()}")
                            capture.documentType?.let { append(" | ${it.name.lowercase()}") }
                        },
                    ) {
                        ZoomableCaptureImage(
                            imagePath = capture.imagePath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        )
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
                        if (capture.status == CaptureProcessingStatus.FAILED) {
                            Button(
                                onClick = { viewModel.retryCapture() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                            ) {
                                Text("Retry Analysis")
                            }
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            Text("Delete Scan")
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
                            onPrimaryAction = { viewModel.promoteCandidate(candidate.id, candidate.kind) },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(candidate.body))
                                viewModel.showFeedback("copied intel to clipboard")
                            },
                        )
                    }
                }

                items(capture.linkedMissions, key = { it.id }) { mission ->
                    var expanded by rememberSaveable(mission.id) { mutableStateOf(false) }
                    Surface(
                        color = CardSurface,
                        shape = RoundedCornerShape(26.dp),
                        border = BorderStroke(1.dp, CardStrokeStrong.copy(alpha = 0.4f)),
                        shadowElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = mission.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = if (expanded) SidequestIcons.Minus else SidequestIcons.Plus,
                                        contentDescription = if (expanded) "Collapse quest" else "Expand quest",
                                        tint = AccentSecondary,
                                    )
                                }
                            }
                            AnimatedVisibility(visible = expanded) {
                                Column {
                                    Text("Mission | ${mission.status.name.lowercase()}", color = TextSecondary)
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
            }

            AnimatedVisibility(
                visible = feedback != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                feedback?.let { message ->
                    CaptureStatusBanner(message = message)
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete scan?") },
                text = {
                    Text("The source image, OCR text, extracted items, and search index entries for this scan will be removed. Linked missions stay intact but lose the source attachment.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteCapture(onDeleted)
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@HiltViewModel
class CaptureDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    captureRepository: CaptureRepository,
    private val missionRepository: MissionRepository,
    private val processingOrchestrator: ProcessingOrchestrator,
) : ViewModel() {
    private val captureId: String = checkNotNull(savedStateHandle["captureId"])
    private val captureRepositoryRef = captureRepository
    private val _feedback = MutableStateFlow<String?>(null)

    val detail: StateFlow<CaptureDetailModel?> =
        captureRepository.observeCaptureDetail(captureId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val feedback: StateFlow<String?> = _feedback

    fun promoteCandidate(candidateId: String, kind: ExtractionKind) {
        viewModelScope.launch {
            missionRepository.promoteCandidate(candidateId)
            _feedback.value = when (kind) {
                ExtractionKind.TASK -> "mission created from source intel"
                ExtractionKind.DATE -> "reminder mission created"
                ExtractionKind.REFERENCE,
                ExtractionKind.FACT,
                -> "intel promoted"
            }
        }
    }

    fun showFeedback(message: String) {
        _feedback.value = message
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    fun deleteCapture(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (captureRepositoryRef.deleteCapture(captureId)) {
                onDeleted()
            } else {
                _feedback.value = "scan could not be deleted"
            }
        }
    }

    fun retryCapture() {
        viewModelScope.launch {
            processingOrchestrator.enqueue(captureId)
            _feedback.value = "analysis queued again"
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
        Text(
            text = candidate.reasoning,
            style = MaterialTheme.typography.bodySmall,
            color = AccentPrimary,
            modifier = Modifier.padding(top = 8.dp),
        )
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
        "camera" -> "Camera Capture"
        "import" -> "Imported Capture"
        else -> capture.sourceLabel.replaceFirstChar { it.uppercase() }
    }
}
