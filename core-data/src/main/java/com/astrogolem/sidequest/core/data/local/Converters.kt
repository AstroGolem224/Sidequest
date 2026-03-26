package com.astrogolem.sidequest.core.data.local

import androidx.room.TypeConverter
import com.astrogolem.sidequest.core.data.model.CaptureProcessingStatus
import com.astrogolem.sidequest.core.data.model.DocumentType
import com.astrogolem.sidequest.core.data.model.ExtractionKind
import com.astrogolem.sidequest.core.data.model.ExtractionStatus
import com.astrogolem.sidequest.core.data.model.MissionStatus
import com.astrogolem.sidequest.core.data.model.ReminderState

class Converters {
    @TypeConverter fun fromDocumentType(value: DocumentType): String = value.name
    @TypeConverter fun toDocumentType(value: String): DocumentType = DocumentType.valueOf(value)
    @TypeConverter fun fromCaptureStatus(value: CaptureProcessingStatus): String = value.name
    @TypeConverter fun toCaptureStatus(value: String): CaptureProcessingStatus = CaptureProcessingStatus.valueOf(value)
    @TypeConverter fun fromExtractionStatus(value: ExtractionStatus): String = value.name
    @TypeConverter fun toExtractionStatus(value: String): ExtractionStatus = ExtractionStatus.valueOf(value)
    @TypeConverter fun fromExtractionKind(value: ExtractionKind): String = value.name
    @TypeConverter fun toExtractionKind(value: String): ExtractionKind = ExtractionKind.valueOf(value)
    @TypeConverter fun fromMissionStatus(value: MissionStatus): String = value.name
    @TypeConverter fun toMissionStatus(value: String): MissionStatus = MissionStatus.valueOf(value)
    @TypeConverter fun fromReminderState(value: ReminderState): String = value.name
    @TypeConverter fun toReminderState(value: String): ReminderState = ReminderState.valueOf(value)
}
