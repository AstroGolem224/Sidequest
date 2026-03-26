package com.astrogolem.sidequest.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CaptureEntity::class,
        CaptureAnalysisEntity::class,
        ExtractedItemEntity::class,
        MissionEntity::class,
        ProjectEntity::class,
        LifeAreaEntity::class,
        KnowledgeNodeEntity::class,
        KnowledgeNodeFtsEntity::class,
        KnowledgeEdgeEntity::class,
        ReminderEntity::class,
        ExportBundleEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        RoutinePlanEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SidequestDatabase : RoomDatabase() {
    abstract fun captureDao(): CaptureDao
    abstract fun missionDao(): MissionDao
    abstract fun searchDao(): SearchDao
    abstract fun reminderDao(): ReminderDao
    abstract fun exportBundleDao(): ExportBundleDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun routinePlanDao(): RoutinePlanDao
}
