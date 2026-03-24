package com.astrogolem.sidequest.feature.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
fun InboxRoute(viewModel: InboxViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(
                title = "Inbox Pipeline",
                subtitle = "Candidates appear here when a background capture job finishes extraction.",
            ) {
                Text("Queued: ${state.queuedCount}")
                Text("Processing: ${state.processingCount}")
                Text("Done: ${state.doneCount}")
                Text("Failed: ${state.failedCount}")
                Text("Needs review: ${state.reviewCount}")
            }
        }

        if (state.candidates.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "No candidates yet",
                    subtitle = state.emptyStateMessage,
                ) {
                    state.recentCaptures.take(3).forEach { capture ->
                        Text("${capture.sourceLabel}: ${capture.status.name.lowercase()}")
                    }
                }
            }
        } else {
            items(state.candidates, key = { it.id }) { candidate ->
                ScaffoldCard(
                    title = candidate.title,
                    subtitle = "Confidence ${(candidate.confidence * 100).toInt()}% | ${candidate.captureStatusLabel}",
                ) {
                    if (candidate.needsReview) {
                        Text("Low confidence extraction. Review before promoting.")
                    }
                    Text(candidate.body, modifier = Modifier.padding(top = 8.dp))
                    Button(
                        onClick = { viewModel.promote(candidate.id) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text("Promote to Mission")
                    }
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
    val recentCaptures: List<CaptureSummary> = emptyList(),
    val emptyStateMessage: String = "Capture something to start the extraction pipeline.",
)

data class InboxCandidateItem(
    val id: String,
    val title: String,
    val body: String,
    val confidence: Float,
    val captureStatusLabel: String,
    val needsReview: Boolean,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    captureRepository: CaptureRepository,
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
                    title = candidate.title,
                    body = candidate.body,
                    confidence = candidate.confidence,
                    captureStatusLabel = statusLabel(statusByCapture[candidate.captureId]?.status),
                    needsReview = candidate.confidence < 0.5f,
                )
            },
            queuedCount = captures.count { it.status == CaptureProcessingStatus.PENDING },
            processingCount = captures.count { it.status == CaptureProcessingStatus.PROCESSING },
            doneCount = captures.count { it.status == CaptureProcessingStatus.DONE },
            failedCount = captures.count { it.status == CaptureProcessingStatus.FAILED },
            reviewCount = candidates.count { it.confidence < 0.5f },
            recentCaptures = captures.take(5),
            emptyStateMessage = deriveEmptyMessage(captures),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxUiState())

    fun promote(candidateId: String) {
        viewModelScope.launch {
            missionRepository.promoteCandidate(candidateId)
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
}
