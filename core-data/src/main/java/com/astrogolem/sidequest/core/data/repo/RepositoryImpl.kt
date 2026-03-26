package com.astrogolem.sidequest.core.data.repo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.work.CoroutineWorker
import com.astrogolem.sidequest.core.data.local.CaptureAnalysisEntity
import com.astrogolem.sidequest.core.data.local.CaptureDao
import com.astrogolem.sidequest.core.data.local.CaptureEntity
import com.astrogolem.sidequest.core.data.local.ExportBundleDao
import com.astrogolem.sidequest.core.data.local.ExportBundleEntity
import com.astrogolem.sidequest.core.data.local.ExtractedItemEntity
import com.astrogolem.sidequest.core.data.local.KnowledgeNodeEntity
import com.astrogolem.sidequest.core.data.local.MissionDao
import com.astrogolem.sidequest.core.data.local.MissionEntity
import com.astrogolem.sidequest.core.data.local.ReminderDao
import com.astrogolem.sidequest.core.data.local.ReminderEntity
import com.astrogolem.sidequest.core.data.local.SearchDao
import com.astrogolem.sidequest.core.data.model.ArchiveValidationResult
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.CaptureDetailModel
import com.astrogolem.sidequest.core.data.model.CaptureMissionLink
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.DocumentType
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.model.ExtractionStatus
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.MissionDetailModel
import com.astrogolem.sidequest.core.data.model.MissionStatus
import com.astrogolem.sidequest.core.data.model.ProcessingResult
import com.astrogolem.sidequest.core.data.model.ProviderAvailability
import com.astrogolem.sidequest.core.data.model.ProviderKind
import com.astrogolem.sidequest.core.data.model.ReminderState
import com.astrogolem.sidequest.core.data.model.SearchResultModel
import com.astrogolem.sidequest.core.data.provider.OpenAiExtractionProvider
import com.astrogolem.sidequest.core.data.provider.ProviderExtractionRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.UUID
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DefaultCaptureRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val captureDao: CaptureDao,
    private val missionDao: MissionDao,
    private val searchDao: SearchDao,
    private val workManager: WorkManager,
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
                it.toExtractionCandidate()
            }.filter { it.kind == ExtractionKind.TASK }
        }
    }

    override fun observeCaptureDetail(captureId: String): Flow<CaptureDetailModel?> {
        return combine(
            captureDao.observeCapture(captureId),
            captureDao.observeAnalysis(captureId),
            captureDao.observeExtractedItemsForCapture(captureId),
            missionDao.observeMissionsForCapture(captureId),
        ) { capture, analysis, extracted, missions ->
            capture?.let {
                    CaptureDetailModel(
                        id = it.id,
                        sourceLabel = it.sourceType,
                        imagePath = it.filePath,
                        status = it.processingStatus,
                        documentType = analysis?.documentType,
                        ocrText = analysis?.ocrText.orEmpty(),
                        summary = analysis?.summary.orEmpty(),
                        candidates = extracted.map { item ->
                            item.toExtractionCandidate()
                        },
                        linkedMissions = missions.map { mission ->
                            CaptureMissionLink(
                                id = mission.id,
                                title = mission.title,
                                status = mission.status,
                            )
                        },
                    )
            }
        }
    }

    private fun ExtractedItemEntity.toExtractionCandidate(): ExtractionCandidate {
        return ExtractionCandidate(
            id = id,
            captureId = captureId,
            title = title,
            body = body,
            confidence = confidence,
            dueAt = dueAt,
            kind = kind,
            reasoning = reasoning,
            status = status,
        )
    }

    override suspend fun saveCapture(uri: Uri, sourceType: String): String {
        val id = UUID.randomUUID().toString()
        val imageDir = File(context.filesDir, "captures").apply { mkdirs() }
        val target = File(imageDir, "$id.jpg")
        openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open image")
        captureDao.upsertCapture(
            CaptureEntity(
                id = id,
                createdAt = System.currentTimeMillis(),
                sourceType = sourceType,
                filePath = target.absolutePath,
                thumbnailPath = target.absolutePath,
                mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
                processingStatus = CaptureProcessingStatus.PENDING,
                retryCount = 0,
            ),
        )
        return id
    }

    override suspend fun importCapture(uri: Uri): String = saveCapture(uri, sourceType = "import")

    override suspend fun dismissCandidate(candidateId: String) {
        captureDao.updateExtractedStatus(candidateId, ExtractionStatus.DISMISSED.name)
    }

    override suspend fun deleteCapture(captureId: String): Boolean {
        val capture = captureDao.getCapture(captureId) ?: return false
        missionDao.detachSourceCapture(captureId)
        searchDao.deleteNodesForCapture(captureId)
        captureDao.clearExtractedItemsForCapture(captureId)
        captureDao.clearAnalysisForCapture(captureId)
        captureDao.deleteCapture(captureId)
        workManager.cancelUniqueWork("capture-processing-$captureId")
        runCatching { File(capture.filePath).takeIf(File::exists)?.delete() }
        if (capture.thumbnailPath != capture.filePath) {
            runCatching { File(capture.thumbnailPath).takeIf(File::exists)?.delete() }
        }
        return true
    }

    private fun openInputStream(uri: Uri) = when (uri.scheme) {
        "file" -> FileInputStream(File(requireNotNull(uri.path)))
        else -> context.contentResolver.openInputStream(uri)
    }
}

