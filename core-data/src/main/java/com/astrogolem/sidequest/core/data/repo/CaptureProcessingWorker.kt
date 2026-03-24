package com.astrogolem.sidequest.core.data.repo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class CaptureProcessingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    private val processingOrchestrator: ProcessingOrchestrator by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            CaptureProcessingWorkerEntryPoint::class.java,
        ).processingOrchestrator()
    }

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

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CaptureProcessingWorkerEntryPoint {
    fun processingOrchestrator(): ProcessingOrchestrator
}
