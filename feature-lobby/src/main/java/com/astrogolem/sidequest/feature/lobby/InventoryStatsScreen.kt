package com.astrogolem.sidequest.feature.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.CaptureSummary
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.model.MissionStatus
import com.astrogolem.sidequest.core.data.model.RoutineCategory
import com.astrogolem.sidequest.core.data.model.RoutinePlanSummary
import com.astrogolem.sidequest.core.data.model.ShoppingListSummary
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.RoutinePlanRepository
import com.astrogolem.sidequest.core.data.repo.ShoppingListRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.components.StatusPill
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun InventoryRoute(
    onOpenShopping: (String) -> Unit,
    onOpenRoutine: (String) -> Unit,
    onOpenMission: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScaffoldCard(
                title = "Inventory",
                subtitle = "Persistent assets that should stay editable and reusable instead of disappearing into the quest feed.",
            ) {
                Text("Shopping lists can be renamed, archived, and reactivated. Routine plans stay editable. Archived quests and stored scans stay one tap away.")
            }
        }

        item { InventorySectionTitle("Shopping Lists", "Active and dormant purchase flows.") }
        if (state.shoppingLists.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "No shopping assets",
                    subtitle = "Create a shopping list from the Dashboard and it will appear here.",
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

        item { InventorySectionTitle("Routine Plans", "Repeatable systems with editable schedules.") }
        if (state.routinePlans.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "No routine plans yet",
                    subtitle = "Use the routine planner to create repeatable body, mind, home, and life-admin systems.",
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
                ScaffoldCard(
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
                ScaffoldCard(
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
            ScaffoldCard(
                title = "Stats",
                subtitle = "Real completion history plus derived health indicators. The formulas are documented so the game layer stays honest.",
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatsMetricCard("Completed Quests", state.completedQuests.toString(), Modifier.weight(1f))
                    StatsMetricCard("Routine Sessions", state.completedRoutineSessions.toString(), Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatsMetricCard("Checked Items", state.checkedShoppingItems.toString(), Modifier.weight(1f))
                    StatsMetricCard("Saved Scans", state.savedScans.toString(), Modifier.weight(1f))
                }
            }
        }

        item { InventorySectionTitle("Wellbeing Signals", "Derived from real completions across routines and quests.") }
        items(state.wellbeing, key = { it.label }) { metric ->
            WellbeingCard(metric)
        }

        item { InventorySectionTitle("Completion Mix", "How work is distributed across the system.") }
        items(state.breakdown, key = { it.label }) { slice ->
            BreakdownCard(slice)
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
private fun ShoppingInventoryCard(
    summary: ShoppingListSummary,
    onRename: (String) -> Unit,
    onToggleArchived: () -> Unit,
    onOpen: () -> Unit,
) {
    var draftTitle by rememberSaveable(summary.id, summary.title) { mutableStateOf(summary.title) }

    ScaffoldCard(
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

@Composable
private fun RoutineInventoryCard(
    plan: RoutinePlanSummary,
    onOpen: () -> Unit,
) {
    ScaffoldCard(
        title = plan.title,
        subtitle = "${plan.category.name.lowercase()} | ${plan.durationMinutes} min | ${plan.completionCount} completions",
    ) {
        StatusPill(
            text = if (plan.active) "active" else "paused",
            tone = if (plan.active) HudTone.Cyan else HudTone.Neutral,
        )
        Text(
            text = "Schedule: ${plan.weekdays.ifBlank { "manual" }} @ ${plan.hour.toString().padStart(2, '0')}:${plan.minute.toString().padStart(2, '0')}",
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
    ScaffoldCard(
        title = mission.title,
        subtitle = mission.description.ifBlank { "Archived quest" },
    ) {
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

@Composable
private fun CaptureInventoryCard(
    capture: CaptureSummary,
    onOpen: () -> Unit,
) {
    ScaffoldCard(
        title = when (capture.sourceLabel.lowercase()) {
            "camera" -> "Camera scan"
            "import" -> "Imported scan"
            else -> capture.sourceLabel.replaceFirstChar { it.uppercase() }
        },
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
    ScaffoldCard(
        title = value,
        subtitle = label,
        modifier = modifier,
    ) {}
}

@Composable
private fun WellbeingCard(
    metric: WellbeingMetricUi,
) {
    ScaffoldCard(
        title = "${metric.label} ${metric.score}%",
        subtitle = metric.description,
    ) {
        StatusPill(text = metric.signal, tone = metric.tone)
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
    ScaffoldCard(
        title = slice.label,
        subtitle = slice.value,
    ) {
        Text(slice.description, color = TextSecondary)
    }
}

data class InventoryUiState(
    val shoppingLists: List<ShoppingListSummary> = emptyList(),
    val routinePlans: List<RoutinePlanSummary> = emptyList(),
    val archivedMissions: List<MissionCardModel> = emptyList(),
    val captures: List<CaptureSummary> = emptyList(),
)

data class WellbeingMetricUi(
    val label: String,
    val score: Int,
    val description: String,
    val breakdown: String,
    val signal: String,
    val tone: HudTone,
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
    private val shoppingListRepository: ShoppingListRepository,
    private val routinePlanRepository: RoutinePlanRepository,
    private val missionRepository: MissionRepository,
    private val captureRepository: CaptureRepository,
) : ViewModel() {
    val state: StateFlow<InventoryUiState> =
        combine(
            shoppingListRepository.observeLists(),
            routinePlanRepository.observePlans(),
            missionRepository.observeMissions(),
            captureRepository.observeCaptures(),
        ) { shoppingLists, routinePlans, missions, captures ->
            InventoryUiState(
                shoppingLists = shoppingLists,
                routinePlans = routinePlans,
                archivedMissions = missions.filter { it.status == MissionStatus.ARCHIVED },
                captures = captures.take(10),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

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
                    ),
                    buildWellbeingMetric(
                        label = "Mental Health",
                        description = "Meditation, journaling, reading, reflection, and other mind-care completions.",
                        count = mentalCount,
                        baseline = 32,
                        multiplier = 10,
                    ),
                    buildWellbeingMetric(
                        label = "Home Order",
                        description = "Cleaning, garden, laundry, kitchen, and general reset completions.",
                        count = homeCount,
                        baseline = 26,
                        multiplier = 12,
                    ),
                    buildWellbeingMetric(
                        label = "Life Admin",
                        description = "Finance, planning, shopping, scheduling, and logistics completions.",
                        count = adminCount,
                        baseline = 24,
                        multiplier = 11,
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
        tone = tone,
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
