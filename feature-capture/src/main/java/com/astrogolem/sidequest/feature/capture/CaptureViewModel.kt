package com.astrogolem.sidequest.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.CaptureDetailModel
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.model.ExtractionStatus
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.ProcessingOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal const val DefaultCaptureMessage = "Ready for the next capture."

data class CaptureUiState(
    val captures: List<CaptureSummary> = emptyList(),
    val isSaving: Boolean = false,
    val lastMessage: String = DefaultCaptureMessage,
    val reviewCapture: CaptureDetailModel? = null,
    val reviewCandidates: List<ExtractionCandidate> = emptyList(),
    val showReviewDrawer: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val missionRepository: MissionRepository,
    private val processingOrchestrator: ProcessingOrchestrator,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow(DefaultCaptureMessage)
    private val reviewCaptureId = MutableStateFlow<String?>(null)
    private val dismissedReviewCaptureId = MutableStateFlow<String?>(null)

    private val reviewCaptureDetail =
        reviewCaptureId.flatMapLatest { captureId ->
            if (captureId == null) {
                flowOf(null)
            } else {
                captureRepository.observeCaptureDetail(captureId)
            }
        }

    private data class CaptureChromeInputs(
        val captures: List<CaptureSummary>,
        val saving: Boolean,
        val message: String,
        val activeReviewId: String?,
        val dismissedId: String?,
    )

    val state: StateFlow<CaptureUiState> = combine(
        captureRepository.observeCaptures(),
        isSaving,
        lastMessage,
        reviewCaptureId,
        dismissedReviewCaptureId,
    ) { captures, saving, message, activeReviewId, dismissedId ->
        CaptureChromeInputs(
            captures = captures,
            saving = saving,
            message = message,
            activeReviewId = activeReviewId,
            dismissedId = dismissedId,
        )
    }.combine(reviewCaptureDetail) { chromeInputs, reviewDetail ->
        val reviewCandidates = reviewDetail
            ?.candidates
            ?.filter { candidate ->
                candidate.status == ExtractionStatus.CANDIDATE && candidate.kind == ExtractionKind.TASK
            }
            .orEmpty()
        CaptureUiState(
            captures = chromeInputs.captures,
            isSaving = chromeInputs.saving,
            lastMessage = if (chromeInputs.saving) {
                "Saving capture locally..."
            } else {
                deriveCaptureMessage(chromeInputs.captures, chromeInputs.message)
            },
            reviewCapture = reviewDetail,
            reviewCandidates = reviewCandidates,
            showReviewDrawer = chromeInputs.activeReviewId != null &&
                chromeInputs.activeReviewId != chromeInputs.dismissedId &&
                reviewDetail != null &&
                reviewDetail.status != CaptureProcessingStatus.PENDING,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureUiState(lastMessage = DefaultCaptureMessage))

    fun import(uri: Uri) {
        persistAndProcess(uri, sourceType = "import")
    }

    fun saveCameraCapture(uri: Uri) {
        persistAndProcess(uri, sourceType = "camera")
    }

    private fun persistAndProcess(uri: Uri, sourceType: String) {
        viewModelScope.launch {
            isSaving.value = true
            val captureId = captureRepository.saveCapture(uri, sourceType = sourceType)
            reviewCaptureId.value = captureId
            dismissedReviewCaptureId.value = null
            processingOrchestrator.enqueue(captureId)
            lastMessage.value = "Capture queued for background extraction."
            isSaving.value = false
        }
    }

    fun dismissReviewDrawer() {
        dismissedReviewCaptureId.value = reviewCaptureId.value
    }

    fun promoteCandidate(candidateId: String) {
        viewModelScope.launch {
            missionRepository.promoteCandidate(candidateId)
        }
    }

    fun dismissCandidate(candidateId: String) {
        viewModelScope.launch {
            captureRepository.dismissCandidate(candidateId)
        }
    }

    private fun deriveCaptureMessage(captures: List<CaptureSummary>, fallback: String): String {
        val latest = captures.firstOrNull() ?: return fallback
        return when (latest.status) {
            com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus.PENDING ->
                "Latest capture queued for extraction."
            com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus.PROCESSING ->
                "Background extraction is running."
            com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus.DONE ->
                "Latest capture finished processing."
            com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus.FAILED ->
                "Latest capture failed to process. Try importing or retaking it."
        }
    }
}
