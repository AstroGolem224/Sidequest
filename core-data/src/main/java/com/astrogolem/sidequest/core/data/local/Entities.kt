package com.astrogolem.sidequest.core.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.DocumentType
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.model.ExtractionStatus
import com.astrogolem.sidequest.core.data.model.MissionStatus
import com.astrogolem.sidequest.core.data.model.ReminderState
import com.astrogolem.sidequest.core.data.model.RoutineCategory
import com.astrogolem.sidequest.core.data.model.RoutineTriggerMode
import com.astrogolem.sidequest.core.data.model.ShoppingListSource

@Entity(tableName = "captures")
data class CaptureEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val sourceType: String,
    val filePath: String,
    val thumbnailPath: String,
    val mimeType: String,
    val processingStatus: CaptureProcessingStatus,
    val retryCount: Int,
)

@Entity(
    tableName = "capture_analysis",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("captureId")],
)
data class CaptureAnalysisEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val documentType: DocumentType,
    val ocrText: String,
    val summary: String,
    val processedAt: Long,
    val errorCode: String?,
)

@Entity(
    tableName = "extracted_items",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("captureId")],
)
data class ExtractedItemEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val title: String,
    val body: String,
    val confidence: Float,
    val dueAt: Long?,
    val kind: ExtractionKind,
    val reasoning: String,
    val status: ExtractionStatus,
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priorityScore: Int,
    val status: MissionStatus,
    val dueAt: Long?,
    val remindAt: Long?,
    val sourceCaptureId: String?,
    val createdAt: Long,
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val colorToken: String,
)

@Entity(tableName = "life_areas")
data class LifeAreaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val themeToken: String,
)

@Entity(tableName = "knowledge_nodes")
data class KnowledgeNodeEntity(
    @PrimaryKey val id: String,
    val captureId: String,
    val title: String,
    val body: String,
)

@Fts4(contentEntity = KnowledgeNodeEntity::class)
@Entity(tableName = "knowledge_nodes_fts")
data class KnowledgeNodeFtsEntity(
    val title: String,
    val body: String,
)

@Entity(tableName = "knowledge_edges")
data class KnowledgeEdgeEntity(
    @PrimaryKey val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val edgeType: String,
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val missionId: String,
    val scheduledAt: Long,
    val state: ReminderState,
)

@Entity(tableName = "export_bundles")
data class ExportBundleEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val fileUri: String,
    val checksum: String,
    val itemCount: Int,
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val title: String,
    val source: ShoppingListSource,
    val createdAt: Long,
    val sourceCaptureId: String?,
    val archived: Boolean,
)

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId")],
)
data class ShoppingListItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val label: String,
    val checked: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "routine_plans")
data class RoutinePlanEntity(
    @PrimaryKey val id: String,
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
    val notes: String,
    val active: Boolean,
    val completionCount: Int,
    val lastCompletedAt: Long?,
    val createdAt: Long,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val markdown: String,
    val createdAt: Long,
    val updatedAt: Long,
    val imported: Boolean,
    val sourceLabel: String?,
)
