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
import com.astrogolem.sidequest.core.ui.components.GlassCard
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.components.SegmentedMeter
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryRoute(
    onOpenShoppingHub: () -> Unit,
    onOpenRoutineHub: () -> Unit,
    onOpenShopping: (String) -> Unit,
    onOpenRoutine: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
            GlassCard(
                title = "Inventory",
                subtitle = "Reusable assets that stay editable, searchable, and re-openable instead of disappearing into the quest feed.",
            ) {
                Text("Shopping lists, notes, routine plans, archived quests, and saved scans live here.")
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    label = { Text("Search inventory") },
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Text(
                    text = "${state.notes.size} notes • ${state.shoppingLists.size} lists • ${state.routinePlans.size} routines • ${state.archivedMissions.size} archived • ${state.captures.size} scans",
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (state.feedback.isNotBlank()) {
                    Text(
                        text = state.feedback,
                        color = AccentSecondary,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }

        item { InventorySectionTitle("Notes", "Markdown notes you can write, import, edit, and reopen.") }
        item {
            NotesCreateCard(
                onCreateBlank = viewModel::createBlankNote,
                onImport = { importLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*")) },
            )
        }
        if (state.notes.isEmpty()) {
            item {
                GlassCard(
                    title = "No notes yet",
                    subtitle = "Create a markdown note or import an existing .md file and it will stay in inventory.",
                ) {}
            }
        } else {
            items(state.notes, key = { it.id }) { note ->
                NoteInventoryCard(note = note, onOpen = { onOpenNote(note.id) })
            }
        }

        item { InventorySectionTitle("Shopping Lists", "Active and dormant purchase flows.") }
        item {
            SectionActionCard(
                title = "Shopping hub",
                subtitle = "Open the full list builder, dictate groceries, or import a photo list.",
                icon = SidequestIcons.QuestLog,
                contentDescription = "Open shopping lists",
                onPrimary = onOpenShoppingHub,
            )
        }
        if (state.shoppingLists.isEmpty()) {
            item {
                GlassCard(
                    title = "No shopping assets",
                    subtitle = "Create a shopping list here and it will stay reusable inside inventory.",
                ) {}
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

        item { InventorySectionTitle("Routine Plans", "Repeatable systems with editable schedules and reminders.") }
        item {
            SectionActionCard(
                title = "Routine hub",
                subtitle = "Build recurring plans, set reminder cadence, and keep repeatable work out of the main quest feed.",
                icon = SidequestIcons.Sprint,
                contentDescription = "Open routine tasks",
                onPrimary = onOpenRoutineHub,
            )
        }
        if (state.routinePlans.isEmpty()) {
            item {
                GlassCard(
                    title = "No routine plans yet",
                    subtitle = "Use the routine planner here to create repeatable body, mind, home, and life-admin systems.",
                ) {}
            }
        } else {
            items(state.routinePlans, key = { it.id }) { plan ->
                RoutineInventoryCard(plan = plan, onOpen = { onOpenRoutine(plan.id) })
            }
        }

        item { InventorySectionTitle("Archived Quests", "Dormant commitments that can be reactivated.") }
        if (state.archivedMissions.isEmpty()) {
            item {
                GlassCard(
                    title = "No archived quests",
                    subtitle = "Abandoned or archived quests will accumulate here instead of vanishing.",
                ) {}
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

        item { InventorySectionTitle("Saved Scans", "Recent capture intel you may want to inspect or prune.") }
        if (state.captures.isEmpty()) {
            item {
                GlassCard(
                    title = "No saved scans",
                    subtitle = "Camera imports and scans land here once Sidequest stores them locally.",
                ) {}
            }
        } else {
            items(state.captures, key = { it.id }) { capture ->
                CaptureInventoryCard(capture = capture, onOpen = { onOpenCapture(capture.id) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsRoute(
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            GlassCard(
                title = "Core Signals",
                subtitle = "The top bars are your real wellbeing readout, derived from finished work instead of self-report.",
            ) {}
        }

        item { InventorySectionTitle("Wellbeing Signals", "The four system-health bars come first. Activity totals sit below them.") }
        items(state.wellbeing, key = { it.label }) { metric ->
            WellbeingCard(metric)
        }

        item { InventorySectionTitle("Activity Totals", "Raw completion counters and mix below the top-level wellbeing bars.") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatsMetricCard("Completed Quests", state.completedQuests.toString(), Modifier.weight(1f))
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
private fun SectionActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onPrimary: () -> Unit,
) {
    GlassCard(
        title = title,
        subtitle = subtitle,
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
                        contentDescription = if (expanded) "Collapse quest" else "Expand quest",
                        tint = AccentSecondary,
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = mission.description.ifBlank { "Archived quest" },
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
    val query: String = "",
    val notes: List<NoteSummary> = emptyList(),
    val shoppingLists: List<ShoppingListSummary> = emptyList(),
    val routinePlans: List<RoutinePlanSummary> = emptyList(),
    val archivedMissions: List<MissionCardModel> = emptyList(),
    val captures: List<CaptureSummary> = emptyList(),
    val feedback: String = "",
    val openNoteId: String? = null,
)

data class WellbeingMetricUi(
    val label: String,
    val score: Int,
    val description: String,
    val breakdown: String,
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
    private val openNoteId = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")

    val state: StateFlow<InventoryUiState> =
        combine(
            notesRepository.observeNotes(),
            shoppingListRepository.observeLists(),
            routinePlanRepository.observePlans(),
            missionRepository.observeMissions(),
            captureRepository.observeCaptures(),
            feedback,
            openNoteId,
            query,
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
            val currentOpenNoteId = values[6] as String?
            val currentQuery = (values[7] as String).trim()
            val normalizedQuery = currentQuery.lowercase()
            val filteredNotes = notes.filter { note ->
                normalizedQuery.isBlank() || note.title.lowercase().contains(normalizedQuery)
            }
            val filteredShoppingLists = shoppingLists.filter { list ->
                normalizedQuery.isBlank() || list.title.lowercase().contains(normalizedQuery)
            }
            val filteredRoutinePlans = routinePlans.filter { plan ->
                normalizedQuery.isBlank() ||
                    plan.title.lowercase().contains(normalizedQuery) ||
                    plan.targetLabel.lowercase().contains(normalizedQuery)
            }
            val filteredArchivedMissions = missions.filter { it.status == MissionStatus.ARCHIVED }.filter { mission ->
                normalizedQuery.isBlank() ||
                    mission.title.lowercase().contains(normalizedQuery) ||
                    mission.description.lowercase().contains(normalizedQuery)
            }
            val filteredCaptures = captures.filter { capture ->
                normalizedQuery.isBlank() ||
                    capture.displayTitle.lowercase().contains(normalizedQuery) ||
                    capture.sourceLabel.lowercase().contains(normalizedQuery) ||
                    capture.status.name.lowercase().contains(normalizedQuery)
            }
            InventoryUiState(
                query = currentQuery,
                notes = filteredNotes,
                shoppingLists = filteredShoppingLists,
                routinePlans = filteredRoutinePlans,
                archivedMissions = filteredArchivedMissions,
                captures = filteredCaptures.take(if (normalizedQuery.isBlank()) 12 else filteredCaptures.size),
                feedback = currentFeedback,
                openNoteId = currentOpenNoteId,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun createBlankNote() {
        viewModelScope.launch {
            val noteId = notesRepository.createNote(title = "New Note")
            feedback.value = "note created"
            openNoteId.value = noteId
        }
    }

    fun importNote(uri: Uri) {
        viewModelScope.launch {
            notesRepository.importMarkdown(uri)
                .fold(
                    onSuccess = { noteId ->
                        feedback.value = "markdown imported"
                        openNoteId.value = noteId
                    },
                    onFailure = { error ->
                        feedback.value = error.message ?: "markdown import failed"
                    },
                )
        }
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
                        count = physicalCount,
                        baseline = 28,
                        multiplier = 11,
                        accentTone = HudTone.Coral,
                    ),
                    buildWellbeingMetric(
                        label = "Mental Health",
                        description = "Meditation, journaling, reading, reflection, and other mind-care completions.",
                        count = mentalCount,
                        baseline = 32,
                        multiplier = 10,
                        accentTone = HudTone.Violet,
                    ),
                    buildWellbeingMetric(
                        label = "Home Order",
                        description = "Cleaning, garden, laundry, kitchen, and general reset completions.",
                        count = homeCount,
                        baseline = 26,
                        multiplier = 12,
                        accentTone = HudTone.Cyan,
                    ),
                    buildWellbeingMetric(
                        label = "Life Admin",
                        description = "Finance, planning, shopping, scheduling, and logistics completions.",
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
                        label = "Quest completions",
                        value = completedQuests.toString(),
                        description = "Finished quests from the main board.",
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
        breakdown = "",
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
