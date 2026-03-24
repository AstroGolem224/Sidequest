package com.astrogolem.sidequest.core.data.model

enum class DocumentType { NOTE, RECEIPT, LETTER, SCREEN, WHITEBOARD, SCENE, UNKNOWN }
enum class CaptureProcessingStatus { PENDING, PROCESSING, DONE, FAILED }
enum class ExtractionStatus { CANDIDATE, ACCEPTED, DISMISSED, PROMOTED }
enum class MissionStatus { OPEN, ACTIVE, DONE, SNOOZED, ARCHIVED }
enum class ReminderState { PENDING, FIRED, CANCELLED }
enum class ProviderKind { OPENAI, ANTHROPIC, NIM, OPENROUTER }

data class CaptureSummary(
    val id: String,
    val createdAt: Long,
    val sourceLabel: String,
    val previewPath: String,
    val status: CaptureProcessingStatus,
)

data class ExtractionCandidate(
    val id: String,
    val captureId: String,
    val title: String,
    val body: String,
    val confidence: Float,
    val dueAt: Long?,
    val status: ExtractionStatus,
)

data class MissionCardModel(
    val id: String,
    val title: String,
    val description: String,
    val priorityScore: Int,
    val status: MissionStatus,
    val dueAt: Long?,
    val sourceCaptureId: String?,
)

data class MissionDetailModel(
    val id: String,
    val title: String,
    val description: String,
    val priorityScore: Int,
    val status: MissionStatus,
    val dueAt: Long?,
    val sourceCaptureId: String?,
)

data class CaptureMissionLink(
    val id: String,
    val title: String,
    val status: MissionStatus,
)

data class CaptureDetailModel(
    val id: String,
    val sourceLabel: String,
    val imagePath: String,
    val status: CaptureProcessingStatus,
    val ocrText: String,
    val summary: String,
    val candidates: List<ExtractionCandidate>,
    val linkedMissions: List<CaptureMissionLink>,
)

data class SearchResultModel(
    val id: String,
    val captureId: String,
    val title: String,
    val snippet: String,
    val sourceLabel: String,
)

data class ProviderAvailability(
    val kind: ProviderKind,
    val configured: Boolean,
    val enabled: Boolean,
)

sealed interface ProcessingResult {
    data class Success(val captureId: String, val candidateCount: Int) : ProcessingResult
    data class Deferred(val captureId: String, val reason: String) : ProcessingResult
    data class Failure(val captureId: String, val reason: String) : ProcessingResult
}

sealed interface MissionAction {
    data class Complete(val missionId: String) : MissionAction
    data class Snooze(val missionId: String, val untilEpochMillis: Long) : MissionAction
    data class Pin(val missionId: String) : MissionAction
    data class Archive(val missionId: String) : MissionAction
    data class UpdateDueDate(val missionId: String, val dueAt: Long?) : MissionAction
}

sealed interface ArchiveValidationResult {
    data object Valid : ArchiveValidationResult
    data class Invalid(val message: String) : ArchiveValidationResult
}
