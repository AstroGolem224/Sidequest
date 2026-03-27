package com.astrogolem.sidequest.feature.lobby

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.MissionStatus
import com.astrogolem.sidequest.core.data.model.NoteSummary
import com.astrogolem.sidequest.core.data.model.RoutineCategory
import com.astrogolem.sidequest.core.data.model.RoutinePlanSummary
import com.astrogolem.sidequest.core.data.model.ShoppingListSummary
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.NotesRepository
import com.astrogolem.sidequest.core.data.repo.RoutinePlanRepository
import com.astrogolem.sidequest.core.data.repo.ShoppingListRepository
import com.astrogolem.sidequest.core.ui.components.EmptyStateCard
import com.astrogolem.sidequest.core.ui.components.ErrorStateCard
import com.astrogolem.sidequest.core.ui.components.GlassCard
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.components.LoadingStateCard
import com.astrogolem.sidequest.core.ui.components.SegmentedMeter
import com.astrogolem.sidequest.core.ui.components.StateShellAction
import com.astrogolem.sidequest.core.ui.components.StatusPill
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.CardSurface
import com.astrogolem.sidequest.core.ui.theme.CardStroke
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryRoute(
    onOpenShoppingHub: () -> Unit,
    onOpenRoutineHub: () -> Unit,
    onOpenMissionsHub: () -> Unit,
    onOpenCaptureHub: () -> Unit,
    onOpenShopping: (String) -> Unit,
    onOpenRoutine: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var notesExpanded by rememberSaveable { mutableStateOf(false) }
    var shoppingExpanded by rememberSaveable { mutableStateOf(false) }
    var routinesExpanded by rememberSaveable { mutableStateOf(false) }
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    var scansExpanded by rememberSaveable { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.importNote(uri)
        }
    }

    LaunchedEffect(state.openNoteId) {
        state.openNoteId?.let { noteId ->
            onOpenNote(noteId)
            viewModel.consumeOpenNote()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            InventoryLibraryHero(state = state)
        }

        if (!state.isLoaded) {
            item {
                LoadingStateCard(
                    title = "Loading inventory",
                    subtitle = "Checking notes, shopping lists, routines, archived missions, and saved captures on this device.",
                )
            }
        } else {
            state.statusMessage?.takeIf { state.statusIsError && it.isNotBlank() }?.let { statusMessage ->
                item {
                    ErrorStateCard(
                        title = "Inventory action failed",
                        subtitle = statusMessage,
                        supportingLines = listOf("Retry the import or continue browsing the rest of the library."),
                        primaryAction = StateShellAction(
                            label = "Import markdown again",
                            onClick = { importLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*")) },
                        ),
                        secondaryAction = StateShellAction(
                            label = "Dismiss",
                            onClick = viewModel::clearStatus,
                        ),
                    )
                }
            }

            item {
                CollapsibleInventorySectionHeader(
                    title = "Notes Library",
                    subtitle = "Markdown notes and captured reference you keep evolving over time.",
                    count = state.notes.size,
                    expanded = notesExpanded,
                ) {
                    notesExpanded = !notesExpanded
                }
            }
            if (notesExpanded) {
                item {
                    NotesCreateCard(
                        onCreateBlank = viewModel::createBlankNote,
                        onImport = { importLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*")) },
                    )
                }
                if (state.notes.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No notes yet",
                            subtitle = "Create a blank markdown note or import an existing file into the local library.",
                            primaryAction = StateShellAction(
                                label = "Create note",
                                onClick = viewModel::createBlankNote,
                            ),
                            secondaryAction = StateShellAction(
                                label = "Import markdown",
                                onClick = { importLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*")) },
                            ),
                        )
                    }
                } else {
                    items(state.notes, key = { it.id }) { note ->
                        NoteInventoryCard(note = note, onOpen = { onOpenNote(note.id) })
                    }
                }
            }

            item {
                CollapsibleInventorySectionHeader(
                    title = "Shopping Systems",
                    subtitle = "Reusable stock lists and grocery plans that support real-world runs.",
                    count = state.shoppingLists.size,
                    expanded = shoppingExpanded,
                ) {
                    shoppingExpanded = !shoppingExpanded
                }
            }
            if (shoppingExpanded) {
                item {
                    SectionActionCard(
                        title = "Shopping hub",
                        icon = SidequestIcons.QuestLog,
                        contentDescription = "Open shopping lists",
                        onPrimary = onOpenShoppingHub,
                    )
                }
                if (state.shoppingLists.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No shopping assets",
                            subtitle = "Open the shopping hub to create your first reusable stock or grocery list.",
                            primaryAction = StateShellAction(
                                label = "Open shopping hub",
                                onClick = onOpenShoppingHub,
                            ),
                        )
                    }
                } else {
                    items(state.shoppingLists, key = { it.id }) { summary ->
                        ShoppingInventoryCard(
                            summary = summary,
                            onRename = { viewModel.renameList(summary.id, it) },
                            onToggleArchived = { viewModel.setListArchived(summary.id, !summary.archived) },
                            onOpen = { onOpenShopping(summary.id) },
                        )
                    }
                }
            }

            item {
                CollapsibleInventorySectionHeader(
                    title = "Routine Systems",
                    subtitle = "Repeatable plans that stay ready for the next scheduled session.",
                    count = state.routinePlans.size,
                    expanded = routinesExpanded,
                ) {
                    routinesExpanded = !routinesExpanded
                }
            }
            if (routinesExpanded) {
                item {
                    SectionActionCard(
                        title = "Routine hub",
                        icon = SidequestIcons.Sprint,
                        contentDescription = "Open routine tasks",
                        onPrimary = onOpenRoutineHub,
                    )
                }
                if (state.routinePlans.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No routine plans yet",
                            subtitle = "Open the routine hub to turn a template into a repeatable plan.",
                            primaryAction = StateShellAction(
                                label = "Open routine hub",
                                onClick = onOpenRoutineHub,
                            ),
                        )
                    }
                } else {
                    items(state.routinePlans, key = { it.id }) { plan ->
                        RoutineInventoryCard(plan = plan, onOpen = { onOpenRoutine(plan.id) })
                    }
                }
            }

            item {
                CollapsibleInventorySectionHeader(
                    title = "Mission Archive",
                    subtitle = "Previously shipped or parked missions that no longer live on the active board.",
                    count = state.archivedMissions.size,
                    expanded = archivedExpanded,
                ) {
                    archivedExpanded = !archivedExpanded
                }
            }
            if (archivedExpanded) {
                if (state.archivedMissions.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No archived missions",
                            subtitle = "Your archive is empty. Open the mission board to deploy or complete work first.",
                            primaryAction = StateShellAction(
                                label = "Open mission board",
                                onClick = onOpenMissionsHub,
                            ),
                        )
                    }
                } else {
                    items(state.archivedMissions, key = { it.id }) { mission ->
                        QuestInventoryCard(
                            mission = mission,
                            onOpen = { onOpenMission(mission.id) },
                            onReactivate = { viewModel.reactivateMission(mission.id) },
                        )
                    }
                }
            }

            item {
                CollapsibleInventorySectionHeader(
                    title = "Intel Archive",
                    subtitle = "Saved scans and evidence sources you can reopen without touching profile or stats.",
                    count = state.captures.size,
                    expanded = scansExpanded,
                ) {
                    scansExpanded = !scansExpanded
                }
            }
            if (scansExpanded) {
                if (state.captures.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No saved scans",
                            subtitle = "Open Capture to scan a note, receipt, or whiteboard into the local archive.",
                            primaryAction = StateShellAction(
                                label = "Open Capture",
                                onClick = onOpenCaptureHub,
                            ),
                        )
                    }
                } else {
                    items(state.captures, key = { it.id }) { capture ->
                        CaptureInventoryCard(capture = capture, onOpen = { onOpenCapture(capture.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsRoute(
    onOpenMissionsHub: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (!state.isLoaded) {
            item {
                LoadingStateCard(
                    title = "Loading stats",
                    subtitle = "Calculating wellbeing signals and completion totals from your local Sidequest data.",
                )
            }
        } else if (
            state.completedQuests == 0 &&
            state.completedRoutineSessions == 0 &&
            state.checkedShoppingItems == 0 &&
            state.savedScans == 0
        ) {
            item {
                EmptyStateCard(
                    title = "No stats yet",
                    subtitle = "Complete a mission, routine, shopping list, or capture flow and the first trend signals will appear here.",
                    primaryAction = StateShellAction(
                        label = "Open mission board",
                        onClick = onOpenMissionsHub,
                    ),
                )
            }
        } else {
            item {
                GlassCard(
                    title = "Stats Compass",
                    subtitle = "These scores are derived from finished work across missions, routines, shopping, and captures. They are directional signals, not medical truth.",
                ) {
                    Text(
                        text = "Profile shows your personal progress. Inventory stores assets. Stats explains what your completed actions imply.",
                        color = TextSecondary,
                    )
                }
            }

            item { InventorySectionTitle("Wellbeing Signals", "Each bar explains what feeds it and how the score should be read.") }
            items(state.wellbeing, key = { it.label }) { metric ->
                WellbeingCard(metric)
            }

            item { InventorySectionTitle("Activity Totals", "Raw completion counters and mix below the top-level wellbeing bars.") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatsMetricCard("Completed Missions", state.completedQuests.toString(), Modifier.weight(1f))
                    StatsMetricCard("Routine Sessions", state.completedRoutineSessions.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatsMetricCard("Checked Items", state.checkedShoppingItems.toString(), Modifier.weight(1f))
                    StatsMetricCard("Saved Scans", state.savedScans.toString(), Modifier.weight(1f))
                }
            }
            item { InventorySectionTitle("Completion Mix", "How work is distributed across the system.") }
            items(state.breakdown, key = { it.label }) { slice ->
                BreakdownCard(slice)
            }
        }
    }
}

@Composable
private fun InventoryLibraryHero(
    state: InventoryUiState,
) {
    val assetCount =
        state.notes.size +
            state.shoppingLists.size +
            state.routinePlans.size +
            state.archivedMissions.size +
            state.captures.size
    GlassCard(
        title = "Inventory Library",
        subtitle = "This tab owns saved assets and archives. Personal progress lives in Profile, and analytics live in Stats.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatsMetricCard("Assets", assetCount.toString(), Modifier.weight(1f))
            StatsMetricCard("Notes", state.notes.size.toString(), Modifier.weight(1f))
            StatsMetricCard("Scans", state.captures.size.toString(), Modifier.weight(1f))
        }
        state.statusMessage?.takeIf { it.isNotBlank() }?.let { statusMessage ->
            Text(
                text = statusMessage,
                color = AccentSecondary,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun NotesCreateCard(
    onCreateBlank: () -> Unit,
    onImport: () -> Unit,
) {
    GlassCard(
        title = "Markdown Notes",
        subtitle = "Notes belong in inventory because they should persist, evolve, and stay searchable.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CompactActionIcon(
                icon = SidequestIcons.Document,
                contentDescription = "Create note",
                onClick = onCreateBlank,
                filled = true,
            )
            CompactActionIcon(
                icon = SidequestIcons.Folder,
                contentDescription = "Import markdown",
                onClick = onImport,
            )
        }
    }
}

@Composable
private fun InventorySectionTitle(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title.uppercase(), color = TextSecondary)
        Text(subtitle, color = TextSecondary)
    }
}

@Composable
private fun CollapsibleInventorySectionHeader(
    title: String,
    subtitle: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "$title ($count)",
                    color = AccentSecondary,
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) SidequestIcons.Minus else SidequestIcons.Plus,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = AccentSecondary,
                )
            }
        }
    }
}

