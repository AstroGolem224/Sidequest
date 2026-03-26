package com.astrogolem.sidequest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astrogolem.sidequest.core.data.model.ShoppingListSource
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCapture(entity: CaptureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalysis(entity: CaptureAnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExtractedItems(items: List<ExtractedItemEntity>)

    @Query("SELECT * FROM captures ORDER BY createdAt DESC")
    fun observeCaptures(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures ORDER BY createdAt DESC")
    suspend fun listCaptures(): List<CaptureEntity>

    @Query("SELECT * FROM captures WHERE id = :captureId LIMIT 1")
    fun observeCapture(captureId: String): Flow<CaptureEntity?>

    @Query("SELECT * FROM extracted_items WHERE status = :status ORDER BY confidence DESC")
    fun observeExtractedItems(status: String): Flow<List<ExtractedItemEntity>>

    @Query("SELECT * FROM extracted_items WHERE captureId = :captureId ORDER BY confidence DESC")
    fun observeExtractedItemsForCapture(captureId: String): Flow<List<ExtractedItemEntity>>

    @Query("SELECT * FROM extracted_items ORDER BY confidence DESC")
    suspend fun listExtractedItems(): List<ExtractedItemEntity>

    @Query("SELECT * FROM extracted_items WHERE id = :candidateId LIMIT 1")
    suspend fun getExtractedItem(candidateId: String): ExtractedItemEntity?

    @Query("SELECT * FROM captures WHERE id = :captureId LIMIT 1")
    suspend fun getCapture(captureId: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE processingStatus = :status ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextPendingCapture(status: String = "PENDING"): CaptureEntity?

    @Query(
        """
        SELECT id
        FROM captures
        WHERE processingStatus IN (:recoverableStatuses)
        ORDER BY createdAt ASC
        LIMIT :limit
        """,
    )
    suspend fun listRecoverableCaptureIds(
        recoverableStatuses: List<String> = listOf("PENDING", "PROCESSING"),
        limit: Int = 20,
    ): List<String>

    @Query("UPDATE captures SET processingStatus = :status WHERE id = :captureId")
    suspend fun updateStatus(captureId: String, status: String)

    @Query("UPDATE captures SET retryCount = :retryCount WHERE id = :captureId")
    suspend fun updateRetryCount(captureId: String, retryCount: Int)

    @Query("UPDATE extracted_items SET status = :status WHERE id = :candidateId")
    suspend fun updateExtractedStatus(candidateId: String, status: String)

    @Query("SELECT * FROM capture_analysis WHERE captureId = :captureId LIMIT 1")
    fun observeAnalysis(captureId: String): Flow<CaptureAnalysisEntity?>

    @Query("SELECT * FROM capture_analysis ORDER BY processedAt DESC")
    suspend fun listAnalyses(): List<CaptureAnalysisEntity>

    @Query("DELETE FROM extracted_items WHERE captureId = :captureId")
    suspend fun clearExtractedItemsForCapture(captureId: String)

    @Query("DELETE FROM capture_analysis WHERE captureId = :captureId")
    suspend fun clearAnalysisForCapture(captureId: String)

    @Query("DELETE FROM extracted_items")
    suspend fun clearExtractedItems()

    @Query("DELETE FROM capture_analysis")
    suspend fun clearAnalyses()

    @Query("DELETE FROM captures")
    suspend fun clearCaptures()

    @Query("DELETE FROM captures WHERE id = :captureId")
    suspend fun deleteCapture(captureId: String)
}

@Dao
interface MissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMission(entity: MissionEntity)

    @Query("SELECT * FROM missions ORDER BY priorityScore DESC, createdAt DESC")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :missionId LIMIT 1")
    fun observeMission(missionId: String): Flow<MissionEntity?>

    @Query("SELECT * FROM missions WHERE sourceCaptureId = :captureId ORDER BY createdAt DESC")
    fun observeMissionsForCapture(captureId: String): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE id = :missionId LIMIT 1")
    suspend fun getMission(missionId: String): MissionEntity?

    @Query("SELECT * FROM missions ORDER BY createdAt DESC")
    suspend fun listMissions(): List<MissionEntity>

    @Query("UPDATE missions SET status = :status WHERE id = :missionId")
    suspend fun updateMissionStatus(missionId: String, status: String)

    @Query("UPDATE missions SET dueAt = :dueAt WHERE id = :missionId")
    suspend fun updateMissionDueAt(missionId: String, dueAt: Long?)

    @Query("UPDATE missions SET remindAt = :remindAt WHERE id = :missionId")
    suspend fun updateMissionRemindAt(missionId: String, remindAt: Long?)

    @Query("UPDATE missions SET description = :description WHERE id = :missionId")
    suspend fun updateMissionDescription(missionId: String, description: String)

    @Query("UPDATE missions SET priorityScore = :priorityScore WHERE id = :missionId")
    suspend fun updateMissionPriority(missionId: String, priorityScore: Int)

    @Query("DELETE FROM missions")
    suspend fun clearMissions()

    @Query("UPDATE missions SET sourceCaptureId = NULL WHERE sourceCaptureId = :captureId")
    suspend fun detachSourceCapture(captureId: String)
}

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNode(entity: KnowledgeNodeEntity)

    @Query("SELECT * FROM knowledge_nodes ORDER BY rowid DESC")
    suspend fun listNodes(): List<KnowledgeNodeEntity>

    @Query(
        """
        SELECT knowledge_nodes.*
        FROM knowledge_nodes
        JOIN knowledge_nodes_fts ON knowledge_nodes.rowid = knowledge_nodes_fts.rowid
        WHERE knowledge_nodes_fts MATCH :query
        ORDER BY bm25(knowledge_nodes_fts)
        """,
    )
    suspend fun search(query: String): List<KnowledgeNodeEntity>

    @Query("DELETE FROM knowledge_nodes")
    suspend fun clearNodes()

    @Query("DELETE FROM knowledge_nodes WHERE captureId = :captureId")
    suspend fun deleteNodesForCapture(captureId: String)
}

@Dao
interface ExportBundleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBundle(entity: ExportBundleEntity)
}

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(entity: ReminderEntity)

    @Query("UPDATE reminders SET state = :state WHERE missionId = :missionId")
    suspend fun updateReminderStateForMission(missionId: String, state: String)

    @Query("SELECT * FROM reminders ORDER BY scheduledAt DESC")
    suspend fun listReminders(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()
}

