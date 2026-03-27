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
import com.astrogolem.sidequest.core.data.model.UserPreferences
import com.astrogolem.sidequest.core.data.model.NoteDetail
import com.astrogolem.sidequest.core.data.model.NoteSummary
import com.astrogolem.sidequest.core.data.model.SearchFilter
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import com.astrogolem.sidequest.core.data.model.RoutinePlanDetail
import com.astrogolem.sidequest.core.data.model.RoutinePlanSummary
import com.astrogolem.sidequest.core.data.model.ShoppingListDetailModel
import com.astrogolem.sidequest.core.data.model.ShoppingListSummary
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
    suspend fun createQuestFromShoppingList(listId: String): String?
    suspend fun createQuestFromRoutinePlan(planId: String): String?
    suspend fun createQuestFromNote(noteId: String): String?
    suspend fun applyAction(action: MissionAction)
}

interface SearchRepository {
    suspend fun search(query: String, filter: SearchFilter = SearchFilter.ALL): List<SearchResultModel>
}

interface ShoppingListRepository {
    fun observeLists(): Flow<List<ShoppingListSummary>>
    fun observeList(listId: String): Flow<ShoppingListDetailModel?>
    suspend fun createManualList(title: String, rawInput: String): Result<String>
    suspend fun createVoiceList(rawInput: String): Result<String>
    suspend fun createPhotoList(imageUri: Uri): Result<String>
    suspend fun toggleItem(itemId: String, checked: Boolean)
    suspend fun renameList(listId: String, title: String)
    suspend fun setListArchived(listId: String, archived: Boolean)
    suspend fun deleteList(listId: String)
}

interface RoutinePlanRepository {
    fun observePlans(): Flow<List<RoutinePlanSummary>>
    fun observePlan(planId: String): Flow<RoutinePlanDetail?>
    suspend fun upsertPlan(detail: RoutinePlanDetail)
    suspend fun completePlan(planId: String)
    suspend fun deletePlan(planId: String)
}

interface NotesRepository {
    fun observeNotes(): Flow<List<NoteSummary>>
    fun observeNote(noteId: String): Flow<NoteDetail?>
    suspend fun createNote(title: String, markdown: String = ""): String
    suspend fun importMarkdown(uri: Uri): Result<String>
    suspend fun saveNote(detail: NoteDetail)
    suspend fun deleteNote(noteId: String)
}

interface ArchiveService {
    suspend fun exportSnapshot(target: Uri): Result<Unit>
    suspend fun validateImport(source: Uri): ArchiveValidationResult
    suspend fun importSnapshot(source: Uri): Result<Unit>
}

interface SecurityService {
    fun observeUserPreferences(): Flow<UserPreferences>
    suspend fun saveProviderKey(kind: ProviderKind, apiKey: String)
    suspend fun getProviderKey(kind: ProviderKind): String?
    suspend fun getProviderAvailability(): List<ProviderAvailability>
    suspend fun getActiveProviderKind(): ProviderKind?
    suspend fun setActiveProviderKind(kind: ProviderKind?)
    suspend fun setThemePreset(themePresetName: String)
    suspend fun setAiFirstCaptureEnabled(enabled: Boolean)
    suspend fun isAiFirstCaptureEnabled(): Boolean
    suspend fun saveAvatarImage(uri: Uri): String
    suspend fun clearAvatarImage()
    suspend fun setBiometricLockEnabled(enabled: Boolean)
    suspend fun isBiometricLockEnabled(): Boolean
    suspend fun setNotesSaveFolderUri(uri: String?)
}
