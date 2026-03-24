package com.astrogolem.sidequest.core.data.repo

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.astrogolem.sidequest.core.data.local.CaptureAnalysisEntity
import com.astrogolem.sidequest.core.data.local.CaptureDao
import com.astrogolem.sidequest.core.data.local.CaptureEntity
import com.astrogolem.sidequest.core.data.local.ExportBundleDao
import com.astrogolem.sidequest.core.data.local.ExportBundleEntity
import com.astrogolem.sidequest.core.data.local.ExtractedItemEntity
import com.astrogolem.sidequest.core.data.local.KnowledgeNodeEntity
import com.astrogolem.sidequest.core.data.local.MissionDao
import com.astrogolem.sidequest.core.data.local.MissionEntity
import com.astrogolem.sidequest.core.data.local.SearchDao
import com.astrogolem.sidequest.core.data.model.ArchiveValidationResult
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.DocumentType
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.model.ExtractionStatus
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.MissionStatus
import com.astrogolem.sidequest.core.data.model.ProcessingResult
import com.astrogolem.sidequest.core.data.model.ProviderAvailability
import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultCaptureRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val captureDao: CaptureDao,
) : CaptureRepository {
    override fun observeCaptures(): Flow<List<CaptureSummary>> {
        return captureDao.observeCaptures().map { captures ->
            captures.map {
                CaptureSummary(
                    id = it.id,
                    createdAt = it.createdAt,
                    sourceLabel = it.sourceType,
                    previewPath = it.thumbnailPath,
                    status = it.processingStatus,
                )
            }
        }
    }

    override fun observeCandidates(): Flow<List<ExtractionCandidate>> {
        return captureDao.observeExtractedItems(ExtractionStatus.CANDIDATE.name).map { items ->
            items.map {
                ExtractionCandidate(
                    id = it.id,
                    captureId = it.captureId,
                    title = it.title,
                    body = it.body,
                    confidence = it.confidence,
                    dueAt = it.dueAt,
                    status = it.status,
                )
            }
        }
    }

    override suspend fun importCapture(uri: Uri): String {
        val id = UUID.randomUUID().toString()
        val imageDir = File(context.filesDir, "captures").apply { mkdirs() }
        val target = File(imageDir, "$id.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open image")
        captureDao.upsertCapture(
            CaptureEntity(
                id = id,
                createdAt = System.currentTimeMillis(),
                sourceType = "import",
                filePath = target.absolutePath,
                thumbnailPath = target.absolutePath,
                mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
                processingStatus = CaptureProcessingStatus.PENDING,
            ),
        )
        return id
    }
}

@Singleton
class DefaultProcessingOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val captureDao: CaptureDao,
    private val searchDao: SearchDao,
) : ProcessingOrchestrator {
    override suspend fun enqueue(captureId: String) {
        captureDao.updateStatus(captureId, CaptureProcessingStatus.PENDING.name)
    }

    override suspend fun processNextPendingCapture(): ProcessingResult {
        val capture = captureDao.nextPendingCapture() ?: return ProcessingResult.Deferred("", "No pending captures")
        captureDao.updateStatus(capture.id, CaptureProcessingStatus.PROCESSING.name)
        return try {
            val image = InputImage.fromFilePath(context, File(capture.filePath).toUri())
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()
            val lines = result.textBlocks.mapNotNull { it.text.trim().takeIf(String::isNotBlank) }
            val analysis = CaptureAnalysisEntity(
                id = UUID.randomUUID().toString(),
                captureId = capture.id,
                documentType = classify(lines.joinToString("\n")),
                ocrText = result.text,
                summary = lines.firstOrNull().orEmpty(),
                processedAt = System.currentTimeMillis(),
                errorCode = null,
            )
            val candidates = lines.take(5).map { line ->
                ExtractedItemEntity(
                    id = UUID.randomUUID().toString(),
                    captureId = capture.id,
                    title = line.take(64),
                    body = line,
                    confidence = 0.6f,
                    dueAt = null,
                    status = ExtractionStatus.CANDIDATE,
                )
            }
            captureDao.upsertAnalysis(analysis)
            if (candidates.isNotEmpty()) {
                captureDao.upsertExtractedItems(candidates)
            }
            searchDao.upsertNode(
                KnowledgeNodeEntity(
                    id = UUID.randomUUID().toString(),
                    captureId = capture.id,
                    title = analysis.summary.ifBlank { "Capture ${capture.id.take(6)}" },
                    body = analysis.ocrText,
                ),
            )
            captureDao.updateStatus(capture.id, CaptureProcessingStatus.DONE.name)
            ProcessingResult.Success(capture.id, candidates.size)
        } catch (error: Throwable) {
            captureDao.updateStatus(capture.id, CaptureProcessingStatus.FAILED.name)
            ProcessingResult.Failure(capture.id, error.message ?: "Processing failed")
        }
    }

    private fun classify(text: String): DocumentType {
        val lowered = text.lowercase()
        return when {
            lowered.isBlank() -> DocumentType.SCENE
            "total" in lowered || "receipt" in lowered -> DocumentType.RECEIPT
            "dear" in lowered || "regards" in lowered -> DocumentType.LETTER
            "todo" in lowered || "task" in lowered || "checklist" in lowered -> DocumentType.NOTE
            else -> DocumentType.UNKNOWN
        }
    }
}

