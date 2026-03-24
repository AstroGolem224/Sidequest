package com.astrogolem.sidequest.core.data.repo

import android.net.Uri
import com.astrogolem.sidequest.core.data.model.ArchiveValidationResult
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.ProcessingResult
import com.astrogolem.sidequest.core.data.model.ProviderAvailability
import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(): Flow<List<CaptureSummary>>
    fun observeCandidates(): Flow<List<ExtractionCandidate>>
    suspend fun importCapture(uri: Uri): String
}

interface ProcessingOrchestrator {
    suspend fun enqueue(captureId: String)
    suspend fun processNextPendingCapture(): ProcessingResult
}

interface MissionRepository {
    fun observeMissions(): Flow<List<MissionCardModel>>
    suspend fun promoteCandidate(candidateId: String)
    suspend fun applyAction(action: MissionAction)
}

interface SearchRepository {
    suspend fun search(query: String): List<SearchResultModel>
}

interface ArchiveService {
    suspend fun exportSnapshot(target: Uri): Result<Unit>
    suspend fun validateImport(source: Uri): ArchiveValidationResult
}

interface SecurityService {
    suspend fun saveProviderKey(kind: ProviderKind, apiKey: String)
    suspend fun getProviderAvailability(): List<ProviderAvailability>
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun isBiometricLockEnabled(): Boolean
}
