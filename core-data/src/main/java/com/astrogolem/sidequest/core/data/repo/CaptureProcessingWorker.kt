package com.astrogolem.sidequest.core.data.repo

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CaptureProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processingOrchestrator: ProcessingOrchestrator,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val captureId = inputData.getString(CAPTURE_ID_KEY) ?: return Result.failure()
        return when (processingOrchestrator.processCapture(captureId)) {
            is com.astrogolem.sidequest.core.data.model.ProcessingResult.Success -> Result.success()
            is com.astrogolem.sidequest.core.data.model.ProcessingResult.Deferred -> Result.retry()
            is com.astrogolem.sidequest.core.data.model.ProcessingResult.Failure ->
                if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val CAPTURE_ID_KEY = "capture_id"
    }
}