@Singleton
class DefaultMissionRepository @Inject constructor(
    private val captureDao: CaptureDao,
    private val missionDao: MissionDao,
) : MissionRepository {
    override fun observeMissions(): Flow<List<MissionCardModel>> {
        return missionDao.observeMissions().map { missions ->
            missions.map {
                MissionCardModel(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    priorityScore = it.priorityScore,
                    status = it.status,
                    dueAt = it.dueAt,
                    sourceCaptureId = it.sourceCaptureId,
                )
            }
        }
    }

    override suspend fun promoteCandidate(candidateId: String) {
        val candidate = captureDao.getExtractedItem(candidateId) ?: return
        missionDao.upsertMission(
            MissionEntity(
                id = UUID.randomUUID().toString(),
                title = candidate.title,
                description = candidate.body,
                priorityScore = priorityFor(candidate),
                status = MissionStatus.OPEN,
                dueAt = candidate.dueAt,
                sourceCaptureId = candidate.captureId,
                createdAt = System.currentTimeMillis(),
            ),
        )
        captureDao.updateExtractedStatus(candidateId, ExtractionStatus.PROMOTED.name)
    }

    override suspend fun applyAction(action: MissionAction) {
        when (action) {
            is MissionAction.Archive -> missionDao.updateMissionStatus(action.missionId, MissionStatus.ARCHIVED.name)
            is MissionAction.Complete -> missionDao.updateMissionStatus(action.missionId, MissionStatus.DONE.name)
            is MissionAction.Pin -> missionDao.updateMissionStatus(action.missionId, MissionStatus.ACTIVE.name)
            is MissionAction.Snooze -> missionDao.updateMissionStatus(action.missionId, MissionStatus.SNOOZED.name)
        }
    }

    private fun priorityFor(candidate: ExtractedItemEntity): Int {
        return when {
            candidate.body.contains("today", ignoreCase = true) -> 95
            candidate.body.contains("urgent", ignoreCase = true) -> 90
            else -> (candidate.confidence * 100).toInt()
        }
    }
}

@Singleton
class DefaultSearchRepository @Inject constructor(
    private val searchDao: SearchDao,
) : SearchRepository {
    override suspend fun search(query: String): List<SearchResultModel> {
        return searchDao.search(query).map {
            SearchResultModel(
                id = it.id,
                captureId = it.captureId,
                title = it.title,
                snippet = it.body.take(160),
            )
        }
    }
}

@Singleton
class DefaultArchiveService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportBundleDao: ExportBundleDao,
) : ArchiveService {
    override suspend fun exportSnapshot(target: Uri): Result<Unit> {
        return runCatching {
            context.contentResolver.openOutputStream(target)?.use { stream ->
                stream.write("""{"version":1,"exportedAt":${System.currentTimeMillis()}}""".toByteArray())
            } ?: error("Unable to open export target")
            exportBundleDao.upsertBundle(
                ExportBundleEntity(
                    id = UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    fileUri = target.toString(),
                    checksum = "pending",
                    itemCount = 0,
                ),
            )
        }
    }

    override suspend fun validateImport(source: Uri): ArchiveValidationResult {
        val body = context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readText() }.orEmpty()
        return if ("\"version\":1" in body) ArchiveValidationResult.Valid else ArchiveValidationResult.Invalid("Unsupported archive")
    }
}

@Singleton
class DefaultSecurityService @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecurityService {
    override suspend fun saveProviderKey(kind: ProviderKind, apiKey: String) {
        val prefs = securePrefs()
        prefs.edit().putString(kind.name, apiKey).apply()
    }

    override suspend fun getProviderAvailability(): List<ProviderAvailability> {
        val prefs = securePrefs()
        return ProviderKind.entries.map { kind ->
            val configured = !prefs.getString(kind.name, null).isNullOrBlank()
            ProviderAvailability(kind = kind, configured = configured, enabled = configured)
        }
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        securePrefs().edit().putBoolean("biometric_lock_enabled", enabled).apply()
    }

    override suspend fun isBiometricLockEnabled(): Boolean {
        return securePrefs().getBoolean("biometric_lock_enabled", false)
    }

    private fun securePrefs() = EncryptedSharedPreferences.create(
        context,
        "sidequest_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
