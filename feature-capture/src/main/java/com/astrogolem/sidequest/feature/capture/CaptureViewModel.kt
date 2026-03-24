package com.astrogolem.sidequest.feature.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.ProcessingResult
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

data class CaptureUiState(
    val captures: List<CaptureSummary> = emptyList(),
    val isImporting: Boolean = false,
    val lastMessage: String = "Ready for the next capture.",
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val processingOrchestrator: ProcessingOrchestrator,
) : ViewModel() {
    private val isImporting = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow("Ready for the next capture.")

    val state: StateFlow<CaptureUiState> = combine(
        captureRepository.observeCaptures(),
        isImporting,
        lastMessage,
    ) { captures, importing, message ->
        CaptureUiState(captures = captures, isImporting = importing, lastMessage = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureUiState())

    fun import(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            val captureId = captureRepository.importCapture(uri)
            processingOrchestrator.enqueue(captureId)
            lastMessage.value = when (val result = processingOrchestrator.processNextPendingCapture()) {
                is ProcessingResult.Success -> "Extracted ${result.candidateCount} candidates."
                is ProcessingResult.Deferred -> result.reason
                is ProcessingResult.Failure -> result.reason
            }
            isImporting.value = false
        }
    }
}
