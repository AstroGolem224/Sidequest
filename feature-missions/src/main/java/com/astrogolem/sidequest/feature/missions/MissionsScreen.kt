package com.astrogolem.sidequest.feature.missions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.MissionDetailModel
import com.astrogolem.sidequest.core.data.model.MissionStatus
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.components.StatusPill
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.CardSurfaceStrong
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.io.File
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
    val activeMissions = missions.filter { it.status.name == "OPEN" || it.status.name == "ACTIVE" }
    val completedCount = missions.count { it.status.name == "DONE" }
    val totalGp = missions.sumOf(::rewardGp).coerceAtLeast(125)
    val level = (totalGp / 180) + 1
    val xpCurrent = totalGp % 180
    val activeCount = activeMissions.size
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Level $level", style = MaterialTheme.typography.headlineLarge)
                        Text(
                            text = if (completedCount >= 20) "STELLAR NAVIGATOR" else "QUEST PILOT",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentPrimary,
                        )
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text("$xpCurrent/180 XP", style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
                        Text(
                            "$activeCount active | v${BuildConfig.SIDEQUEST_VERSION_LABEL}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                        .background(CardSurfaceStrong),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((xpCurrent / 180f).coerceIn(0.08f, 1f))
                            .height(6.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                            .background(AccentPrimary),
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text("ACTIVE SIDEQUESTS", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Surface(
                    color = AccentPrimary.copy(alpha = 0.12f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = "${activeCount.coerceAtLeast(missions.size)} ACTIVE",
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (missions.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "No quests deployed",
                    subtitle = "Capture physical context, review AI suggestions, then ship the ones that matter.",
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
                    subtitle = mission.description,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.Top,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                color = AccentPrimary.copy(alpha = 0.12f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                modifier = Modifier.size(44.dp),
                            ) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Icon(
                                        painter = painterResource(missionIconRes(mission)),
                                        contentDescription = mission.title,
                                        tint = AccentPrimary,
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill(text = missionTierLabel(mission), tone = HudTone.Amber)
                                StatusPill(text = urgencyLabel(mission), tone = urgencyTone(mission))
                            }
                        }
                        Text(
                            text = "${rewardXp(mission)} XP",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentSecondary,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 18.dp),
                    ) {
                        RewardChip(android.R.drawable.star_big_on, "${rewardXp(mission)} XP")
                        RewardChip(android.R.drawable.ic_menu_info_details, "${rewardGp(mission)} GP")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 18.dp),
                    ) {
                        Button(onClick = { onOpenMission(mission.id) }, modifier = Modifier.weight(1f)) {
                            Text("Open")
                        }
                        Button(onClick = { viewModel.complete(mission.id) }, modifier = Modifier.weight(1f)) {
                            Text("Complete")
                        }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MissionHero(detail = detail)
            }

            item {
                ScaffoldCard(title = "Briefing", subtitle = "Refine the quest text before execution.") {
                    OutlinedTextField(
                        value = draftDescription,
                        onValueChange = { draftDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { viewModel.updateDescription(draftDescription) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Save Brief")
                        }
                        if (sourceCaptureId != null) {
                            OutlinedButton(
                                onClick = { onOpenCapture(sourceCaptureId) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Evidence Window")
                            }
                        }
                    }
                }
            }

            item {
                SpoilsCard(detail = detail)
            }

            item {
                VitalStatsCard(
                    detail = detail,
                    onPickDueDate = { openDateTimePicker(dueCalendar, viewModel::updateDueDate) },
                    onPickReminder = { openDateTimePicker(reminderCalendar, viewModel::updateReminderAt) },
                    onClearDueDate = viewModel::clearDueDate,
                    onClearReminder = viewModel::clearReminderAt,
                    onSelectPriority = viewModel::updatePriority,
                )
            }

            item {
                CommandActionsCard(
                    detail = detail,
                    onComplete = viewModel::complete,
                    onActivate = viewModel::activate,
                    onArchive = viewModel::archive,
                    onSnoozeThirty = { viewModel.snooze(TimeUnit.MINUTES.toMillis(30)) },
                    onSnoozeTwoHours = { viewModel.snooze(TimeUnit.HOURS.toMillis(2)) },
                    onSnoozeOneDay = { viewModel.snooze(TimeUnit.DAYS.toMillis(1)) },
                    onOpenCapture = sourceCaptureId?.let { { onOpenCapture(it) } },
                )
            }
        }
    }
}

@Composable
private fun MissionHero(detail: MissionDetailModel) {
    Surface(
        color = BgGlow,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp),
        ) {
            detail.sourceImagePath?.let { sourceImagePath ->
                MissionSourceImage(
                    imagePath = sourceImagePath,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.82f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                StatusPill(
                    text = when (detail.status) {
                        MissionStatus.DONE -> "completed quest"
                        MissionStatus.ACTIVE -> "active quest"
                        MissionStatus.SNOOZED -> "snoozed quest"
                        MissionStatus.ARCHIVED -> "archived quest"
                        MissionStatus.OPEN -> "open quest"
                    },
                    tone = HudTone.Amber,
                )
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    text = buildSubtitle(
                        MissionCardModel(
                            id = detail.id,
                            title = detail.title,
                            description = detail.description,
                            priorityScore = detail.priorityScore,
                            status = detail.status,
                            dueAt = detail.dueAt,
                            remindAt = detail.remindAt,
                            sourceCaptureId = detail.sourceCaptureId,
                        ),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun MissionSourceImage(
    imagePath: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            imageView.setImageURI(Uri.fromFile(File(imagePath)))
        },
        modifier = modifier,
    )
}

@Composable
private fun SpoilsCard(detail: MissionDetailModel) {
    ScaffoldCard(
        title = "Potential Spoils",
        subtitle = "Rewards are deterministic so the game wrapper stays honest.",
    ) {
        RewardStrip(label = "Experience", value = "+${rewardXp(detail.toMissionCard())} XP", iconRes = android.R.drawable.ic_menu_compass)
        RewardStrip(
            label = "Gold Pieces",
            value = "+${rewardGp(detail.toMissionCard())} GP",
            iconRes = android.R.drawable.star_big_on,
            modifier = Modifier.padding(top = 10.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            color = AccentPrimary.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.16f)),
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Rare Bonus", style = MaterialTheme.typography.titleSmall, color = AccentPrimary)
                Text(
                    text = if (detail.priorityScore >= 85) "Critical-focus bonus (15%)" else "Steady progress bonus (5%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentSecondary,
                )
            }
        }
    }
}

@Composable
private fun RewardStrip(
    label: String,
    value: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BgGlow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(label, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            }
            Text(value, style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
        }
    }
}

@Composable
private fun VitalStatsCard(
    detail: MissionDetailModel,
    onPickDueDate: () -> Unit,
    onPickReminder: () -> Unit,
    onClearDueDate: () -> Unit,
    onClearReminder: () -> Unit,
    onSelectPriority: (Int) -> Unit,
) {
    ScaffoldCard(
        title = "Vital Stats",
        subtitle = "Timing, difficulty, and mission cost.",
    ) {
        val difficultyProgress = (detail.priorityScore / 100f).coerceIn(0.08f, 1f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Difficulty", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Text(priorityTier(detail.priorityScore), style = MaterialTheme.typography.titleSmall, color = AccentPrimary)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(CardSurfaceStrong),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(difficultyProgress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AccentPrimary),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "Duration",
                value = estimatedDuration(detail.priorityScore),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Energy Cost",
                value = "${(detail.priorityScore / 8).coerceAtLeast(4)} HP",
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "Due: ${detail.dueAt?.let(::formatTimestamp) ?: "not set"}",
            modifier = Modifier.padding(top = 16.dp),
            color = TextSecondary,
        )
        Text(
            text = "Reminder: ${detail.remindAt?.let(::formatTimestamp) ?: "not set"}",
            modifier = Modifier.padding(top = 4.dp),
            color = TextSecondary,
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onPickDueDate, modifier = Modifier.weight(1f)) { Text("Set Due") }
            Button(onClick = onPickReminder, modifier = Modifier.weight(1f)) { Text("Set Reminder") }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onClearDueDate, modifier = Modifier.weight(1f)) { Text("Clear Due") }
            OutlinedButton(onClick = onClearReminder, modifier = Modifier.weight(1f)) { Text("Clear Reminder") }
        }
        Text(
            text = "Priority Presets",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        PrioritySelector(
            selected = detail.priorityScore,
            onSelect = onSelectPriority,
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = BgGlow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = AccentSecondary)
        }
    }
}