@Singleton
class DefaultProcessingOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val captureDao: CaptureDao,
    private val searchDao: SearchDao,
    private val workManager: WorkManager,
    private val openAiExtractionProvider: OpenAiExtractionProvider,
) : ProcessingOrchestrator {
    override suspend fun enqueue(captureId: String) {
        captureDao.updateStatus(captureId, CaptureProcessingStatus.PENDING.name)
        workManager.enqueueUniqueWork(
            uniqueWorkName(captureId),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CaptureProcessingWorker>()
                .setInputData(workDataOf(CaptureProcessingWorker.CAPTURE_ID_KEY to captureId))
                .build(),
        )
    }

    override suspend fun recoverPendingCaptures(limit: Int): Int {
        val recoverableCaptureIds = captureDao.listRecoverableCaptureIds(limit = limit)
        recoverableCaptureIds.forEach { captureId ->
            enqueue(captureId)
        }
        return recoverableCaptureIds.size
    }

    override suspend fun processNextPendingCapture(): ProcessingResult {
        val capture = captureDao.nextPendingCapture() ?: return ProcessingResult.Deferred("", "No pending captures")
        return processCapture(capture.id)
    }

    override suspend fun processCapture(captureId: String): ProcessingResult {
        val capture = captureDao.getCapture(captureId) ?: return ProcessingResult.Failure(captureId, "Capture not found")
        captureDao.updateStatus(capture.id, CaptureProcessingStatus.PROCESSING.name)
        return try {
            captureDao.clearAnalysisForCapture(capture.id)
            captureDao.clearExtractedItemsForCapture(capture.id)
            val image = InputImage.fromFilePath(context, File(capture.filePath).toUri())
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()
            val localLines = result.textBlocks.mapNotNull { it.text.trim().takeIf(String::isNotBlank) }
            val documentType = classifyDocumentType(result.text)
            val providerImageDataUrl = buildProviderImageDataUrl(capture.filePath)
            val enhanced = if (result.text.isNotBlank() || providerImageDataUrl != null) {
                openAiExtractionProvider.enhance(
                    ProviderExtractionRequest(
                        ocrText = result.text,
                        documentHint = documentType.name,
                        imageDataUrl = providerImageDataUrl,
                    ),
                ).getOrNull()
            } else {
                null
            }
            val extractionDrafts = buildExtractionDrafts(
                documentType = documentType,
                ocrText = result.text,
                localLines = localLines,
                providerItems = enhanced?.items.orEmpty(),
            )
            val lowConfidence = result.text.isBlank() || extractionDrafts.isEmpty() || extractionDrafts.none { it.confidence >= 0.64f }
            val analysis = CaptureAnalysisEntity(
                id = UUID.randomUUID().toString(),
                captureId = capture.id,
                documentType = documentType,
                ocrText = result.text,
                summary = enhanced?.summary?.ifBlank { null }
                    ?: extractionDrafts.firstOrNull { it.kind == ExtractionKind.TASK }?.title
                    ?: extractionDrafts.firstOrNull { it.kind == ExtractionKind.FACT }?.title
                    ?: extractionDrafts.firstOrNull()?.title
                    ?: localLines.firstOrNull()
                    ?: result.text.lineSequence().firstOrNull { it.isNotBlank() }
                    .orEmpty(),
                processedAt = System.currentTimeMillis(),
                errorCode = if (lowConfidence) "LOW_CONFIDENCE" else null,
            )
            val candidates = extractionDrafts.map { draft ->
                ExtractedItemEntity(
                    id = UUID.randomUUID().toString(),
                    captureId = capture.id,
                    title = draft.title,
                    body = draft.body,
                    confidence = draft.confidence,
                    dueAt = draft.dueAt,
                    kind = draft.kind,
                    reasoning = draft.reasoning,
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
            captureDao.updateRetryCount(capture.id, 0)
            ProcessingResult.Success(capture.id, candidates.count { it.kind == ExtractionKind.TASK })
        } catch (error: Throwable) {
            captureDao.updateStatus(capture.id, CaptureProcessingStatus.FAILED.name)
            captureDao.updateRetryCount(capture.id, capture.retryCount + 1)
            ProcessingResult.Failure(capture.id, error.message ?: "Processing failed")
        }
    }

    private fun uniqueWorkName(captureId: String) = "capture-processing-$captureId"

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
    private val reminderDao: ReminderDao,
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
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
                    remindAt = it.remindAt,
                    sourceCaptureId = it.sourceCaptureId,
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMission(missionId: String): Flow<MissionDetailModel?> {
        return missionDao.observeMission(missionId).flatMapLatest { mission ->
            if (mission == null) {
                flowOf(null)
            } else if (mission.sourceCaptureId == null) {
                flowOf(mission.toMissionDetailModel(sourceImagePath = null))
            } else {
                captureDao.observeCapture(mission.sourceCaptureId).map { capture ->
                    mission.toMissionDetailModel(sourceImagePath = capture?.filePath)
                }
            }
        }
    }

    override suspend fun promoteCandidate(candidateId: String) {
        val candidate = captureDao.getExtractedItem(candidateId) ?: return
        if (candidate.kind !in setOf(ExtractionKind.TASK, ExtractionKind.DATE)) return
        val mission = MissionEntity(
            id = UUID.randomUUID().toString(),
            title = candidate.title,
            description = candidate.body,
            priorityScore = priorityFor(candidate),
            status = MissionStatus.OPEN,
            dueAt = candidate.dueAt,
            remindAt = candidate.dueAt,
            sourceCaptureId = candidate.captureId,
            createdAt = System.currentTimeMillis(),
        )
        missionDao.upsertMission(mission)
        if (mission.remindAt != null) {
            scheduleReminder(mission.id, mission.title, mission.description, mission.remindAt)
        }
        captureDao.updateExtractedStatus(candidateId, ExtractionStatus.PROMOTED.name)
    }

    override suspend fun applyAction(action: MissionAction) {
        when (action) {
            is MissionAction.Archive -> {
                missionDao.updateMissionStatus(action.missionId, MissionStatus.ARCHIVED.name)
                reminderDao.updateReminderStateForMission(action.missionId, ReminderState.CANCELLED.name)
                workManager.cancelUniqueWork(reminderWorkName(action.missionId))
            }
            is MissionAction.Complete -> {
                missionDao.updateMissionStatus(action.missionId, MissionStatus.DONE.name)
                reminderDao.updateReminderStateForMission(action.missionId, ReminderState.CANCELLED.name)
                workManager.cancelUniqueWork(reminderWorkName(action.missionId))
            }
            is MissionAction.Pin -> missionDao.updateMissionStatus(action.missionId, MissionStatus.ACTIVE.name)
            is MissionAction.Activate -> missionDao.updateMissionStatus(action.missionId, MissionStatus.ACTIVE.name)
            is MissionAction.Snooze -> {
                missionDao.updateMissionStatus(action.missionId, MissionStatus.SNOOZED.name)
                missionDao.updateMissionRemindAt(action.missionId, action.untilEpochMillis)
                val mission = missionDao.getMission(action.missionId) ?: return
                scheduleReminder(action.missionId, mission.title, mission.description, action.untilEpochMillis)
            }
            is MissionAction.UpdateDueDate -> {
                missionDao.updateMissionDueAt(action.missionId, action.dueAt)
                val mission = missionDao.getMission(action.missionId) ?: return
                if (mission.remindAt == null && action.dueAt != null) {
                    missionDao.updateMissionRemindAt(action.missionId, action.dueAt)
                    scheduleReminder(action.missionId, mission.title, mission.description, action.dueAt)
                }
            }
            is MissionAction.UpdateReminderAt -> {
                missionDao.updateMissionRemindAt(action.missionId, action.remindAt)
                reminderDao.updateReminderStateForMission(action.missionId, ReminderState.CANCELLED.name)
                workManager.cancelUniqueWork(reminderWorkName(action.missionId))
                if (action.remindAt != null) {
                    val mission = missionDao.getMission(action.missionId) ?: return
                    scheduleReminder(action.missionId, mission.title, mission.description, action.remindAt)
                }
            }
            is MissionAction.UpdateDescription -> {
                missionDao.updateMissionDescription(action.missionId, action.description.trim())
            }
            is MissionAction.UpdatePriority -> {
                missionDao.updateMissionPriority(action.missionId, action.priorityScore.coerceIn(0, 100))
            }
        }
    }

    private fun priorityFor(candidate: ExtractedItemEntity): Int {
        return when {
            candidate.kind == ExtractionKind.DATE -> 88
            candidate.body.contains("today", ignoreCase = true) -> 95
            candidate.body.contains("urgent", ignoreCase = true) -> 90
            candidate.kind == ExtractionKind.TASK -> (candidate.confidence * 100).toInt().coerceAtLeast(55)
            else -> (candidate.confidence * 100).toInt()
        }
    }

    private fun MissionEntity.toMissionDetailModel(sourceImagePath: String?): MissionDetailModel {
        return MissionDetailModel(
            id = id,
            title = title,
            description = description,
            priorityScore = priorityScore,
            status = status,
            dueAt = dueAt,
            remindAt = remindAt,
            sourceCaptureId = sourceCaptureId,
            sourceImagePath = sourceImagePath,
        )
    }

    private suspend fun scheduleReminder(
        missionId: String,
        title: String,
        description: String,
        triggerAt: Long,
    ) {
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            missionId = missionId,
            scheduledAt = triggerAt,
            state = ReminderState.PENDING,
        )
        reminderDao.upsertReminder(reminder)
        val delay = max(triggerAt - System.currentTimeMillis(), 0L)
        workManager.enqueueUniqueWork(
            reminderWorkName(missionId),
            REPLACE,
            OneTimeWorkRequestBuilder<MissionReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        MissionReminderWorker.MISSION_ID_KEY to missionId,
                        MissionReminderWorker.TITLE_KEY to title,
                        MissionReminderWorker.DESCRIPTION_KEY to description,
                    ),
                )
                .build(),
        )
    }

    private fun reminderWorkName(missionId: String) = "mission-reminder-$missionId"
}

