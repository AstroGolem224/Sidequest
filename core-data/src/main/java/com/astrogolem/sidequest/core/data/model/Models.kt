package com.astrogolem.sidequest.core.data.model

enum class DocumentType { NOTE, RECEIPT, LETTER, SCREEN, WHITEBOARD, SCENE, UNKNOWN }
enum class CaptureProcessingStatus { PENDING, PROCESSING, DONE, FAILED }
enum class ExtractionStatus { CANDIDATE, ACCEPTED, DISMISSED, PROMOTED }
enum class ExtractionKind { TASK, FACT, DATE, REFERENCE }
enum class MissionStatus { OPEN, ACTIVE, DONE, SNOOZED, ARCHIVED }
enum class ReminderState { PENDING, FIRED, CANCELLED }
enum class ProviderKind { OPENAI, ANTHROPIC, NIM, OPENROUTER }
enum class ShoppingListSource { MANUAL, VOICE, PHOTO }
enum class RoutineCategory { MIND, BODY, HOME, OUTDOOR, LIFE }
enum class RoutineTriggerMode { MANUAL, CAPTURE, FITNESS }

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
    val kind: ExtractionKind,
    val reasoning: String,
    val status: ExtractionStatus,
)

data class MissionCardModel(
    val id: String,
    val title: String,
    val description: String,
    val priorityScore: Int,
    val status: MissionStatus,
    val dueAt: Long?,
    val remindAt: Long?,
    val sourceCaptureId: String?,
)

data class MissionDetailModel(
    val id: String,
    val title: String,
    val description: String,
    val priorityScore: Int,
    val status: MissionStatus,
    val dueAt: Long?,
    val remindAt: Long?,
    val sourceCaptureId: String?,
    val sourceImagePath: String?,
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
    val documentType: DocumentType?,
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
    val matchLabel: String,
)

data class ShoppingListSummary(
    val id: String,
    val title: String,
    val source: ShoppingListSource,
    val createdAt: Long,
    val itemCount: Int,
    val checkedCount: Int,
    val sourceCaptureId: String?,
)

data class ShoppingListItemModel(
    val id: String,
    val label: String,
    val checked: Boolean,
)

data class ShoppingListDetailModel(
    val id: String,
    val title: String,
    val source: ShoppingListSource,
    val createdAt: Long,
    val sourceCaptureId: String?,
    val items: List<ShoppingListItemModel>,
)

data class RoutinePlanSummary(
    val id: String,
    val templateKey: String,
    val title: String,
    val category: RoutineCategory,
    val durationMinutes: Int,
    val recurring: Boolean,
    val weekdays: String,
    val hour: Int,
    val minute: Int,
    val xpReward: Int,
    val triggerMode: RoutineTriggerMode,
    val targetLabel: String,
    val active: Boolean,
    val completionCount: Int,
    val lastCompletedAt: Long?,
)

data class RoutinePlanDetail(
    val id: String,
    val templateKey: String,
    val title: String,
    val category: RoutineCategory,
    val createdAt: Long,
    val durationMinutes: Int,
    val recurring: Boolean,
    val weekdays: String,
    val hour: Int,
    val minute: Int,
    val xpReward: Int,
    val triggerMode: RoutineTriggerMode,
    val targetLabel: String,
    val notes: String,
    val active: Boolean,
    val completionCount: Int,
    val lastCompletedAt: Long?,
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
    data class UpdateReminderAt(val missionId: String, val remindAt: Long?) : MissionAction
    data class UpdateDescription(val missionId: String, val description: String) : MissionAction
    data class UpdatePriority(val missionId: String, val priorityScore: Int) : MissionAction
    data class Activate(val missionId: String) : MissionAction
}

sealed interface ArchiveValidationResult {
    data object Valid : ArchiveValidationResult
    data class Invalid(val message: String) : ArchiveValidationResult
}