@Composable
private fun SectionActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onPrimary: () -> Unit,
) {
    GlassCard(
        title = title,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            CompactActionIcon(
                icon = icon,
                contentDescription = contentDescription,
                onClick = onPrimary,
            )
        }
    }
}

@Composable
private fun CompactActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    filled: Boolean = false,
) {
    val colors = if (filled) {
        IconButtonDefaults.filledIconButtonColors(
            containerColor = AccentPrimary,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
        )
    } else {
        IconButtonDefaults.outlinedIconButtonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = AccentSecondary,
        )
    }
    OutlinedIconButton(
        onClick = onClick,
        colors = colors,
        border = if (filled) null else IconButtonDefaults.outlinedIconButtonBorder(enabled = true),
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NoteInventoryCard(
    note: NoteSummary,
    onOpen: () -> Unit,
) {
    GlassCard(
        title = note.title,
        subtitle = "${if (note.imported) "imported" else "local"} • updated ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(note.updatedAt))}",
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            CompactActionIcon(
                icon = SidequestIcons.Document,
                contentDescription = "Open note",
                onClick = onOpen,
            )
        }
    }
}

@Composable
private fun ShoppingInventoryCard(
    summary: ShoppingListSummary,
    onRename: (String) -> Unit,
    onToggleArchived: () -> Unit,
    onOpen: () -> Unit,
) {
    var draftTitle by rememberSaveable(summary.id, summary.title) { mutableStateOf(summary.title) }

    GlassCard(
        title = draftTitle,
        subtitle = "${summary.source.name.lowercase()} | ${summary.checkedCount}/${summary.itemCount} checked",
    ) {
        OutlinedTextField(
            value = draftTitle,
            onValueChange = { draftTitle = it },
            label = { Text("List title") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = { onRename(draftTitle) }, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
            OutlinedButton(onClick = onToggleArchived, modifier = Modifier.weight(1f)) {
                Text(if (summary.archived) "Reactivate" else "Archive")
            }
        }
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            Text("Open List")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineInventoryCard(
    plan: RoutinePlanSummary,
    onOpen: () -> Unit,
) {
    GlassCard(
        title = plan.title,
        subtitle = "${plan.category.name.lowercase()} | ${plan.durationMinutes} min | ${plan.completionCount} completions",
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusPill(
                text = if (plan.active) "active" else "paused",
                tone = if (plan.active) HudTone.Cyan else HudTone.Neutral,
            )
            StatusPill(
                text = "${plan.hour.toString().padStart(2, '0')}:${plan.minute.toString().padStart(2, '0')}",
                tone = HudTone.Violet,
            )
        }
        Text(
            text = "Schedule: ${plan.weekdays.ifBlank { "manual" }}",
            color = TextSecondary,
            modifier = Modifier.padding(top = 10.dp),
        )
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text("Edit Routine")
        }
    }
}

@Composable
private fun QuestInventoryCard(
    mission: MissionCardModel,
    onOpen: () -> Unit,
    onReactivate: () -> Unit,
) {
    var expanded by rememberSaveable(mission.id) { mutableStateOf(false) }
    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke.copy(alpha = 0.72f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = mission.title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) SidequestIcons.Minus else SidequestIcons.Plus,
                        contentDescription = if (expanded) "Collapse mission" else "Expand mission",
                        tint = AccentSecondary,
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = mission.description.ifBlank { "Archived mission" },
                        color = TextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                            Text("Open")
                        }
                        OutlinedButton(onClick = onReactivate, modifier = Modifier.weight(1f)) {
                            Text("Reactivate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureInventoryCard(
    capture: CaptureSummary,
    onOpen: () -> Unit,
) {
    GlassCard(
        title = capture.displayTitle,
        subtitle = "Status: ${capture.status.name.lowercase()}",
    ) {
        OutlinedButton(onClick = onOpen) {
            Text("Open Details")
        }
    }
}

@Composable
private fun StatsMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        title = value,
        subtitle = label,
        modifier = modifier,
    ) {}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WellbeingCard(
    metric: WellbeingMetricUi,
) {
    GlassCard(
        title = metric.label,
        subtitle = metric.description,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusPill(text = "${metric.score}%", tone = metric.accentTone)
            StatusPill(text = metric.signal, tone = metric.signalTone)
        }
        SegmentedMeter(
            progress = (metric.score / 100f).coerceIn(0.08f, 1f),
            tone = metric.accentTone,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = metric.breakdown,
            color = TextSecondary,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = metric.sourceSummary,
            color = TextSecondary,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun BreakdownCard(
    slice: StatsBreakdownUi,
) {
    GlassCard(
        title = slice.label,
        subtitle = slice.value,
    ) {
        Text(slice.description, color = TextSecondary)
    }
}

data class InventoryUiState(
    val notes: List<NoteSummary> = emptyList(),
    val shoppingLists: List<ShoppingListSummary> = emptyList(),
    val routinePlans: List<RoutinePlanSummary> = emptyList(),
    val archivedMissions: List<MissionCardModel> = emptyList(),
    val captures: List<CaptureSummary> = emptyList(),
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
    val openNoteId: String? = null,
    val isLoaded: Boolean = false,
)

data class WellbeingMetricUi(
    val label: String,
    val score: Int,
    val description: String,
    val breakdown: String,
    val sourceSummary: String,
    val signal: String,
    val accentTone: HudTone,
    val signalTone: HudTone,
)

data class StatsBreakdownUi(
    val label: String,
    val value: String,
    val description: String,
)

data class StatsUiState(
    val completedQuests: Int = 0,
    val completedRoutineSessions: Int = 0,
    val checkedShoppingItems: Int = 0,
    val savedScans: Int = 0,
    val wellbeing: List<WellbeingMetricUi> = emptyList(),
    val breakdown: List<StatsBreakdownUi> = emptyList(),
    val isLoaded: Boolean = false,
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val routinePlanRepository: RoutinePlanRepository,
    private val missionRepository: MissionRepository,
    private val captureRepository: CaptureRepository,
) : ViewModel() {
    private val feedback = MutableStateFlow("")
    private val feedbackIsError = MutableStateFlow(false)
    private val openNoteId = MutableStateFlow<String?>(null)

    val state: StateFlow<InventoryUiState> =
        combine(
            notesRepository.observeNotes(),
            shoppingListRepository.observeLists(),
            routinePlanRepository.observePlans(),
            missionRepository.observeMissions(),
            captureRepository.observeCaptures(),
            feedback,
            feedbackIsError,
            openNoteId,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val notes = values[0] as List<NoteSummary>
            @Suppress("UNCHECKED_CAST")
            val shoppingLists = values[1] as List<ShoppingListSummary>
            @Suppress("UNCHECKED_CAST")
            val routinePlans = values[2] as List<RoutinePlanSummary>
            @Suppress("UNCHECKED_CAST")
            val missions = values[3] as List<MissionCardModel>
            @Suppress("UNCHECKED_CAST")
            val captures = values[4] as List<CaptureSummary>
            val currentFeedback = values[5] as String
            val currentFeedbackIsError = values[6] as Boolean
            val currentOpenNoteId = values[7] as String?
            InventoryUiState(
                notes = notes,
                shoppingLists = shoppingLists,
                routinePlans = routinePlans,
                archivedMissions = missions.filter { it.status == MissionStatus.ARCHIVED },
                captures = captures,
                statusMessage = currentFeedback.ifBlank { null },
                statusIsError = currentFeedbackIsError,
                openNoteId = currentOpenNoteId,
                isLoaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun createBlankNote() {
        viewModelScope.launch {
            val noteId = notesRepository.createNote(title = "New Note")
            feedback.value = "note created"
            feedbackIsError.value = false
            openNoteId.value = noteId
        }
    }

    fun importNote(uri: Uri) {
        viewModelScope.launch {
            notesRepository.importMarkdown(uri)
                .fold(
                    onSuccess = { noteId ->
                        feedback.value = "markdown imported"
                        feedbackIsError.value = false
                        openNoteId.value = noteId
                    },
                    onFailure = { error ->
                        feedback.value = error.message ?: "markdown import failed"
                        feedbackIsError.value = true
                    },
                )
        }
    }

    fun clearStatus() {
        feedback.value = ""
        feedbackIsError.value = false
    }

    fun consumeOpenNote() {
        openNoteId.value = null
    }

    fun renameList(listId: String, title: String) {
        viewModelScope.launch {
            shoppingListRepository.renameList(listId, title)
        }
    }

    fun setListArchived(listId: String, archived: Boolean) {
        viewModelScope.launch {
            shoppingListRepository.setListArchived(listId, archived)
        }
    }

    fun reactivateMission(missionId: String) {
        viewModelScope.launch {
            missionRepository.applyAction(MissionAction.Activate(missionId))
        }
    }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    missionRepository: MissionRepository,
    routinePlanRepository: RoutinePlanRepository,
    shoppingListRepository: ShoppingListRepository,
    captureRepository: CaptureRepository,
) : ViewModel() {
    val state: StateFlow<StatsUiState> =
        combine(
            missionRepository.observeMissions(),
            routinePlanRepository.observePlans(),
            shoppingListRepository.observeLists(),
            captureRepository.observeCaptures(),
        ) { missions, routines, shoppingLists, captures ->
            val completedQuests = missions.count { it.status == MissionStatus.DONE }
            val completedRoutineSessions = routines.sumOf { it.completionCount }
            val checkedShoppingItems = shoppingLists.sumOf { it.checkedCount }
            val savedScans = captures.size

            val missionBuckets = missions.filter { it.status == MissionStatus.DONE }
                .map(::classifyMissionBucket)
            val routineBuckets = routines.flatMap { plan -> List(plan.completionCount) { bucketForCategory(plan.category) } }

            val physicalCount = missionBuckets.count { it == StatsBucket.PHYSICAL } + routineBuckets.count { it == StatsBucket.PHYSICAL }
            val mentalCount = missionBuckets.count { it == StatsBucket.MENTAL } + routineBuckets.count { it == StatsBucket.MENTAL }
            val homeCount = missionBuckets.count { it == StatsBucket.HOME } + routineBuckets.count { it == StatsBucket.HOME }
            val adminCount = missionBuckets.count { it == StatsBucket.ADMIN } + routineBuckets.count { it == StatsBucket.ADMIN }

            StatsUiState(
                completedQuests = completedQuests,
                completedRoutineSessions = completedRoutineSessions,
                checkedShoppingItems = checkedShoppingItems,
                savedScans = savedScans,
                wellbeing = listOf(
                    buildWellbeingMetric(
                        label = "Physical Health",
                        description = "Body routines, workouts, walks, and outdoor maintenance completed.",
                        sourceSummary = "Source: completed body-tagged missions and BODY routine sessions.",
                        count = physicalCount,
                        baseline = 28,
                        multiplier = 11,
                        accentTone = HudTone.Coral,
                    ),
                    buildWellbeingMetric(
                        label = "Mental Health",
                        description = "Meditation, journaling, reading, reflection, and other mind-care completions.",
                        sourceSummary = "Source: completed mind-care missions and MIND routine sessions.",
                        count = mentalCount,
                        baseline = 32,
                        multiplier = 10,
                        accentTone = HudTone.Violet,
                    ),
                    buildWellbeingMetric(
                        label = "Home Order",
                        description = "Cleaning, garden, laundry, kitchen, and general reset completions.",
                        sourceSummary = "Source: completed home-reset missions plus HOME and OUTDOOR routine sessions.",
                        count = homeCount,
                        baseline = 26,
                        multiplier = 12,
                        accentTone = HudTone.Cyan,
                    ),
                    buildWellbeingMetric(
                        label = "Life Admin",
                        description = "Finance, planning, shopping, scheduling, and logistics completions.",
                        sourceSummary = "Source: admin-style missions, LIFE routines, and shopping follow-through.",
                        count = adminCount,
                        baseline = 24,
                        multiplier = 11,
                        accentTone = HudTone.Amber,
                    ),
                ).map { metric ->
                    when (metric.label) {
                        "Physical Health" -> metric.copy(breakdown = "counted sessions: $physicalCount")
                        "Mental Health" -> metric.copy(breakdown = "counted sessions: $mentalCount")
                        "Home Order" -> metric.copy(breakdown = "counted sessions: $homeCount")
                        else -> metric.copy(breakdown = "counted sessions: $adminCount")
                    }
                },
                breakdown = listOf(
                    StatsBreakdownUi(
                        label = "Mission completions",
                        value = completedQuests.toString(),
                        description = "Finished missions from the main board.",
                    ),
                    StatsBreakdownUi(
                        label = "Routine completions",
                        value = completedRoutineSessions.toString(),
                        description = "Sessions claimed from repeatable routine plans.",
                    ),
                    StatsBreakdownUi(
                        label = "Shopping follow-through",
                        value = checkedShoppingItems.toString(),
                        description = "Checked grocery and restock items across all lists.",
                    ),
                    StatsBreakdownUi(
                        label = "Captured intel",
                        value = savedScans.toString(),
                        description = "Saved scans available for recall, search, or cleanup.",
                    ),
                ),
                isLoaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

private enum class StatsBucket {
    PHYSICAL,
    MENTAL,
    HOME,
    ADMIN,
}

private fun buildWellbeingMetric(
    label: String,
    description: String,
    sourceSummary: String,
    count: Int,
    baseline: Int,
    multiplier: Int,
    accentTone: HudTone,
): WellbeingMetricUi {
    val score = (baseline + count * multiplier).coerceIn(18, 100)
    val signal = when {
        score >= 85 -> "strong"
        score >= 65 -> "stable"
        score >= 45 -> "recovering"
        else -> "low"
    }
    val tone = when {
        score >= 85 -> HudTone.Cyan
        score >= 65 -> HudTone.Violet
        score >= 45 -> HudTone.Amber
        else -> HudTone.Neutral
    }
    return WellbeingMetricUi(
        label = label,
        score = score,
        description = description,
        breakdown = "Formula: baseline $baseline + $multiplier per matched completion.",
        sourceSummary = sourceSummary,
        signal = signal,
        accentTone = accentTone,
        signalTone = tone,
    )
}

private fun bucketForCategory(category: RoutineCategory): StatsBucket {
    return when (category) {
        RoutineCategory.BODY -> StatsBucket.PHYSICAL
        RoutineCategory.MIND -> StatsBucket.MENTAL
        RoutineCategory.HOME,
        RoutineCategory.OUTDOOR,
        -> StatsBucket.HOME
        RoutineCategory.LIFE -> StatsBucket.ADMIN
    }
}

private fun classifyMissionBucket(mission: MissionCardModel): StatsBucket {
    val haystack = "${mission.title} ${mission.description}".lowercase()
    return when {
        listOf("run", "walk", "sport", "workout", "stretch", "gym", "garden").any { it in haystack } -> StatsBucket.PHYSICAL
        listOf("meditat", "journal", "read", "study", "reflect", "breath").any { it in haystack } -> StatsBucket.MENTAL
        listOf("clean", "kitchen", "room", "desk", "laundry", "garden", "reset").any { it in haystack } -> StatsBucket.HOME
        else -> StatsBucket.ADMIN
    }
}