@Singleton
class DefaultSearchRepository @Inject constructor(
    private val searchDao: SearchDao,
) : SearchRepository {
    override suspend fun search(query: String): List<SearchResultModel> {
        val normalizedQuery = query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "\"${it.replace("\"", "")}\"*" }
        val exact = searchDao.search(normalizedQuery).map {
            SearchResultModel(
                id = it.id,
                captureId = it.captureId,
                title = it.title,
                snippet = it.body.take(160),
                sourceLabel = "capture",
                matchLabel = "Exact",
            )
        }
        if (exact.size >= 8) return exact

        val queryTokens = query.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 2 }
            .toSet()
        if (queryTokens.isEmpty()) return exact

        val exactIds = exact.mapTo(mutableSetOf()) { it.id }
        val related = searchDao.listNodes()
            .asSequence()
            .filterNot { it.id in exactIds }
            .mapNotNull { node ->
                val haystack = "${node.title} ${node.body}".lowercase()
                val score = semanticScore(queryTokens, haystack)
                if (score < 0.34f) null else node to score
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { (node, _) ->
                SearchResultModel(
                    id = node.id,
                    captureId = node.captureId,
                    title = node.title,
                    snippet = node.body.take(160),
                    sourceLabel = "capture",
                    matchLabel = "Related",
                )
            }
            .toList()

        return exact + related
    }
}

