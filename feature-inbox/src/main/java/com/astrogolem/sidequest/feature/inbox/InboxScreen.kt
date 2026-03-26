package com.astrogolem.sidequest.feature.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(
                title = "Quest Intake",
                subtitle = "Fresh candidates stage here after a scan so you can approve or reject them before deployment.",
            ) {
                Text("Queued: ${state.queuedCount}")
                Text("Processing: ${state.processingCount}")
                Text("Done: ${state.doneCount}")
                Text("Failed: ${state.failedCount}")
                Text("Needs review: ${state.reviewCount}")
                Text("Best bets: ${state.bestBetCount}")
            }
        }

        if (state.candidates.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "No staged quests",
                    subtitle = state.emptyStateMessage,
                ) {
                    state.recentCaptures.take(3).forEach { capture ->
                        Text("${capture.sourceLabel}: ${capture.status.name.lowercase()}")
                    }
                }
            }
        } else {
            val bestBets = state.candidates.filterNot { it.needsReview }
            val reviewQueue = state.candidates.filter { it.needsReview }

            if (bestBets.isNotEmpty()) {
                item {
                    ScaffoldCard(
                        title = "Ready To Deploy",
                        subtitle = "Higher-signal candidates that already look like real quests.",
                    ) {}
                }
                items(bestBets, key = { it.id }) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onPromote = { viewModel.promote(candidate.id) },
                        onDismiss = { viewModel.dismiss(candidate.id) },
                        onOpenCapture = { onOpenCapture(candidate.captureId) },
                    )
                }
            }

            if (reviewQueue.isNotEmpty()) {
                item {
                    ScaffoldCard(
                        title = "Needs Review",
                        subtitle = "Low-signal OCR lines. Only keep the candidates that survive a human glance.",
                    ) {}
                }
                items(reviewQueue, key = { it.id }) { candidate ->
                    CandidateCard(
                        candidate = candidate,
                        onPromote = { viewModel.promote(candidate.id) },
                        onDismiss = { viewModel.dismiss(candidate.id) },
                        onOpenCapture = { onOpenCapture(candidate.captureId) },
                    )
                }
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
private fun CandidateCard(
    candidate: InboxCandidateItem,
    onPromote: () -> Unit,
    onDismiss: () -> Unit,
    onOpenCapture: () -> Unit,
) {
    ScaffoldCard(
        title = candidate.title,
        subtitle = "${candidate.qualityLabel} | ${(candidate.confidence * 100).toInt()}% confidence",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = {}, label = { Text(candidate.sourceLabel) })
            AssistChip(onClick = {}, label = { Text(candidate.captureStatusLabel) })
            AssistChip(onClick = {}, label = { Text(candidate.kind.name.lowercase()) })
        }
        if (candidate.needsReview) {
            Text(
                "This extraction looks noisy. Dismiss it unless it clearly belongs in your mission queue.",
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Text(candidate.body, modifier = Modifier.padding(top = 12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onPromote, modifier = Modifier.weight(1f)) {
                Text("Promote")
            }
            OutlinedButton(onClick = onOpenCapture, modifier = Modifier.weight(1f)) {
                Text("Open Intel")
            }
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Dismiss")
        }
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
