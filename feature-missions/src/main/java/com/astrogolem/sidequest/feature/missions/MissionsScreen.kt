package com.astrogolem.sidequest.feature.missions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.MissionDetailModel
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun MissionsRoute(
    onOpenMission: (String) -> Unit,
    viewModel: MissionsViewModel = hiltViewModel(),
) {
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(missions, key = { it.id }) { mission ->
            ScaffoldCard(
                title = mission.title,
                subtitle = buildSubtitle(mission),
            ) {
                Text(mission.description)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Button(onClick = { onOpenMission(mission.id) }) { Text("Open") }
                    Button(onClick = { viewModel.complete(mission.id) }) { Text("Complete") }
                    Button(onClick = { viewModel.remindInOneHour(mission.id) }) { Text("Remind 1h") }
                }
            }
        }
    }
}

@Composable
fun MissionDetailRoute(
    onOpenCapture: (String) -> Unit,
    viewModel: MissionDetailViewModel = hiltViewModel(),
) {
    val mission by viewModel.mission.collectAsStateWithLifecycle()
    val context = LocalContext.current

    mission?.let { detail ->
        val dueAt = detail.dueAt
        val sourceCaptureId = detail.sourceCaptureId
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dueAt ?: System.currentTimeMillis()
        }

        fun openDueDatePicker() {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            picked.set(Calendar.HOUR_OF_DAY, hour)
                            picked.set(Calendar.MINUTE, minute)
                            picked.set(Calendar.SECOND, 0)
                            viewModel.updateDueDate(picked.timeInMillis)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true,
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            ).show()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ScaffoldCard(
                    title = detail.title,
                    subtitle = "Priority ${detail.priorityScore} | ${detail.status.name.lowercase()}",
                ) {
                    Text(detail.description)
                    Text(
                        text = "Due: ${dueAt?.let(::formatTimestamp) ?: "not set"}",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Button(onClick = ::openDueDatePicker) { Text("Set due date") }
                        Button(onClick = { viewModel.clearDueDate() }) { Text("Clear due") }
                    }
                }
            }

            item {
                ScaffoldCard(title = "Actions", subtitle = "Reminder and completion controls.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.complete() }) { Text("Complete") }
                        Button(onClick = { viewModel.remindInOneHour() }) { Text("Remind 1h") }
                    }
                    if (sourceCaptureId != null) {
                        Button(
                            onClick = { onOpenCapture(sourceCaptureId) },
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Text("Open source capture")
                        }
                    }
                }
            }
        }
    }
}

private fun buildSubtitle(mission: MissionCardModel): String {
    val dueAt = mission.dueAt
    return buildString {
        append("Priority ${mission.priorityScore} | ${mission.status.name.lowercase()}")
        if (dueAt != null) {
            append(" | ${formatTimestamp(dueAt)}")
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}

@HiltViewModel
class MissionsViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
) : ViewModel() {
    val missions: StateFlow<List<MissionCardModel>> =
        missionRepository.observeMissions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun complete(missionId: String) {
        viewModelScope.launch { missionRepository.applyAction(MissionAction.Complete(missionId)) }
    }

    fun remindInOneHour(missionId: String) {
        viewModelScope.launch {
            missionRepository.applyAction(
                MissionAction.Snooze(
                    missionId = missionId,
                    untilEpochMillis = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                ),
            )
        }
    }
}

@HiltViewModel
class MissionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val missionRepository: MissionRepository,
) : ViewModel() {
    private val missionId: String = checkNotNull(savedStateHandle["missionId"])

    val mission: StateFlow<MissionDetailModel?> =
        missionRepository.observeMission(missionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateDueDate(timestamp: Long) {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.UpdateDueDate(missionId, timestamp))
        }
    }

    fun clearDueDate() {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.UpdateDueDate(missionId, null))
        }
    }

    fun complete() {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.Complete(missionId))
        }
    }

    fun remindInOneHour() {
        viewModelScope.launch {
            missionRepository.applyAction(
                MissionAction.Snooze(
                    missionId = missionId,
                    untilEpochMillis = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                ),
            )
        }
    }
}