@Composable
private fun CommandActionsCard(
    detail: MissionDetailModel,
    onComplete: () -> Unit,
    onActivate: () -> Unit,
    onArchive: () -> Unit,
    onSnoozeThirty: () -> Unit,
    onSnoozeTwoHours: () -> Unit,
    onSnoozeOneDay: () -> Unit,
    onOpenCapture: (() -> Unit)?,
) {
    ScaffoldCard(
        title = "Command Actions",
        subtitle = "Deploy, pause, or drop the quest without losing control over source intel.",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onComplete, modifier = Modifier.weight(1f)) { Text("Complete Quest") }
            OutlinedButton(onClick = if (detail.status == MissionStatus.ACTIVE) onArchive else onActivate, modifier = Modifier.weight(1f)) {
                Text(if (detail.status == MissionStatus.ACTIVE) "Forfeit Quest" else "Activate")
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onSnoozeThirty, modifier = Modifier.weight(1f)) { Text("30m") }
            OutlinedButton(onClick = onSnoozeTwoHours, modifier = Modifier.weight(1f)) { Text("2h") }
            OutlinedButton(onClick = onSnoozeOneDay, modifier = Modifier.weight(1f)) { Text("1d") }
        }
        if (onOpenCapture != null) {
            Button(
                onClick = onOpenCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Text("Open Source Intel")
            }
        }
    }
}

