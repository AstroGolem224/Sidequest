package com.astrogolem.sidequest.core.data.repo

import android.net.Uri
import com.astrogolem.sidequest.core.data.model.ArchiveValidationResult
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.CaptureDetailModel
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.MissionDetailModel
import com.astrogolem.sidequest.core.data.model.ProcessingResult
import com.astrogolem.sidequest.core.data.model.ProviderAvailability
import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(): Flow<List<CaptureSummary>>
    fun observeCandidates(): Flow<List<ExtractionCandidate>>
    fun observeCaptureDetail(captureId: String): Flow<CaptureDetailModel?>
    suspend fun saveCapture(uri: Uri, sourceType: String = "import"): String
    suspend fun importCapture(uri: Uri): String
    suspend fun dismissCandidate(candidateId: String)
    suspend fun deleteCapture(captureId: String): Boolean
}

interface ProcessingOrchestrator {
    suspend fun enqueue(captureId: String)
    suspend fun recoverPendingCaptures(limit: Int = 20): Int
    suspend fun processNextPendingCapture(): ProcessingResult
    suspend fun processCapture(captureId: String): ProcessingResult
}

interface MissionRepository {
    fun observeMissions(): Flow<List<MissionCardModel>>
    fun observeMission(missionId: String): Flow<MissionDetailModel?>
    suspend fun promoteCandidate(candidateId: String)
    suspend fun applyAction(action: MissionAction)
}

interface SearchRepository {
    suspend fun search(query: String): List<SearchResultModel>
}

interface ArchiveService {
    suspend fun exportSnapshot(target: Uri): Result<Unit>
    suspend fun validateImport(source: Uri): ArchiveValidationResult
    suspend fun importSnapshot(source: Uri): Result<Unit>
}

interface SecurityService {
    suspend fun saveProviderKey(kind: ProviderKind, apiKey: String)
    suspend fun getProviderKey(kind: ProviderKind): String?
    suspend fun getProviderAvailability(): List<ProviderAvailability>
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun isBiometricLockEnabled(): Boolean
}
