package com.astrogolem.sidequest.feature.inbox

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.CardSurfaceStrong
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

        if (state.candidates.isEmpty()) {
            item {
                ScaffoldCard(
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
                ScaffoldCard(
                    title = "No scan history yet",
                    subtitle = "Run a capture or import to start filling the intel archive.",
                ) {}
            }
        } else {
            items(state.recentCaptures, key = { it.id }) { capture ->
                RecentCaptureCard(
                    capture = capture,
                    onOpenCapture = { onOpenCapture(capture.id) },
                )
            }
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
)

data class InboxCandidateItem(
    val id: String,
    val captureId: String,
    val title: String,
    val body: String,
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
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Quest Intake", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                Text(
                    "Level $intakeLevel intake officer",
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
                    .fillMaxWidth(progress)
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
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardStroke),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
                        Text(
                            text = if (candidate.needsReview) "?" else "Q",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = AccentPrimary,
                        )
                    }
                }
                Surface(
                    color = if (candidate.needsReview) BgGlow else AccentPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = questTierLabel(candidate),
                        color = if (candidate.needsReview) TextSecondary else AccentPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
            Text(candidate.title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text(candidate.body, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(onClick = {}, label = { Text(candidate.sourceLabel) })
                AssistChip(onClick = {}, label = { Text(candidate.captureStatusLabel) })
            }
            Text(
                text = candidate.qualityLabel,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = if (candidate.needsReview) TextSecondary else AccentSecondary,
            )
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
                    Text("Intel")
                }
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
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
) {
    Surface(
        color = BgGlow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.8f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(intelLabel(capture), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(capture.status.name.lowercase(), color = TextSecondary)
            }
            OutlinedButton(onClick = onOpenCapture) {
                Text("Open Intel")
            }
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

private fun intelLabel(capture: CaptureSummary): String {
    return when (capture.sourceLabel.lowercase()) {
        "camera" -> "Field Scan"
        "import" -> "Recovered Intel"
        else -> capture.sourceLabel.replaceFirstChar { it.uppercase() }
    }
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val missionRepository: MissionRepository,
) : ViewModel() {
    val state: StateFlow<InboxUiState> = combine(
        captureRepository.observeCandidates(),
        captureRepository.observeCaptures(),
    ) { candidates, captures ->
        val statusByCapture = captures.associateBy { it.id }
        InboxUiState(
            candidates = candidates.map { candidate ->
                InboxCandidateItem(
                    id = candidate.id,
                    captureId = candidate.captureId,
                    title = candidate.title,
                    body = candidate.body,
                    confidence = candidate.confidence,
                    sourceLabel = statusByCapture[candidate.captureId]?.sourceLabel?.replaceFirstChar { it.uppercase() } ?: "Capture",
                    captureStatusLabel = statusLabel(statusByCapture[candidate.captureId]?.status),
                    needsReview = candidate.confidence < 0.55f,
                    qualityLabel = qualityLabel(candidate.confidence),
                    kind = candidate.kind,
                )
            }.sortedByDescending { it.confidence },
            queuedCount = captures.count { it.status == CaptureProcessingStatus.PENDING },
            processingCount = captures.count { it.status == CaptureProcessingStatus.PROCESSING },
            doneCount = captures.count { it.status == CaptureProcessingStatus.DONE },
            failedCount = captures.count { it.status == CaptureProcessingStatus.FAILED },
            reviewCount = candidates.count { it.confidence < 0.55f },
            bestBetCount = candidates.count { it.confidence >= 0.55f },
            recentCaptures = captures.take(5),
            emptyStateMessage = deriveEmptyMessage(captures),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    fun promote(candidateId: String) {
        viewModelScope.launch {
            missionRepository.promoteCandidate(candidateId)
        }
    }

    fun dismiss(candidateId: String) {
        viewModelScope.launch {
            captureRepository.dismissCandidate(candidateId)
        }
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
        confidence >= 0.55f -> "Review quickly"
        else -> "Noisy OCR"
    }
}