private fun MissionDetailModel.toMissionCard(): MissionCardModel {
    return MissionCardModel(
        id = id,
        title = title,
        description = description,
        priorityScore = priorityScore,
        status = status,
        dueAt = dueAt,
        remindAt = remindAt,
        sourceCaptureId = sourceCaptureId,
    )
}

private fun priorityTier(priorityScore: Int): String {
    return when {
        priorityScore >= 90 -> "Veteran"
        priorityScore >= 70 -> "Elite"
        priorityScore >= 45 -> "Standard"
        else -> "Scout"
    }
}

private fun estimatedDuration(priorityScore: Int): String {
    return when {
        priorityScore >= 90 -> "45 mins"
        priorityScore >= 70 -> "30 mins"
        priorityScore >= 45 -> "20 mins"
        else -> "10 mins"
    }
}

@Composable
private fun RewardChip(
    iconRes: Int,
    value: String,
) {
    Surface(
        color = BgGlow,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = value,
                tint = AccentPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(value, style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
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

private fun rewardXp(mission: MissionCardModel): Int {
    return (mission.priorityScore * 2).coerceAtLeast(50)
}

private fun rewardGp(mission: MissionCardModel): Int {
    return (mission.priorityScore / 4).coerceAtLeast(10)
}

private fun missionTierLabel(mission: MissionCardModel): String {
    return when {
        mission.priorityScore >= 90 -> "HARD"
        mission.priorityScore >= 70 -> "TIER II"
        else -> "TIER I"
    }
}

private fun missionIconRes(mission: MissionCardModel): Int {
    val title = mission.title.lowercase()
    return when {
        "clean" in title || "desk" in title || "kitchen" in title -> android.R.drawable.ic_menu_delete
        "read" in title || "book" in title || "study" in title -> android.R.drawable.ic_menu_edit
        "run" in title || "walk" in title || "fit" in title -> android.R.drawable.ic_media_play
        else -> android.R.drawable.ic_menu_compass
    }
}

private fun urgencyLabel(mission: MissionCardModel): String {
    val dueAt = mission.dueAt ?: return "open window"
    val remaining = dueAt - System.currentTimeMillis()
    return when {
        remaining <= 0L -> "time to failure"
        remaining <= TimeUnit.HOURS.toMillis(6) -> "critical window"
        remaining <= TimeUnit.DAYS.toMillis(1) -> "today"
        else -> "queued"
    }
}

private fun urgencyTone(mission: MissionCardModel): HudTone {
    val dueAt = mission.dueAt ?: return HudTone.Cyan
    val remaining = dueAt - System.currentTimeMillis()
    return when {
        remaining <= TimeUnit.HOURS.toMillis(6) -> HudTone.Amber
        remaining <= TimeUnit.DAYS.toMillis(1) -> HudTone.Violet
        else -> HudTone.Cyan
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

    fun archive() {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.Archive(missionId))
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
