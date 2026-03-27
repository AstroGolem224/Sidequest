package com.astrogolem.sidequest.feature.missions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.astrogolem.sidequest.core.ui.components.DestructiveOutlinedButton
import com.astrogolem.sidequest.core.ui.components.GlassCard
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.components.SplitActionRow
import com.astrogolem.sidequest.core.ui.components.StateShellCard
import com.astrogolem.sidequest.core.ui.components.StatusPill
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.CardSurfaceStrong
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.SidequestSpacing
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun MissionsRoute(
    onOpenMission: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
    viewModel: MissionsViewModel = hiltViewModel(),
) {
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val completionReward by viewModel.completionReward.collectAsStateWithLifecycle()
    val activeMissions = missions.filter { it.status.name == "OPEN" || it.status.name == "ACTIVE" }
    val completedCount = missions.count { it.status.name == "DONE" }
    val totalGp = missions.sumOf(::rewardGp).coerceAtLeast(125)
    val level = (totalGp / 180) + 1
    val xpCurrent = totalGp % 180
    val activeCount = activeMissions.size
    var abandonMissionId by rememberSaveable { mutableStateOf<String?>(null) }
    var abandonMissionTitle by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(completionReward?.nonce) {
        if (completionReward != null) {
            delay(2200)
            viewModel.clearCompletionReward()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            Text(
                                stringResource(R.string.missions_hero_level_format, level),
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            Text(
                                text = stringResource(
                                    if (completedCount >= 20) {
                                        R.string.missions_hero_mode_steady
                                    } else {
                                        R.string.missions_hero_mode_active
                                    },
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = AccentPrimary,
                            )
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(
                                stringResource(R.string.missions_hero_xp_format, xpCurrent),
                                style = MaterialTheme.typography.titleSmall,
                                color = AccentSecondary,
                            )
                            Text(
                                stringResource(R.string.missions_hero_active_version_format, activeCount, BuildConfig.SIDEQUEST_VERSION_LABEL),
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
                    Text(
                        stringResource(R.string.missions_active_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                    )
                    Surface(
                        color = AccentPrimary.copy(alpha = 0.12f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.missions_active_badge_format, activeCount),
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            if (activeMissions.isEmpty()) {
                item {
                    StateShellCard(
                        title = stringResource(R.string.missions_empty_title),
                        subtitle = stringResource(R.string.missions_empty_subtitle),
                        supportingLines = listOf(
                            stringResource(R.string.missions_empty_step_capture),
                            stringResource(R.string.missions_empty_step_wait),
                            stringResource(R.string.missions_empty_step_promote),
                        ),
                    )
                }
            } else {
                items(activeMissions, key = { it.id }) { mission ->
                    MissionBoardCard(
                        cardKey = mission.id,
                        title = mission.title,
                        subtitle = buildBoardStatusLine(mission),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.Top,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    color = AccentPrimary.copy(alpha = 0.12f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                        Icon(
                                            imageVector = missionIconRes(mission),
                                            contentDescription = mission.title,
                                            tint = AccentPrimary,
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusPill(text = missionTierLabel(mission), tone = HudTone.Amber)
                                        StatusPill(text = urgencyLabel(mission), tone = urgencyTone(mission))
                                    }
                                    mission.description.takeIf { it.isNotBlank() }?.let { description ->
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${rewardXp(mission)} XP",
                                style = MaterialTheme.typography.titleSmall,
                                color = AccentSecondary,
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            BoardSignalTile(
                                label = stringResource(R.string.missions_board_due_label),
                                value = boardDueLabel(mission),
                                modifier = Modifier.weight(1f),
                            )
                            BoardSignalTile(
                                label = stringResource(R.string.missions_board_reminder_label),
                                value = boardReminderLabel(mission),
                                modifier = Modifier.weight(1f),
                            )
                            BoardSignalTile(
                                label = stringResource(R.string.missions_board_next_label),
                                value = stringResource(boardPrimaryActionLabelRes(mission)),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 18.dp),
                        ) {
                            RewardChip(SidequestIcons.Spark, "${rewardXp(mission)} XP")
                            RewardChip(SidequestIcons.Coin, "${rewardGp(mission)} GP")
                        }
                        SplitActionRow(modifier = Modifier.padding(top = SidequestSpacing.CardContentGap)) {
                            Button(onClick = { onOpenMission(mission.id) }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.missions_board_open))
                            }
                            Button(
                                onClick = {
                                    if (mission.status == MissionStatus.ACTIVE) {
                                        viewModel.complete(mission.id)
                                    } else {
                                        viewModel.activate(mission.id)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(boardPrimaryActionLabelRes(mission)))
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = SidequestSpacing.ControlGap),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            mission.sourceCaptureId?.let { captureId ->
                                OutlinedButton(onClick = { onOpenCapture(captureId) }) {
                                    Icon(
                                        imageVector = SidequestIcons.Intel,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.missions_board_source_intel),
                                        modifier = Modifier.padding(start = SidequestSpacing.Xs),
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    abandonMissionId = mission.id
                                    abandonMissionTitle = mission.title
                                },
                            ) {
                                Text(stringResource(R.string.missions_board_abandon_later))
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = completionReward != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .padding(top = 12.dp),
        ) {
            completionReward?.let { reward ->
                CompletionRewardBanner(reward = reward)
            }
        }
    }

    if (abandonMissionId != null) {
        val abandonTitle = abandonMissionTitle ?: stringResource(R.string.missions_abandon_dialog_fallback_title)
        AlertDialog(
            onDismissRequest = {
                abandonMissionId = null
                abandonMissionTitle = null
            },
            title = { Text(stringResource(R.string.missions_abandon_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.missions_abandon_dialog_body_format, abandonTitle),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        abandonMissionId?.let(viewModel::abandon)
                        abandonMissionId = null
                        abandonMissionTitle = null
                    },
                ) {
                    Text(stringResource(R.string.missions_abandon_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        abandonMissionId = null
                        abandonMissionTitle = null
                    },
                ) {
                    Text(stringResource(R.string.missions_abandon_dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun MissionBoardCard(
    title: String,
    cardKey: String = title,
    subtitle: String? = null,
    collapsible: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(cardKey) { mutableStateOf(!collapsible) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.72f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = collapsible) { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (collapsible) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) SidequestIcons.Minus else SidequestIcons.Plus,
                            contentDescription = if (expanded) "Collapse quest" else "Expand quest",
                            tint = AccentSecondary,
                        )
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                        )
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun BoardSignalTile(
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
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
    var showAbandonDialog by rememberSaveable { mutableStateOf(false) }

    mission?.let { detail ->
        val dueCalendar = rememberCalendar(detail.dueAt)
        val reminderCalendar = rememberCalendar(detail.remindAt ?: detail.dueAt)
        val sourceCaptureId = detail.sourceCaptureId
        val primaryActionLabelRes = missionDetailPrimaryActionLabelRes(detail.status)

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
                MissionPrimaryActionCard(
                    detail = detail,
                    primaryActionLabelRes = primaryActionLabelRes,
                    onPrimaryAction = {
                        when (detail.status) {
                            MissionStatus.ACTIVE -> viewModel.complete()
                            MissionStatus.OPEN,
                            MissionStatus.SNOOZED,
                            MissionStatus.ARCHIVED,
                            -> viewModel.activate()
                            MissionStatus.DONE -> Unit
                        }
                    },
                )
            }

            item {
                MissionBriefingCard(
                    draftDescription = draftDescription,
                    onDescriptionChange = { draftDescription = it },
                    onSave = { viewModel.updateDescription(draftDescription) },
                )
            }

            item {
                MissionSchedulingCard(
                    detail = detail,
                    onPickDueDate = { openDateTimePicker(dueCalendar, viewModel::updateDueDate) },
                    onPickReminder = { openDateTimePicker(reminderCalendar, viewModel::updateReminderAt) },
                    onClearDueDate = viewModel::clearDueDate,
                    onClearReminder = viewModel::clearReminderAt,
                    onSelectPriority = viewModel::updatePriority,
                    onSnoozeThirty = { viewModel.snooze(TimeUnit.MINUTES.toMillis(30)) },
                    onSnoozeTwoHours = { viewModel.snooze(TimeUnit.HOURS.toMillis(2)) },
                    onSnoozeOneDay = { viewModel.snooze(TimeUnit.DAYS.toMillis(1)) },
                )
            }

            if (sourceCaptureId != null) {
                item {
                    MissionIntelLinkCard(
                        onOpenCapture = { onOpenCapture(sourceCaptureId) },
                    )
                }
            }

            item {
                SpoilsCard(detail = detail)
            }

            item {
                MissionDangerZoneCard(
                    onAbandon = { showAbandonDialog = true },
                )
            }
        }

        if (showAbandonDialog) {
            AlertDialog(
                onDismissRequest = { showAbandonDialog = false },
                title = { Text(stringResource(R.string.missions_detail_abandon_title)) },
                text = {
                    Text(stringResource(R.string.missions_detail_abandon_body_format, detail.title))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAbandonDialog = false
                            viewModel.abandon()
                        },
                    ) {
                        Text(stringResource(R.string.missions_detail_abandon_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAbandonDialog = false }) {
                        Text(stringResource(R.string.missions_detail_abandon_cancel))
                    }
                },
            )
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
                    text = stringResource(missionStatusLabelRes(detail.status)),
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
private fun MissionPrimaryActionCard(
    detail: MissionDetailModel,
    primaryActionLabelRes: Int?,
    onPrimaryAction: () -> Unit,
) {
    GlassCard(
        title = stringResource(R.string.missions_detail_primary_title),
        subtitle = stringResource(R.string.missions_detail_primary_subtitle),
    ) {
        Text(
            text = stringResource(missionDetailPrimaryBodyRes(detail.status)),
            color = TextSecondary,
        )
        primaryActionLabelRes?.let { actionLabel ->
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(top = SidequestSpacing.ItemGap),
            ) {
                Text(stringResource(actionLabel))
            }
        }
    }
}

@Composable
private fun MissionBriefingCard(
    draftDescription: String,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    GlassCard(
        title = stringResource(R.string.missions_detail_briefing_title),
        subtitle = stringResource(R.string.missions_detail_briefing_subtitle),
    ) {
        OutlinedTextField(
            value = draftDescription,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(top = SidequestSpacing.ItemGap),
        ) {
            Text(stringResource(R.string.missions_detail_briefing_save))
        }
    }
}

@Composable
private fun MissionSourceImage(
    imagePath: String,
    modifier: Modifier = Modifier,
) {
    val imageUri = remember(imagePath) {
        val parsed = Uri.parse(imagePath)
        if (parsed.scheme.isNullOrBlank()) Uri.fromFile(File(imagePath)) else parsed
    }
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            imageView.setImageURI(imageUri)
        },
        modifier = modifier,
    )
}

@Composable
private fun SpoilsCard(detail: MissionDetailModel) {
    GlassCard(
        title = stringResource(R.string.missions_spoils_title),
        subtitle = stringResource(R.string.missions_spoils_subtitle),
    ) {
        RewardStrip(
            label = stringResource(R.string.missions_spoils_experience),
            value = "+${rewardXp(detail.toMissionCard())} XP",
            icon = SidequestIcons.Spark,
        )
        RewardStrip(
            label = stringResource(R.string.missions_spoils_gold),
            value = "+${rewardGp(detail.toMissionCard())} GP",
            icon = SidequestIcons.Coin,
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
                Text(
                    stringResource(R.string.missions_spoils_bonus_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentPrimary,
                )
                Text(
                    text = stringResource(
                        if (detail.priorityScore >= 85) {
                            R.string.missions_spoils_bonus_critical
                        } else {
                            R.string.missions_spoils_bonus_steady
                        },
                    ),
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
    icon: ImageVector,
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
                    imageVector = icon,
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
private fun MissionSchedulingCard(
    detail: MissionDetailModel,
    onPickDueDate: () -> Unit,
    onPickReminder: () -> Unit,
    onClearDueDate: () -> Unit,
    onClearReminder: () -> Unit,
    onSelectPriority: (Int) -> Unit,
    onSnoozeThirty: () -> Unit,
    onSnoozeTwoHours: () -> Unit,
    onSnoozeOneDay: () -> Unit,
) {
    GlassCard(
        title = stringResource(R.string.missions_schedule_title),
        subtitle = stringResource(R.string.missions_schedule_subtitle),
    ) {
        val difficultyProgress = (detail.priorityScore / 100f).coerceIn(0.08f, 1f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.missions_schedule_difficulty),
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )
            Text(
                priorityTier(detail.priorityScore),
                style = MaterialTheme.typography.titleSmall,
                color = AccentPrimary,
            )
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
                label = stringResource(R.string.missions_schedule_duration),
                value = estimatedDuration(detail.priorityScore),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.missions_schedule_energy),
                value = stringResource(R.string.missions_schedule_energy_value_format, (detail.priorityScore / 8).coerceAtLeast(4)),
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = stringResource(
                R.string.missions_schedule_due_format,
                detail.dueAt?.let(::formatTimestamp) ?: stringResource(R.string.missions_schedule_not_set),
            ),
            modifier = Modifier.padding(top = 16.dp),
            color = TextSecondary,
        )
        Text(
            text = stringResource(
                R.string.missions_schedule_reminder_format,
                detail.remindAt?.let(::formatTimestamp) ?: stringResource(R.string.missions_schedule_not_set),
            ),
            modifier = Modifier.padding(top = 4.dp),
            color = TextSecondary,
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onPickDueDate, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.missions_schedule_set_due))
            }
            Button(onClick = onPickReminder, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.missions_schedule_set_reminder))
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onClearDueDate, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.missions_schedule_clear_due))
            }
            OutlinedButton(onClick = onClearReminder, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.missions_schedule_clear_reminder))
            }
        }
        Text(
            text = stringResource(R.string.missions_schedule_priority_title),
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        PrioritySelector(
            selected = detail.priorityScore,
            onSelect = onSelectPriority,
        )
        Text(
            text = stringResource(R.string.missions_schedule_snooze_title),
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SidequestSpacing.Xs)) {
            OutlinedButton(onClick = onSnoozeThirty, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.missions_command_snooze_thirty))
            }
            OutlinedButton(onClick = onSnoozeTwoHours, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.missions_command_snooze_two_hours))
            }
            OutlinedButton(onClick = onSnoozeOneDay, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.missions_command_snooze_one_day))
            }
        }
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
private fun MissionIntelLinkCard(
    onOpenCapture: () -> Unit,
) {
    GlassCard(
        title = stringResource(R.string.missions_detail_intel_title),
        subtitle = stringResource(R.string.missions_detail_intel_subtitle),
    ) {
        OutlinedButton(
            onClick = onOpenCapture,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.missions_detail_intel_button))
        }
    }
}