@Singleton
class DefaultArchiveService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportBundleDao: ExportBundleDao,
    private val captureDao: CaptureDao,
    private val missionDao: MissionDao,
    private val reminderDao: ReminderDao,
    private val searchDao: SearchDao,
) : ArchiveService {
    override suspend fun exportSnapshot(target: Uri): Result<Unit> {
        return runCatching {
            val captures = captureDao.listCaptures()
            val analyses = captureDao.listAnalyses()
            val extractedItems = captureDao.listExtractedItems()
            val missions = missionDao.listMissions()
            val reminders = reminderDao.listReminders()
            val nodes = searchDao.listNodes()
            val manifest = JSONObject().apply {
                put("version", 3)
                put("exportedAt", System.currentTimeMillis())
                put("counts", JSONObject().apply {
                    put("captures", captures.size)
                    put("analyses", analyses.size)
                    put("extractedItems", extractedItems.size)
                    put("missions", missions.size)
                    put("reminders", reminders.size)
                    put("knowledgeNodes", nodes.size)
                })
            }

            context.contentResolver.openOutputStream(target)?.use { stream ->
                ZipOutputStream(stream.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write(manifest.toString(2).toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("data/captures.json"))
                    zip.write(JSONArray(captures.map { it.toJson() }).toString().toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("data/analyses.json"))
                    zip.write(JSONArray(analyses.map { it.toJson() }).toString().toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("data/extracted_items.json"))
                    zip.write(JSONArray(extractedItems.map { it.toJson() }).toString().toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("data/missions.json"))
                    zip.write(JSONArray(missions.map { it.toJson() }).toString().toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("data/reminders.json"))
                    zip.write(JSONArray(reminders.map { it.toJson() }).toString().toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry("data/knowledge_nodes.json"))
                    zip.write(JSONArray(nodes.map { it.toJson() }).toString().toByteArray())
                    zip.closeEntry()

                    captures.forEach { capture ->
                        val file = File(capture.filePath)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry("captures/${capture.id}.jpg"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            } ?: error("Unable to open export target")
            exportBundleDao.upsertBundle(
                ExportBundleEntity(
                    id = UUID.randomUUID().toString(),
                    createdAt = System.currentTimeMillis(),
                    fileUri = target.toString(),
                    checksum = "zip-v3",
                    itemCount = captures.size + analyses.size + extractedItems.size + missions.size + reminders.size + nodes.size,
                ),
            )
        }
    }

    override suspend fun validateImport(source: Uri): ArchiveValidationResult {
        return runCatching {
            val entries = readArchiveEntries(source)
            val manifest = entries["manifest.json"]?.decodeToString()?.let(::JSONObject)
                ?: return ArchiveValidationResult.Invalid("Missing manifest.json")
            if (manifest.optInt("version") !in setOf(2, 3)) {
                ArchiveValidationResult.Invalid("Unsupported archive version")
            } else if (!entries.containsKey("data/captures.json") || !entries.containsKey("data/missions.json")) {
                ArchiveValidationResult.Invalid("Archive data is incomplete")
            } else {
                ArchiveValidationResult.Valid
            }
        }.getOrElse { ArchiveValidationResult.Invalid(it.message ?: "Invalid archive") }
    }

    override suspend fun importSnapshot(source: Uri): Result<Unit> {
        return runCatching {
            val entries = readArchiveEntries(source)
            val manifest = entries["manifest.json"]?.decodeToString()?.let(::JSONObject)
                ?: error("Missing manifest")
            val archiveVersion = manifest.optInt("version")
            require(archiveVersion in setOf(2, 3)) { "Unsupported archive version" }

            val captures = entries.requireJsonArray("data/captures.json")
            val analyses = entries.requireJsonArray("data/analyses.json")
            val extractedItems = entries.requireJsonArray("data/extracted_items.json")
            val missions = entries.requireJsonArray("data/missions.json")
            val reminders = entries.requireJsonArray("data/reminders.json")
            val nodes = entries.requireJsonArray("data/knowledge_nodes.json")

            reminderDao.clearReminders()
            missionDao.clearMissions()
            captureDao.clearExtractedItems()
            captureDao.clearAnalyses()
            captureDao.clearCaptures()
            searchDao.clearNodes()

            for (index in 0 until captures.length()) {
                val json = captures.getJSONObject(index)
                val captureId = json.getString("id")
                val captureFile = File(context.filesDir, "captures/${captureId}.jpg").apply {
                    parentFile?.mkdirs()
                }
                entries["captures/$captureId.jpg"]?.let { bytes ->
                    captureFile.outputStream().use { it.write(bytes) }
                }
                captureDao.upsertCapture(
                    CaptureEntity(
                        id = captureId,
                        createdAt = json.getLong("createdAt"),
                        sourceType = json.getString("sourceType"),
                        filePath = captureFile.absolutePath,
                        thumbnailPath = captureFile.absolutePath,
                        mimeType = json.getString("mimeType"),
                        processingStatus = CaptureProcessingStatus.valueOf(json.getString("processingStatus")),
                        retryCount = json.optInt("retryCount", 0),
                    ),
                )
            }

            for (index in 0 until analyses.length()) {
                val json = analyses.getJSONObject(index)
                captureDao.upsertAnalysis(
                    CaptureAnalysisEntity(
                        id = json.getString("id"),
                        captureId = json.getString("captureId"),
                        documentType = DocumentType.valueOf(json.getString("documentType")),
                        ocrText = json.getString("ocrText"),
                        summary = json.getString("summary"),
                        processedAt = json.getLong("processedAt"),
                        errorCode = json.optString("errorCode").takeIf { it.isNotBlank() },
                    ),
                )
            }

            val extractedEntities = buildList {
                for (index in 0 until extractedItems.length()) {
                    val json = extractedItems.getJSONObject(index)
                    add(
                        ExtractedItemEntity(
                            id = json.getString("id"),
                            captureId = json.getString("captureId"),
                            title = json.getString("title"),
                            body = json.getString("body"),
                            confidence = json.getDouble("confidence").toFloat(),
                            dueAt = json.optLongOrNull("dueAt"),
                            kind = json.optString("kind")
                                .takeIf { it.isNotBlank() }
                                ?.let(ExtractionKind::valueOf)
                                ?: deriveExtractionKind(json.getString("body")),
                            reasoning = json.optString("reasoning")
                                .takeIf { it.isNotBlank() }
                                ?: if (archiveVersion >= 3) "Imported extraction" else "Imported legacy extraction",
                            status = ExtractionStatus.valueOf(json.getString("status")),
                        ),
                    )
                }
            }
            if (extractedEntities.isNotEmpty()) {
                captureDao.upsertExtractedItems(extractedEntities)
            }

            for (index in 0 until missions.length()) {
                val json = missions.getJSONObject(index)
                missionDao.upsertMission(
                    MissionEntity(
                        id = json.getString("id"),
                        title = json.getString("title"),
                        description = json.getString("description"),
                        priorityScore = json.getInt("priorityScore"),
                        status = MissionStatus.valueOf(json.getString("status")),
                        dueAt = json.optLongOrNull("dueAt"),
                        remindAt = json.optLongOrNull("remindAt"),
                        sourceCaptureId = json.optString("sourceCaptureId").takeIf { it.isNotBlank() },
                        createdAt = json.getLong("createdAt"),
                    ),
                )
            }

            for (index in 0 until reminders.length()) {
                val json = reminders.getJSONObject(index)
                reminderDao.upsertReminder(
                    ReminderEntity(
                        id = json.getString("id"),
                        missionId = json.getString("missionId"),
                        scheduledAt = json.getLong("scheduledAt"),
                        state = ReminderState.valueOf(json.getString("state")),
                    ),
                )
            }

            for (index in 0 until nodes.length()) {
                val json = nodes.getJSONObject(index)
                searchDao.upsertNode(
                    KnowledgeNodeEntity(
                        id = json.getString("id"),
                        captureId = json.getString("captureId"),
                        title = json.getString("title"),
                        body = json.getString("body"),
                    ),
                )
            }
        }
    }

    private fun readArchiveEntries(source: Uri): Map<String, ByteArray> {
        val input = context.contentResolver.openInputStream(source) ?: error("Unable to open archive")
        return input.use(::readZipEntries)
    }
}

private fun readZipEntries(inputStream: InputStream): Map<String, ByteArray> {
    val entries = mutableMapOf<String, ByteArray>()
    ZipInputStream(inputStream.buffered()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                entries[entry.name] = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return entries
}

private fun buildProviderImageDataUrl(filePath: String): String? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(filePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var inSampleSize = 1
    while ((bounds.outWidth / inSampleSize) > 1280 || (bounds.outHeight / inSampleSize) > 1280) {
        inSampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
    }
    val bitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null
    val output = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, output)
    bitmap.recycle()
    val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$encoded"
}

private data class CandidateDraft(
    val title: String,
    val body: String,
    val confidence: Float,
    val score: Float,
)

private fun buildCandidateDrafts(
    ocrText: String,
    localLines: List<String>,
    providerLines: List<String>,
): List<CandidateDraft> {
    val providerEntries = providerLines.map { it to true }
    val localEntries = buildRawCandidateLines(ocrText, localLines).map { it to false }
    return (providerEntries + localEntries)
        .asSequence()
        .mapNotNull { (line, fromProvider) ->
            val cleaned = cleanCandidateLine(line)
            val normalized = normalizeExtractionLine(cleaned)
            if (!isUsefulCandidateLine(cleaned, normalized)) return@mapNotNull null
            val signal = candidateSignal(cleaned, normalized, fromProvider)
            if (!shouldKeepAsMissionCandidate(signal)) return@mapNotNull null
            val score = candidateScore(signal)
            CandidateDraft(
                title = deriveCandidateTitle(cleaned),
                body = cleaned,
                confidence = candidateConfidence(score),
                score = score,
            ) to normalized
        }
        .groupBy({ it.second }, { it.first })
        .values
        .map { drafts -> drafts.maxBy { it.score } }
        .sortedByDescending { it.score }
        .take(5)
}

private fun buildRawCandidateLines(ocrText: String, localLines: List<String>): List<String> {
    val fromText = ocrText
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    return (fromText + localLines)
        .flatMap { entry ->
            entry.split(Regex("[\\n•]+"))
                .map(String::trim)
                .filter(String::isNotBlank)
        }
}

private fun cleanCandidateLine(value: String): String {
    return value
        .replace(Regex("\\s+"), " ")
        .replace(Regex("^[\\-•*\\d.)\\s]+"), "")
        .trim()
}

private fun normalizeExtractionLine(value: String): String {
    return value.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun isUsefulCandidateLine(cleaned: String, normalized: String): Boolean {
    if (normalized.length < 4) return false
    if (normalized.all { it.isDigit() || it == ' ' }) return false
    if (normalized.split(' ').size == 1 && normalized.length < 5) return false
    if (looksLikeMetaOrCodeLine(cleaned, normalized)) return false
    val alphaNumericCount = cleaned.count { it.isLetterOrDigit() }
    if (alphaNumericCount < 4) return false
    val uniqueAlphaNumeric = cleaned.filter { it.isLetterOrDigit() }.lowercase().toSet().size
    if (uniqueAlphaNumeric <= 1) return false
    return true
}

private data class CandidateSignal(
    val cleaned: String,
    val normalized: String,
    val fromProvider: Boolean,
    val hasActionHint: Boolean,
    val hasDateHint: Boolean,
    val hasStructureHint: Boolean,
    val startsWithVerbLikeToken: Boolean,
    val balancedLength: Boolean,
    val wordWindow: Boolean,
    val digitHeavy: Boolean,
    val looksLikeMetaOrCode: Boolean,
    val looksLikeStatement: Boolean,
)

private fun candidateSignal(cleaned: String, normalized: String, fromProvider: Boolean): CandidateSignal {
    val lowered = cleaned.lowercase()
    val words = normalized.split(' ').filter { it.isNotBlank() }
    val hasActionHint = ActionHints.any { Regex("""\b$it\b""").containsMatchIn(lowered) }
    val hasDateHint = Regex("\\b\\d{1,2}[./-]\\d{1,2}([./-]\\d{2,4})?\\b").containsMatchIn(cleaned) ||
        Regex("\\b(today|tomorrow|tonight|heute|morgen)\\b").containsMatchIn(lowered)
    val hasStructureHint = ':' in cleaned ||
        cleaned.startsWith("[") ||
        cleaned.startsWith("(") ||
        cleaned.startsWith("todo", ignoreCase = true) ||
        cleaned.startsWith("fix", ignoreCase = true) ||
        cleaned.startsWith("check", ignoreCase = true) ||
        cleaned.startsWith("review", ignoreCase = true)
    val startsWithVerbLikeToken = words.firstOrNull() in VerbLikeLeadingTokens
    val balancedLength = cleaned.length in 12..120
    val wordWindow = words.size in 3..14
    val digitHeavy = cleaned.count(Char::isDigit) > cleaned.count(Char::isLetter)
    val looksLikeMetaOrCode = looksLikeMetaOrCodeLine(cleaned, normalized)
    val looksLikeStatement = lowered.startsWith("the ") ||
        lowered.startsWith("this ") ||
        lowered.startsWith("that ") ||
        lowered.startsWith("meanwhile ") ||
        lowered.startsWith("wir ") ||
        lowered.startsWith("es ") ||
        lowered.startsWith("sidequest ") ||
        lowered.endsWith("?").not() && words.size > 8 && !hasActionHint && !hasDateHint

    return CandidateSignal(
        cleaned = cleaned,
        normalized = normalized,
        fromProvider = fromProvider,
        hasActionHint = hasActionHint,
        hasDateHint = hasDateHint,
        hasStructureHint = hasStructureHint,
        startsWithVerbLikeToken = startsWithVerbLikeToken,
        balancedLength = balancedLength,
        wordWindow = wordWindow,
        digitHeavy = digitHeavy,
        looksLikeMetaOrCode = looksLikeMetaOrCode,
        looksLikeStatement = looksLikeStatement,
    )
}

private fun shouldKeepAsMissionCandidate(signal: CandidateSignal): Boolean {
    if (!signal.balancedLength || !signal.wordWindow || signal.digitHeavy || signal.looksLikeMetaOrCode) return false
    if (signal.hasActionHint || signal.hasDateHint || signal.hasStructureHint || signal.startsWithVerbLikeToken) {
        return true
    }
    return signal.fromProvider && !signal.looksLikeStatement && signal.cleaned.length in 18..80
}

private fun candidateScore(signal: CandidateSignal): Float {
    var score = 0.25f
    if (signal.fromProvider) score += 0.18f
    if (signal.hasActionHint) score += 0.28f
    if (signal.hasDateHint) score += 0.16f
    if (signal.hasStructureHint) score += 0.08f
    if (signal.startsWithVerbLikeToken) score += 0.12f
    if (signal.balancedLength) score += 0.08f
    if (signal.wordWindow) score += 0.08f
    if (signal.looksLikeStatement) score -= 0.2f
    if (signal.looksLikeMetaOrCode) score -= 0.35f
    if (signal.digitHeavy) score -= 0.15f
    return score.coerceIn(0.2f, 0.92f)
}

private fun candidateConfidence(score: Float): Float {
    return when {
        score >= 0.8f -> 0.9f
        score >= 0.68f -> 0.75f
        score >= 0.55f -> 0.6f
        else -> 0.4f
    }
}

private fun deriveCandidateTitle(cleaned: String): String {
    val compact = cleaned.trim().removeSuffix(".")
    if (compact.length <= 64) return compact
    val words = compact.split(' ')
    return buildString {
        for (word in words) {
            if (isNotEmpty() && length + word.length + 1 > 64) break
            if (isNotEmpty()) append(' ')
            append(word)
        }
    }.ifBlank { compact.take(64) }
}

private fun semanticScore(queryTokens: Set<String>, haystack: String): Float {
    val haystackTokens = haystack.split(Regex("\\W+")).filter { it.length > 2 }.toSet()
    if (haystackTokens.isEmpty()) return 0f
    val overlap = queryTokens.intersect(haystackTokens).size.toFloat() / queryTokens.size
    val substringBoost = queryTokens.count { it in haystack }.toFloat() / queryTokens.size
    return (overlap * 0.7f) + (substringBoost * 0.3f)
}

private fun looksLikeMetaOrCodeLine(cleaned: String, normalized: String): Boolean {
    val lowered = cleaned.lowercase()
    if (MetaLinePhrases.any { it in lowered }) return true
    if (Regex("""\b[a-z0-9_]+\.(kt|java|xml|gradle|md|json)\b""").containsMatchIn(lowered)) return true
    if (Regex("""(:app:|testdebugunittest|assembledebug|gradlew|repositoryimpl|viewmodel|build successful|selectobject|adb exec-out)""").containsMatchIn(lowered)) return true
    if (Regex("""[\\/].+\.[a-z]{2,6}\b""").containsMatchIn(cleaned)) return true
    if (normalized.count { it == ' ' } <= 1 && normalized.any(Char::isDigit)) return true
    return false
}

private fun deriveExtractionKind(value: String): ExtractionKind {
    val lowered = value.lowercase()
    return when {
        Regex("\\b(reference|ref|id|number|nr|code)\\b").containsMatchIn(lowered) -> ExtractionKind.REFERENCE
        Regex("\\b\\d{1,2}[./-]\\d{1,2}([./-]\\d{2,4})?\\b").containsMatchIn(value) ||
            Regex("\\b(january|february|march|april|may|june|july|august|september|october|november|december|today|tomorrow|heute|morgen)\\b").containsMatchIn(lowered) ->
            ExtractionKind.DATE
        ActionHints.any { Regex("""\b$it\b""").containsMatchIn(lowered) } ||
            value.trim().split(Regex("\\s+")).firstOrNull()?.lowercase() in VerbLikeLeadingTokens ->
            ExtractionKind.TASK
        else -> ExtractionKind.FACT
    }
}

private fun deriveDueAt(value: String): Long? {
    val trimmed = value.trim()
    val lowered = trimmed.lowercase(Locale.ROOT)
    val now = Calendar.getInstance()

    if (Regex("""\btoday\b|\bheute\b""").containsMatchIn(lowered)) {
        return now.apply {
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    if (Regex("""\btomorrow\b|\bmorgen\b""").containsMatchIn(lowered)) {
        return now.apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val patterns = listOf(
        "d MMMM yyyy",
        "d MMM yyyy",
        "dd MMMM yyyy",
        "dd MMM yyyy",
        "d.M.yyyy",
        "dd.MM.yyyy",
        "d/M/yyyy",
        "dd/MM/yyyy",
        "d-M-yyyy",
        "dd-MM-yyyy",
        "M/d/yyyy",
        "MM/dd/yyyy",
    )

    val normalized = trimmed
        .replace(Regex("""\b(until|valid until|due|by|am|bis|eta)\b""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    for (pattern in patterns) {
        parseDateCandidate(normalized, pattern)?.let { return it }
    }

    Regex("""\b\d{1,2}[./-]\d{1,2}([./-]\d{2,4})?\b""")
        .find(trimmed)
        ?.value
        ?.let { token ->
            for (pattern in patterns.filter { it.contains("d") || it.contains("M") }) {
                parseDateCandidate(token, pattern)?.let { return it }
            }
        }

    return null
}

private fun parseDateCandidate(value: String, pattern: String): Long? {
    val locales = listOf(Locale.ENGLISH, Locale.GERMAN)
    return locales.firstNotNullOfOrNull { locale ->
        val parser = SimpleDateFormat(pattern, locale).apply { isLenient = false }
        runCatching {
            parser.parse(value)?.let { parsed ->
                Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }.getOrNull()
    }
}

private val ActionHints = setOf(
    "add",
    "book",
    "buy",
    "call",
    "check",
    "email",
    "fix",
    "follow",
    "need",
    "pay",
    "plan",
    "review",
    "schedule",
    "send",
    "set",
    "todo",
    "update",
    "anrufen",
    "bezahlen",
    "kaufen",
    "planen",
    "prufen",
)

private val VerbLikeLeadingTokens = setOf(
    "add",
    "book",
    "buy",
    "call",
    "check",
    "email",
    "fix",
    "follow",
    "pay",
    "plan",
    "review",
    "schedule",
    "send",
    "set",
    "update",
    "anrufen",
    "bezahlen",
    "kaufen",
    "planen",
    "prufen",
)

private val MetaLinePhrases = setOf(
    "nächster schritt",
    "nächster sinnvoller schritt",
    "bearbeitet",
    "ausgeführt",
    "validated with",
    "build successful",
    "import image",
    "mission control",
    "sidequest app",
)

private fun Map<String, ByteArray>.requireJsonArray(path: String): JSONArray {
    return this[path]?.decodeToString()?.let(::JSONArray) ?: error("Missing $path")
}

private fun JSONArray.forEachJsonObject(block: (JSONObject) -> Unit) {
    for (index in 0 until length()) {
        block(getJSONObject(index))
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    return if (isNull(key) || !has(key)) null else getLong(key)
}

private fun CaptureEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("createdAt", createdAt)
    put("sourceType", sourceType)
    put("mimeType", mimeType)
    put("processingStatus", processingStatus.name)
    put("retryCount", retryCount)
}

private fun CaptureAnalysisEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("captureId", captureId)
    put("documentType", documentType.name)
    put("ocrText", ocrText)
    put("summary", summary)
    put("processedAt", processedAt)
    put("errorCode", errorCode)
}

private fun ExtractedItemEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("captureId", captureId)
    put("title", title)
    put("body", body)
    put("confidence", confidence.toDouble())
    put("dueAt", dueAt)
    put("kind", kind.name)
    put("reasoning", reasoning)
    put("status", status.name)
}

private fun MissionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("description", description)
    put("priorityScore", priorityScore)
    put("status", status.name)
    put("dueAt", dueAt)
    put("remindAt", remindAt)
    put("sourceCaptureId", sourceCaptureId)
    put("createdAt", createdAt)
}

private fun ReminderEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("missionId", missionId)
    put("scheduledAt", scheduledAt)
    put("state", state.name)
}

private fun KnowledgeNodeEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("captureId", captureId)
    put("title", title)
    put("body", body)
}

@Singleton
class DefaultSecurityService @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecurityService {
    override suspend fun saveProviderKey(kind: ProviderKind, apiKey: String) {
        val prefs = securePrefs()
        val normalizedKey = apiKey.trim()
        check(prefs.edit().putString(kind.name, normalizedKey).commit()) { "Provider key could not be stored" }
    }

    override suspend fun getProviderAvailability(): List<ProviderAvailability> {
        val prefs = securePrefs()
        return ProviderKind.entries.map { kind ->
            val configured = !prefs.getString(kind.name, null).isNullOrBlank()
            ProviderAvailability(kind = kind, configured = configured, enabled = configured)
        }
    }

    override suspend fun getProviderKey(kind: ProviderKind): String? {
        return securePrefs().getString(kind.name, null)
    }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        check(securePrefs().edit().putBoolean("biometric_lock_enabled", enabled).commit()) { "Biometric setting could not be stored" }
    }

    override suspend fun isBiometricLockEnabled(): Boolean {
        return securePrefs().getBoolean("biometric_lock_enabled", false)
    }

    private fun securePrefs(): SharedPreferences {
        return runCatching {
            EncryptedSharedPreferences.create(
                context,
                "sidequest_secure",
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            context.getSharedPreferences("sidequest_secure_fallback", Context.MODE_PRIVATE)
        }
    }
}

@HiltWorker
class MissionReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val missionId = inputData.getString(MISSION_ID_KEY) ?: return Result.failure()
        val title = inputData.getString(TITLE_KEY) ?: "Mission reminder"
        val description = inputData.getString(DESCRIPTION_KEY).orEmpty()

        val channelId = "sidequest_missions"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "Sidequest Missions", NotificationManager.IMPORTANCE_DEFAULT),
        )

        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
            putExtra("mission_id", missionId)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: return Result.failure()

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            missionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(description.ifBlank { "Your Sidequest mission is due." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(description.ifBlank { "Your Sidequest mission is due." }))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(missionId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val MISSION_ID_KEY = "mission_id"
        const val TITLE_KEY = "title"
        const val DESCRIPTION_KEY = "description"
    }
}
