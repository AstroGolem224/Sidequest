package com.astrogolem.sidequest.feature.missions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
        item {
            ScaffoldCard(
                title = "Mission Control",
                subtitle = if (missions.isEmpty()) {
                    "No missions yet. Capture or import something to let Sidequest build your first quest."
                } else {
                    "Your active queue of extracted work."
                },
            ) {
                Text(
                    if (missions.isEmpty()) {
                        "Start with the Capture tab, then review candidates in Inbox. Promoted items will appear here."
                    } else {
                        "Open, complete or snooze missions as they move through your day."
                    },
                )
            }
        }

        if (missions.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "Nothing in flight",
                    subtitle = "The dashboard is healthy, just empty.",
                ) {
                    Text("1. Capture a note, receipt or whiteboard.")
                    Text("2. Wait for background extraction to finish.")
                    Text("3. Promote a candidate from Inbox.")
                }
            }
        } else {
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
                        Button(onClick = { viewModel.snooze(mission.id, TimeUnit.HOURS.toMillis(1)) }) { Text("Snooze 1h") }
                    }
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
    var draftDescription by rememberSaveable { mutableStateOf("") }

    mission?.let { detail ->
        val dueCalendar = rememberCalendar(detail.dueAt)
        val reminderCalendar = rememberCalendar(detail.remindAt ?: detail.dueAt)
        val sourceCaptureId = detail.sourceCaptureId

        LaunchedEffect(detail.id, detail.description) {
            draftDescription = detail.description
        }

        fun openDateTimePicker(
            calendar: Calendar,
            onPicked: (Long) -> Unit,
        ) {
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
                            onPicked(picked.timeInMillis)
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
                    Text(
                        text = "Due: ${detail.dueAt?.let(::formatTimestamp) ?: "not set"}",
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = "Reminder: ${detail.remindAt?.let(::formatTimestamp) ?: "not set"}",
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Button(onClick = { openDateTimePicker(dueCalendar, viewModel::updateDueDate) }) { Text("Set due") }
                        Button(onClick = { openDateTimePicker(reminderCalendar, viewModel::updateReminderAt) }) { Text("Set reminder") }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Button(onClick = { viewModel.clearDueDate() }) { Text("Clear due") }
                        Button(onClick = { viewModel.clearReminderAt() }) { Text("Clear reminder") }
                    }
                }
            }

            item {
                ScaffoldCard(title = "Description", subtitle = "Edit the mission brief directly.") {
                    OutlinedTextField(
                        value = draftDescription,
                        onValueChange = { draftDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    Button(
                        onClick = { viewModel.updateDescription(draftDescription) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text("Save description")
                    }
                }
            }

            item {
                ScaffoldCard(title = "Priority", subtitle = "Use fixed presets to keep scoring readable.") {
                    PrioritySelector(
                        selected = detail.priorityScore,
                        onSelect = viewModel::updatePriority,
                    )
                }
            }

            item {
                ScaffoldCard(title = "Quick actions", subtitle = "Status and re-engagement controls.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.complete() }) { Text("Complete") }
                        Button(onClick = { viewModel.activate() }) { Text("Activate") }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Button(onClick = { viewModel.snooze(TimeUnit.MINUTES.toMillis(30)) }) { Text("Snooze 30m") }
                        Button(onClick = { viewModel.snooze(TimeUnit.HOURS.toMillis(2)) }) { Text("Snooze 2h") }
                        Button(onClick = { viewModel.snooze(TimeUnit.DAYS.toMillis(1)) }) { Text("Snooze 1d") }
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

@Composable
private fun PrioritySelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf(25 to "Low", 50 to "Normal", 75 to "High", 95 to "Critical")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (score, label) ->
                    Button(onClick = { onSelect(score) }) {
                        Text(if (selected == score) "$label *" else label)
                    }
                }
            }
        }
    }
}

private fun rememberCalendar(timestamp: Long?): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp ?: System.currentTimeMillis()
    }
}

private fun buildSubtitle(mission: MissionCardModel): String {
    return buildString {
        append("Priority ${mission.priorityScore} | ${mission.status.name.lowercase()}")
        mission.dueAt?.let { append(" | due ${formatTimestamp(it)}") }
        mission.remindAt?.let { append(" | remind ${formatTimestamp(it)}") }
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

    fun snooze(missionId: String, delayMillis: Long) {
        viewModelScope.launch {
            missionRepository.applyAction(
                MissionAction.Snooze(
                    missionId = missionId,
                    untilEpochMillis = System.currentTimeMillis() + delayMillis,
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

    fun updateReminderAt(timestamp: Long) {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.UpdateReminderAt(missionId, timestamp))
        }
    }

    fun clearReminderAt() {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.UpdateReminderAt(missionId, null))
        }
    }

    fun updateDescription(description: String) {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.UpdateDescription(missionId, description))
        }
    }

    fun updatePriority(priorityScore: Int) {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.UpdatePriority(missionId, priorityScore))
        }
    }

    fun complete() {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.Complete(missionId))
        }
    }

    fun activate() {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.Activate(missionId))
        }
    }

    fun snooze(delayMillis: Long) {
        viewModelScope.launch {
            missionRepository.applyAction(
                MissionAction.Snooze(
                    missionId = missionId,
                    untilEpochMillis = System.currentTimeMillis() + delayMillis,
                ),
            )
        }
    }
}
