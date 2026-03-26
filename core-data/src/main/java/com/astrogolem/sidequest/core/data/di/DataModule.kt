package com.astrogolem.sidequest.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.astrogolem.sidequest.core.data.local.CaptureDao
import com.astrogolem.sidequest.core.data.local.ExportBundleDao
import com.astrogolem.sidequest.core.data.local.MissionDao
import com.astrogolem.sidequest.core.data.local.NoteDao
import com.astrogolem.sidequest.core.data.local.ReminderDao
import com.astrogolem.sidequest.core.data.local.RoutinePlanDao
import com.astrogolem.sidequest.core.data.local.SearchDao
import com.astrogolem.sidequest.core.data.local.ShoppingListDao
import com.astrogolem.sidequest.core.data.local.SidequestDatabase
import com.astrogolem.sidequest.core.data.repo.ArchiveService
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.DefaultArchiveService
import com.astrogolem.sidequest.core.data.repo.DefaultCaptureRepository
import com.astrogolem.sidequest.core.data.repo.DefaultMissionRepository
import com.astrogolem.sidequest.core.data.repo.DefaultNotesRepository
import com.astrogolem.sidequest.core.data.repo.DefaultProcessingOrchestrator
import com.astrogolem.sidequest.core.data.repo.DefaultSearchRepository
import com.astrogolem.sidequest.core.data.repo.DefaultSecurityService
import com.astrogolem.sidequest.core.data.repo.DefaultRoutinePlanRepository
import com.astrogolem.sidequest.core.data.repo.DefaultShoppingListRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.NotesRepository
import com.astrogolem.sidequest.core.data.repo.ProcessingOrchestrator
import com.astrogolem.sidequest.core.data.repo.SearchRepository
import com.astrogolem.sidequest.core.data.repo.SecurityService
import com.astrogolem.sidequest.core.data.repo.RoutinePlanRepository
import com.astrogolem.sidequest.core.data.repo.ShoppingListRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SidequestDatabase {
        return Room.databaseBuilder(context, SidequestDatabase::class.java, "sidequest.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides fun provideCaptureDao(database: SidequestDatabase): CaptureDao = database.captureDao()
    @Provides fun provideMissionDao(database: SidequestDatabase): MissionDao = database.missionDao()
    @Provides fun provideSearchDao(database: SidequestDatabase): SearchDao = database.searchDao()
    @Provides fun provideReminderDao(database: SidequestDatabase): ReminderDao = database.reminderDao()
    @Provides fun provideExportBundleDao(database: SidequestDatabase): ExportBundleDao = database.exportBundleDao()
    @Provides fun provideShoppingListDao(database: SidequestDatabase): ShoppingListDao = database.shoppingListDao()
    @Provides fun provideRoutinePlanDao(database: SidequestDatabase): RoutinePlanDao = database.routinePlanDao()
    @Provides fun provideNoteDao(database: SidequestDatabase): NoteDao = database.noteDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindCaptureRepository(impl: DefaultCaptureRepository): CaptureRepository
    @Binds abstract fun bindProcessingOrchestrator(impl: DefaultProcessingOrchestrator): ProcessingOrchestrator
    @Binds abstract fun bindMissionRepository(impl: DefaultMissionRepository): MissionRepository
    @Binds abstract fun bindSearchRepository(impl: DefaultSearchRepository): SearchRepository
    @Binds abstract fun bindNotesRepository(impl: DefaultNotesRepository): NotesRepository
    @Binds abstract fun bindShoppingListRepository(impl: DefaultShoppingListRepository): ShoppingListRepository
    @Binds abstract fun bindRoutinePlanRepository(impl: DefaultRoutinePlanRepository): RoutinePlanRepository
    @Binds abstract fun bindArchiveService(impl: DefaultArchiveService): ArchiveService
    @Binds abstract fun bindSecurityService(impl: DefaultSecurityService): SecurityService
}