@Composable
private fun MissionDangerZoneCard(
    onAbandon: () -> Unit,
) {
    GlassCard(
        title = stringResource(R.string.missions_detail_danger_title),
        subtitle = stringResource(R.string.missions_detail_danger_subtitle),
    ) {
        DestructiveOutlinedButton(
            label = stringResource(R.string.missions_detail_danger_action),
            onClick = onAbandon,
            modifier = Modifier.fillMaxWidth(),
        )
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
    icon: ImageVector,
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
                imageVector = icon,
                contentDescription = value,
                tint = AccentPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(value, style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
        }
    }
}

@Composable
private fun CompletionRewardBanner(
    reward: CompletionRewardUi,
) {
    Surface(
        color = BgGlow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.28f)),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Icon(
                        imageVector = SidequestIcons.Spark,
                        contentDescription = stringResource(R.string.missions_completion_icon_content_description),
                        tint = AccentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.missions_completion_banner_format, reward.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RewardSummaryLine(label = "+${reward.xp} XP")
                    RewardSummaryLine(label = "+${reward.gp} GP")
                }
            }
        }
    }
}

@Composable
private fun RewardSummaryLine(label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AccentPrimary),
        )
        Text(label, style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
    }
}

@Composable
private fun PrioritySelector(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    val options = listOf(
        25 to R.string.missions_priority_low,
        50 to R.string.missions_priority_normal,
        75 to R.string.missions_priority_high,
        95 to R.string.missions_priority_critical,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (score, labelRes) ->
                    Button(onClick = { onSelect(score) }) {
                        val label = stringResource(labelRes)
                        Text(if (selected == score) "$label *" else label)
                    }
                }
            }
        }
    }
}

