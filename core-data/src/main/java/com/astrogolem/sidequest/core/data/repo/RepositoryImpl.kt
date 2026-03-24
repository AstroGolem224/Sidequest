package com.astrogolem.sidequest.core.data.repo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@Singleton
class DefaultCaptureRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val captureDao: CaptureDao,
    private val missionDao: MissionDao,
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
                    ocrText = analysis?.ocrText.orEmpty(),
                    summary = analysis?.summary.orEmpty(),
                    candidates = extracted.map { item ->
                        ExtractionCandidate(
                            id = item.id,
                            captureId = item.captureId,
                            title = item.title,
                            body = item.body,
                            confidence = item.confidence,
                            dueAt = item.dueAt,
                            status = item.status,
                        )
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
            ),
        )
        return id
    }

    override suspend fun importCapture(uri: Uri): String = saveCapture(uri, sourceType = "import")

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

    override suspend fun processNextPendingCapture(): ProcessingResult {
        val capture = captureDao.nextPendingCapture() ?: return ProcessingResult.Deferred("", "No pending captures")
        return processCapture(capture.id)
    }

    override suspend fun processCapture(captureId: String): ProcessingResult {
        val capture = captureDao.getCapture(captureId) ?: return ProcessingResult.Failure(captureId, "Capture not found")
        captureDao.updateStatus(capture.id, CaptureProcessingStatus.PROCESSING.name)
        return try {
            val image = InputImage.fromFilePath(context, File(capture.filePath).toUri())
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = recognizer.process(image).await()
            val localLines = result.textBlocks.mapNotNull { it.text.trim().takeIf(String::isNotBlank) }
            val documentType = classify(localLines.joinToString("\n"))
            val enhanced = if (result.text.isNotBlank()) {
                openAiExtractionProvider.enhance(
                    ProviderExtractionRequest(
                        ocrText = result.text,
                        documentHint = documentType.name,
                    ),
                ).getOrNull()
            } else {
                null
            }
            val lines = enhanced?.normalizedLines?.takeIf { it.isNotEmpty() } ?: localLines
            val analysis = CaptureAnalysisEntity(
                id = UUID.randomUUID().toString(),
                captureId = capture.id,
                documentType = documentType,
                ocrText = result.text,
                summary = enhanced?.summary?.ifBlank { null } ?: lines.firstOrNull().orEmpty(),
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
                    sourceCaptureId = it.sourceCaptureId,
                )
            }
        }
    }

    override fun observeMission(missionId: String): Flow<MissionDetailModel?> {
        return missionDao.observeMission(missionId).map { mission ->
            mission?.let {
                MissionDetailModel(
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
        val mission = MissionEntity(
            id = UUID.randomUUID().toString(),
            title = candidate.title,
            description = candidate.body,
            priorityScore = priorityFor(candidate),
            status = MissionStatus.OPEN,
            dueAt = candidate.dueAt,
            sourceCaptureId = candidate.captureId,
            createdAt = System.currentTimeMillis(),
        )
        missionDao.upsertMission(mission)
        if (candidate.dueAt != null) {
            scheduleReminder(mission.id, mission.title, mission.description, candidate.dueAt)
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
            is MissionAction.Snooze -> {
                missionDao.updateMissionStatus(action.missionId, MissionStatus.SNOOZED.name)
                val mission = missionDao.getMission(action.missionId) ?: return
                scheduleReminder(action.missionId, mission.title, mission.description, action.untilEpochMillis)
            }
            is MissionAction.UpdateDueDate -> {
                missionDao.updateMissionDueAt(action.missionId, action.dueAt)
                reminderDao.updateReminderStateForMission(action.missionId, ReminderState.CANCELLED.name)
                workManager.cancelUniqueWork(reminderWorkName(action.missionId))
                if (action.dueAt != null) {
                    val mission = missionDao.getMission(action.missionId) ?: return
                    scheduleReminder(action.missionId, mission.title, mission.description, action.dueAt)
                }
            }
        }
    }

    private fun priorityFor(candidate: ExtractedItemEntity): Int {
        return when {
            candidate.body.contains("today", ignoreCase = true) -> 95
            candidate.body.contains("urgent", ignoreCase = true) -> 90
            else -> (candidate.confidence * 100).toInt()
        }
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
        return searchDao.search(normalizedQuery).map {
            SearchResultModel(
                id = it.id,
                captureId = it.captureId,
                title = it.title,
                snippet = it.body.take(160),
                sourceLabel = "capture",
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

    override suspend fun getProviderKey(kind: ProviderKind): String? {
        return securePrefs().getString(kind.name, null)
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
