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

    @Query("SELECT * FROM extracted_items WHERE status = :status ORDER BY confidence DESC")
    fun observeExtractedItems(status: String): Flow<List<ExtractedItemEntity>>

    @Query("SELECT * FROM extracted_items WHERE id = :candidateId LIMIT 1")
    suspend fun getExtractedItem(candidateId: String): ExtractedItemEntity?

    @Query("SELECT * FROM captures WHERE processingStatus = :status ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextPendingCapture(status: String = "PENDING"): CaptureEntity?

    @Query("UPDATE captures SET processingStatus = :status WHERE id = :captureId")
    suspend fun updateStatus(captureId: String, status: String)

    @Query("UPDATE extracted_items SET status = :status WHERE id = :candidateId")
    suspend fun updateExtractedStatus(candidateId: String, status: String)
}

@Dao
interface MissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMission(entity: MissionEntity)

    @Query("SELECT * FROM missions ORDER BY priorityScore DESC, createdAt DESC")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("UPDATE missions SET status = :status WHERE id = :missionId")
    suspend fun updateMissionStatus(missionId: String, status: String)
}

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNode(entity: KnowledgeNodeEntity)

    @Query(
        """
        SELECT * FROM knowledge_nodes
        WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%'
        ORDER BY id DESC
        """,
    )
    suspend fun search(query: String): List<KnowledgeNodeEntity>
}

@Dao
interface ExportBundleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBundle(entity: ExportBundleEntity)
}