private fun boardPrimaryActionLabelRes(mission: MissionCardModel): Int {
    return if (mission.status == MissionStatus.ACTIVE) {
        R.string.missions_board_complete
    } else {
        R.string.missions_command_activate
    }
}

private fun buildBoardStatusLine(mission: MissionCardModel): String {
    return buildString {
        append(stringResourceLabel(missionStatusLabelRes(mission.status)))
        append(" | ")
        append(priorityTier(mission.priorityScore))
        mission.dueAt?.let {
            append(" | ")
            append(stringResourceLabel(R.string.missions_board_due_label))
            append(" ")
            append(formatTimestamp(it))
        }
    }
}

private fun boardDueLabel(mission: MissionCardModel): String {
    return mission.dueAt?.let(::formatTimestamp) ?: stringResourceLabel(R.string.missions_board_no_due)
}

private fun boardReminderLabel(mission: MissionCardModel): String {
    return mission.remindAt?.let(::formatTimestamp) ?: stringResourceLabel(R.string.missions_board_no_reminder)
}

private fun missionStatusLabelRes(status: MissionStatus): Int {
    return when (status) {
        MissionStatus.DONE -> R.string.missions_status_done
        MissionStatus.ACTIVE -> R.string.missions_status_active
        MissionStatus.SNOOZED -> R.string.missions_status_snoozed
        MissionStatus.ARCHIVED -> R.string.missions_status_archived
        MissionStatus.OPEN -> R.string.missions_status_open
    }
}

