package com.astrogolem.sidequest.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM captures WHERE id = :captureId LIMIT 1")
    fun observeCapture(captureId: String): Flow<CaptureEntity?>

    @Query("SELECT * FROM extracted_items WHERE status = :status ORDER BY confidence DESC")
    fun observeExtractedItems(status: String): Flow<List<ExtractedItemEntity>>

    @Query("SELECT * FROM extracted_items WHERE captureId = :captureId ORDER BY confidence DESC")
    fun observeExtractedItemsForCapture(captureId: String): Flow<List<ExtractedItemEntity>>

    @Query("SELECT * FROM extracted_items WHERE id = :candidateId LIMIT 1")
    suspend fun getExtractedItem(candidateId: String): ExtractedItemEntity?

    @Query("SELECT * FROM captures WHERE id = :captureId LIMIT 1")
    suspend fun getCapture(captureId: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE processingStatus = :status ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextPendingCapture(status: String = "PENDING"): CaptureEntity?

    @Query("UPDATE captures SET processingStatus = :status WHERE id = :captureId")
    suspend fun updateStatus(captureId: String, status: String)

    @Query("UPDATE extracted_items SET status = :status WHERE id = :candidateId")
    suspend fun updateExtractedStatus(candidateId: String, status: String)

    @Query("SELECT * FROM capture_analysis WHERE captureId = :captureId LIMIT 1")
    fun observeAnalysis(captureId: String): Flow<CaptureAnalysisEntity?>
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

    @Query("UPDATE missions SET status = :status WHERE id = :missionId")
    suspend fun updateMissionStatus(missionId: String, status: String)

    @Query("UPDATE missions SET dueAt = :dueAt WHERE id = :missionId")
    suspend fun updateMissionDueAt(missionId: String, dueAt: Long?)
}

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNode(entity: KnowledgeNodeEntity)

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
}
