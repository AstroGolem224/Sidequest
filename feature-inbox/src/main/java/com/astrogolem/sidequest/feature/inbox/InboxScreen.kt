package com.astrogolem.sidequest.feature.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.ProcessingOrchestrator
import com.astrogolem.sidequest.core.ui.components.GlassCard
import com.astrogolem.sidequest.core.ui.components.StatusPill
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgPanel
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.CardSurfaceStrong
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun InboxRoute(
    onOpenCapture: (String) -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val totalCandidates = state.bestBetCount + state.reviewCount
    val bestBets = state.candidates.filterNot { it.needsReview }
    val reviewQueue = state.candidates.filter { it.needsReview }
    var pendingDeleteCaptureId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    LaunchedEffect(state.feedbackMessage) {
        if (state.feedbackMessage != null) {
            delay(1800)
            viewModel.clearFeedback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                QuestIntakeHero(state = state, totalCandidates = totalCandidates)
            }

            if (bestBets.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Ready To Deploy",
                        badge = "${bestBets.size} ready",
                    )
                }
                items(bestBets, key = { it.id }) { candidate ->
                    QuestCandidateCard(
                        candidate = candidate,
                        onPromote = { viewModel.promote(candidate.id) },
                        onDismiss = { viewModel.dismiss(candidate.id) },
                        onOpenCapture = { onOpenCapture(candidate.captureId) },
                    )
                }
            }

            if (reviewQueue.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Needs Review",
                        badge = "${reviewQueue.size} flagged",
                    )
                }
                items(reviewQueue, key = { it.id }) { candidate ->
                    QuestCandidateCard(
                        candidate = candidate,
                        onPromote = { viewModel.promote(candidate.id) },
                        onDismiss = { viewModel.dismiss(candidate.id) },
                        onOpenCapture = { onOpenCapture(candidate.captureId) },
                    )
                }
            }

            item {
                if (state.candidates.isEmpty()) {
                    GlassCard(
                        title = "No staged quests",
                        subtitle = state.emptyStateMessage,
                    ) {
                        Text(
                            text = "Completed scans can still hold facts, dates, and references inside intel. This queue only surfaces task-like candidates.",
                            color = TextSecondary,
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Recent Scan Activity",
                    badge = "${state.recentCaptures.size} scans",
                )
            }

            if (state.recentCaptures.isEmpty()) {
                item {
                    GlassCard(
                        title = "No scan history yet",
                        subtitle = "Run a capture or import to start filling the intel archive.",
                    ) {}
                }
            } else {
                items(state.recentCaptures, key = { it.id }) { capture ->
                    RecentCaptureCard(
                        capture = capture,
                        onOpenCapture = { onOpenCapture(capture.id) },
                        onRetry = { viewModel.retryCapture(capture.id) },
                        onDelete = { pendingDeleteCaptureId = capture.id },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.feedbackMessage != null,
            enter = fadeIn(animationSpec = tween(160)) + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut(animationSpec = tween(140)) + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .padding(top = 8.dp),
        ) {
            state.feedbackMessage?.let { message ->
                ActionFeedbackBanner(message)
            }
        }

        if (pendingDeleteCaptureId != null) {
            AlertDialog(
                onDismissRequest = { pendingDeleteCaptureId = null },
                title = { Text("Delete scan?") },
                text = {
                    Text("This removes the scan, OCR, extracted items, and search entries. Missions keep running but lose their source attachment.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val captureId = pendingDeleteCaptureId
                            pendingDeleteCaptureId = null
                            if (captureId != null) viewModel.deleteCapture(captureId)
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteCaptureId = null }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

data class InboxUiState(
    val candidates: List<InboxCandidateItem> = emptyList(),
    val queuedCount: Int = 0,
    val processingCount: Int = 0,
    val doneCount: Int = 0,
    val failedCount: Int = 0,
    val reviewCount: Int = 0,
    val bestBetCount: Int = 0,
    val recentCaptures: List<CaptureSummary> = emptyList(),
    val emptyStateMessage: String = "Capture something to start the extraction pipeline.",
    val feedbackMessage: String? = null,
)

data class InboxCandidateItem(
    val id: String,
    val captureId: String,
    val title: String,
    val body: String,
    val reasoning: String,
    val confidence: Float,
    val sourceLabel: String,
    val captureStatusLabel: String,
    val needsReview: Boolean,
    val qualityLabel: String,
    val kind: ExtractionKind,
)

@Composable
private fun QuestIntakeHero(
    state: InboxUiState,
    totalCandidates: Int,
) {
    val intakeLevel = max(1, state.doneCount / 3 + state.bestBetCount + 1)
    val progress = if (totalCandidates == 0) 0.16f else (state.bestBetCount / totalCandidates.toFloat()).coerceIn(0.12f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 420),
        label = "quest-intake-progress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Quest Log", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                Text(
                    "Level $intakeLevel review flow",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = AccentPrimary,
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("${state.bestBetCount} deployable", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = AccentSecondary)
                Text("${state.reviewCount} need human veto", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(CardSurfaceStrong),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AccentPrimary),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroMetric(label = "Ready", value = state.bestBetCount.toString(), modifier = Modifier.weight(1f))
            HeroMetric(label = "Review", value = state.reviewCount.toString(), modifier = Modifier.weight(1f))
            HeroMetric(label = "Scans", value = state.doneCount.toString(), modifier = Modifier.weight(1f))
        }
        if (state.processingCount > 0 || state.queuedCount > 0) {
            Surface(
                color = BgGlow,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.8f)),
            ) {
                Text(
                    text = if (state.processingCount > 0) {
                        "${state.processingCount} scans are still resolving in the background."
                    } else {
                        "${state.queuedCount} scans are queued for extraction."
                    },
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ActionFeedbackBanner(message: String) {
    Surface(
        color = BgPanel.copy(alpha = 0.96f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.75f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SidequestIcons.Spark,
                contentDescription = null,
                tint = AccentPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(message, color = AccentSecondary, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = BgGlow,
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label.uppercase(), style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = TextSecondary)
            Text(value, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = AccentSecondary)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badge: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Surface(
            color = AccentPrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = badge.uppercase(),
                color = AccentSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun QuestCandidateCard(
    candidate: InboxCandidateItem,
    onPromote: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCapture: () -> Unit,
) {
    var expanded by rememberSaveable(candidate.id) { androidx.compose.runtime.mutableStateOf(false) }
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.72f)),
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
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.Top,
                    ) {
                        Surface(
                            color = AccentPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Icon(
                                    imageVector = if (candidate.needsReview) SidequestIcons.Intel else SidequestIcons.QuestLog,
                                    contentDescription = candidate.title,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                text = questTierLabel(candidate),
                                tone = if (candidate.needsReview) HudTone.Neutral else HudTone.Amber,
                            )
                            Text(
                                text = candidate.qualityLabel.uppercase(),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Text(candidate.body, color = TextSecondary)
                    Text(
                        text = candidate.reasoning,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AccentPrimary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(onClick = {}, label = { Text(candidate.sourceLabel) })
                        AssistChip(onClick = {}, label = { Text(candidate.captureStatusLabel) })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        RewardLine(label = "${rewardXp(candidate)} XP")
                        RewardLine(label = "${rewardGp(candidate)} GP")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(onClick = onPromote, modifier = Modifier.weight(1f)) {
                            Text("Deploy")
                        }
                        OutlinedButton(onClick = onOpenCapture, modifier = Modifier.weight(1f)) {
                            Text("Open Details")
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardLine(label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AccentPrimary),
        )
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = AccentSecondary)
    }
}

@Composable
private fun RecentCaptureCard(
    capture: CaptureSummary,
    onOpenCapture: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(
        title = capture.displayTitle,
        subtitle = capture.status.name.lowercase(),
        modifier = Modifier.clickable(onClick = onOpenCapture),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Icon(
                        imageVector = SidequestIcons.Intel,
                        contentDescription = capture.displayTitle,
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                if (capture.status == CaptureProcessingStatus.FAILED) {
                    CaptureActionIcon(
                        icon = SidequestIcons.Retake,
                        contentDescription = "Retry scan",
                        onClick = onRetry,
                    )
                }
                CaptureActionIcon(
                    icon = SidequestIcons.Delete,
                    contentDescription = "Delete scan",
                    onClick = onDelete,
                )
                CaptureActionIcon(
                    icon = SidequestIcons.Intel,
                    contentDescription = "Open scan details",
                    onClick = onOpenCapture,
                )
            }
        }
    }
}

@Composable
private fun CaptureActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.85f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = AccentSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun questTierLabel(candidate: InboxCandidateItem): String {
    return when {
        candidate.confidence >= 0.85f -> "HARD"
        candidate.confidence >= 0.7f -> "TIER II"
        candidate.needsReview -> "REVIEW"
        else -> "TIER I"
    }
}

private fun rewardXp(candidate: InboxCandidateItem): Int {
    return ((candidate.confidence * 240).toInt()).coerceAtLeast(40)
}

private fun rewardGp(candidate: InboxCandidateItem): Int {
    return (rewardXp(candidate) / 4).coerceAtLeast(10)
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val missionRepository: MissionRepository,
    private val processingOrchestrator: ProcessingOrchestrator,
) : ViewModel() {
    private val feedbackMessage = MutableStateFlow<String?>(null)

    val state: StateFlow<InboxUiState> = combine(
        captureRepository.observeCandidates(),
        captureRepository.observeCaptures(),
        feedbackMessage,
    ) { candidates, captures, feedback ->
        val statusByCapture = captures.associateBy { it.id }
        InboxUiState(
            candidates = candidates.map { candidate ->
                InboxCandidateItem(
                    id = candidate.id,
                    captureId = candidate.captureId,
                    title = candidate.title,
                    body = candidate.body,
                    reasoning = candidate.reasoning,
                    confidence = candidate.confidence,
                    sourceLabel = statusByCapture[candidate.captureId]?.sourceLabel?.replaceFirstChar { it.uppercase() } ?: "Capture",
                    captureStatusLabel = statusLabel(statusByCapture[candidate.captureId]?.status),
                    needsReview = candidate.confidence < 0.64f,
                    qualityLabel = qualityLabel(candidate.confidence),
                    kind = candidate.kind,
                )
            }.sortedByDescending { it.confidence },
            queuedCount = captures.count { it.status == CaptureProcessingStatus.PENDING },
            processingCount = captures.count { it.status == CaptureProcessingStatus.PROCESSING },
            doneCount = captures.count { it.status == CaptureProcessingStatus.DONE },
            failedCount = captures.count { it.status == CaptureProcessingStatus.FAILED },
            reviewCount = candidates.count { it.confidence < 0.64f },
            bestBetCount = candidates.count { it.confidence >= 0.64f },
            recentCaptures = captures.take(5),
            emptyStateMessage = deriveEmptyMessage(captures),
            feedbackMessage = feedback,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    fun promote(candidateId: String) {
        val candidateTitle = state.value.candidates.firstOrNull { it.id == candidateId }?.title ?: "quest"
        viewModelScope.launch {
            missionRepository.promoteCandidate(candidateId)
            feedbackMessage.value = "deployed: $candidateTitle"
        }
    }

    fun dismiss(candidateId: String) {
        val candidateTitle = state.value.candidates.firstOrNull { it.id == candidateId }?.title ?: "candidate"
        viewModelScope.launch {
            captureRepository.dismissCandidate(candidateId)
            feedbackMessage.value = "dismissed: $candidateTitle"
        }
    }

    fun retryCapture(captureId: String) {
        viewModelScope.launch {
            processingOrchestrator.enqueue(captureId)
            feedbackMessage.value = "requeued scan"
        }
    }

    fun deleteCapture(captureId: String) {
        viewModelScope.launch {
            if (captureRepository.deleteCapture(captureId)) {
                feedbackMessage.value = "scan deleted"
            } else {
                feedbackMessage.value = "scan could not be deleted"
            }
        }
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    private fun statusLabel(status: CaptureProcessingStatus?): String = when (status) {
        CaptureProcessingStatus.PENDING -> "queued"
        CaptureProcessingStatus.PROCESSING -> "processing"
        CaptureProcessingStatus.DONE -> "ready"
        CaptureProcessingStatus.FAILED -> "failed"
        null -> "unknown"
    }

    private fun deriveEmptyMessage(captures: List<CaptureSummary>): String {
        return when {
            captures.any { it.status == CaptureProcessingStatus.PROCESSING } ->
                "A capture is still processing in the background."
            captures.any { it.status == CaptureProcessingStatus.PENDING } ->
                "A capture is queued and waiting for background extraction."
            captures.any { it.status == CaptureProcessingStatus.FAILED } ->
                "At least one capture failed after retries. Retake or re-import the source image."
            captures.any { it.status == CaptureProcessingStatus.DONE } ->
                "Completed captures did not yield candidate tasks."
            else ->
                "Capture something to start the extraction pipeline."
        }
    }

    private fun qualityLabel(confidence: Float): String = when {
        confidence >= 0.85f -> "Strong signal"
        confidence >= 0.7f -> "Good candidate"
        confidence >= 0.64f -> "Review quickly"
        else -> "Noisy OCR"
    }
}