data class ShoppingListWithCount(
    val id: String,
    val title: String,
    val source: ShoppingListSource,
    val createdAt: Long,
    val sourceCaptureId: String?,
    val archived: Boolean,
    val itemCount: Int,
    val checkedCount: Int,
)

data class ShoppingListWithItems(
    val id: String,
    val title: String,
    val source: ShoppingListSource,
    val createdAt: Long,
    val sourceCaptureId: String?,
    val archived: Boolean,
    val itemId: String,
    val itemLabel: String,
    val itemChecked: Boolean,
    val itemSortOrder: Int,
)

@Dao
interface ShoppingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(entity: ShoppingListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ShoppingListItemEntity>)

    @Query(
        """
        SELECT shopping_lists.id,
               shopping_lists.title,
               shopping_lists.source,
               shopping_lists.createdAt,
               shopping_lists.sourceCaptureId,
               shopping_lists.archived,
               COUNT(shopping_list_items.id) AS itemCount,
               SUM(CASE WHEN shopping_list_items.checked THEN 1 ELSE 0 END) AS checkedCount
        FROM shopping_lists
        LEFT JOIN shopping_list_items ON shopping_list_items.listId = shopping_lists.id
        GROUP BY shopping_lists.id
        ORDER BY shopping_lists.archived ASC, shopping_lists.createdAt DESC
        """,
    )
    fun observeLists(): Flow<List<ShoppingListWithCount>>

    @Query(
        """
        SELECT shopping_lists.id,
               shopping_lists.title,
               shopping_lists.source,
               shopping_lists.createdAt,
               shopping_lists.sourceCaptureId,
               shopping_lists.archived,
               shopping_list_items.id AS itemId,
               shopping_list_items.label AS itemLabel,
               shopping_list_items.checked AS itemChecked,
               shopping_list_items.sortOrder AS itemSortOrder
        FROM shopping_lists
        JOIN shopping_list_items ON shopping_list_items.listId = shopping_lists.id
        WHERE shopping_lists.id = :listId
        ORDER BY shopping_list_items.sortOrder ASC, shopping_list_items.id ASC
        """,
    )
    fun observeList(listId: String): Flow<List<ShoppingListWithItems>>

    @Query("UPDATE shopping_list_items SET checked = :checked WHERE id = :itemId")
    suspend fun updateItemChecked(itemId: String, checked: Boolean)

    @Query("UPDATE shopping_lists SET title = :title WHERE id = :listId")
    suspend fun updateListTitle(listId: String, title: String)

    @Query("UPDATE shopping_lists SET archived = :archived WHERE id = :listId")
    suspend fun updateListArchived(listId: String, archived: Boolean)

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteList(listId: String)

    @Query("DELETE FROM shopping_list_items WHERE listId = :listId")
    suspend fun clearItemsForList(listId: String)

    @Query("DELETE FROM shopping_list_items")
    suspend fun clearItems()

    @Query("DELETE FROM shopping_lists")
    suspend fun clearLists()
}

@Dao
interface RoutinePlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(entity: RoutinePlanEntity)

    @Query("SELECT * FROM routine_plans ORDER BY active DESC, hour ASC, minute ASC, createdAt DESC")
    fun observePlans(): Flow<List<RoutinePlanEntity>>

    @Query("SELECT * FROM routine_plans ORDER BY active DESC, hour ASC, minute ASC, createdAt DESC")
    suspend fun listPlans(): List<RoutinePlanEntity>

    @Query("SELECT * FROM routine_plans WHERE id = :planId LIMIT 1")
    fun observePlan(planId: String): Flow<RoutinePlanEntity?>

    @Query("UPDATE routine_plans SET active = :active WHERE id = :planId")
    suspend fun updateActive(planId: String, active: Boolean)

    @Query("UPDATE routine_plans SET completionCount = completionCount + 1, lastCompletedAt = :completedAt WHERE id = :planId")
    suspend fun markCompleted(planId: String, completedAt: Long)

    @Query("DELETE FROM routine_plans WHERE id = :planId")
    suspend fun deletePlan(planId: String)

    @Query("DELETE FROM routine_plans")
    suspend fun clearPlans()
}

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(entity: NoteEntity)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC, createdAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    fun observeNote(noteId: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC, createdAt DESC")
    suspend fun listNotes(): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("DELETE FROM notes")
    suspend fun clearNotes()
}