private fun missionDetailPrimaryActionLabelRes(status: MissionStatus): Int? {
    return when (status) {
        MissionStatus.ACTIVE -> R.string.missions_command_complete
        MissionStatus.OPEN,
        MissionStatus.SNOOZED,
        MissionStatus.ARCHIVED,
        -> R.string.missions_command_activate
        MissionStatus.DONE -> null
    }
}

private fun missionDetailPrimaryBodyRes(status: MissionStatus): Int {
    return when (status) {
        MissionStatus.ACTIVE -> R.string.missions_detail_primary_body_active
        MissionStatus.OPEN -> R.string.missions_detail_primary_body_open
        MissionStatus.SNOOZED -> R.string.missions_detail_primary_body_snoozed
        MissionStatus.ARCHIVED -> R.string.missions_detail_primary_body_archived
        MissionStatus.DONE -> R.string.missions_detail_primary_body_done
    }
}

private fun stringResourceLabel(@androidx.annotation.StringRes resId: Int): String {
    return when (resId) {
        R.string.missions_board_due_label -> "Due"
        R.string.missions_board_no_due -> "No due date"
        R.string.missions_board_no_reminder -> "No reminder"
        R.string.missions_status_done -> "completed quest"
        R.string.missions_status_active -> "active quest"
        R.string.missions_status_snoozed -> "snoozed quest"
        R.string.missions_status_archived -> "archived quest"
        R.string.missions_status_open -> "open quest"
        else -> ""
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

private fun missionIconRes(mission: MissionCardModel): ImageVector {
    val title = mission.title.lowercase()
    return when {
        "clean" in title || "desk" in title || "kitchen" in title -> SidequestIcons.Clean
        "read" in title || "book" in title || "study" in title -> SidequestIcons.Study
        "run" in title || "walk" in title || "fit" in title -> SidequestIcons.Sprint
        else -> SidequestIcons.Compass
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

    private val _completionReward = MutableStateFlow<CompletionRewardUi?>(null)
    val completionReward: StateFlow<CompletionRewardUi?> = _completionReward.asStateFlow()

    fun complete(missionId: String) {
        missions.value.firstOrNull { it.id == missionId }?.let { mission ->
            _completionReward.value = CompletionRewardUi(
                title = mission.title,
                xp = rewardXp(mission),
                gp = rewardGp(mission),
                nonce = System.currentTimeMillis(),
            )
        }
        viewModelScope.launch { missionRepository.applyAction(MissionAction.Complete(missionId)) }
    }

    fun activate(missionId: String) {
        viewModelScope.launch { missionRepository.applyAction(MissionAction.Activate(missionId)) }
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

    fun abandon(missionId: String) {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.Archive(missionId))
        }
    }

    fun clearCompletionReward() {
        _completionReward.value = null
    }
}

data class CompletionRewardUi(
    val title: String,
    val xp: Int,
    val gp: Int,
    val nonce: Long,
)

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

    fun abandon() {
        archive()
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
