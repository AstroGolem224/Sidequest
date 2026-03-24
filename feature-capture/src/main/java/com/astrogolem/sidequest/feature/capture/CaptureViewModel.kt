package com.astrogolem.sidequest.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.ProcessingOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DefaultCaptureMessage = "Ready for the next capture."

data class CaptureUiState(
    val captures: List<CaptureSummary> = emptyList(),
    val isSaving: Boolean = false,
    val lastMessage: String = DefaultCaptureMessage,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val processingOrchestrator: ProcessingOrchestrator,
) : ViewModel() {
    private val isSaving = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow(DefaultCaptureMessage)

    val state: StateFlow<CaptureUiState> = combine(
        captureRepository.observeCaptures(),
        isSaving,
        lastMessage,
    ) { captures, saving, message ->
        CaptureUiState(
            captures = captures,
            isSaving = saving,
            lastMessage = if (saving) {
                "Saving capture locally..."
            } else {
                deriveCaptureMessage(captures, message)
            },
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
            processingOrchestrator.enqueue(captureId)
            lastMessage.value = "Capture queued for background extraction."
            isSaving.value = false
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
