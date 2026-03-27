package com.astrogolem.sidequest.feature.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.RoutineCategory
import com.astrogolem.sidequest.core.data.model.RoutinePlanDetail
import com.astrogolem.sidequest.core.data.model.RoutinePlanSummary
import com.astrogolem.sidequest.core.data.model.RoutineTriggerMode
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.RoutinePlanRepository
import com.astrogolem.sidequest.core.ui.components.DestructiveOutlinedButton
import com.astrogolem.sidequest.core.ui.components.EmptyStateCard
import com.astrogolem.sidequest.core.ui.components.InlineSupportText
import com.astrogolem.sidequest.core.ui.components.LoadingStateCard
import com.astrogolem.sidequest.core.ui.components.PreferenceSwitchRow
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineTemplate(
    val key: String,
    val title: String,
    val category: RoutineCategory,
    val durationMinutes: Int,
    val xpReward: Int,
    val triggerMode: RoutineTriggerMode,
    val targetLabel: String,
    val notes: String,
)

private val routineTemplates = listOf(
    RoutineTemplate("meditation", "Meditation", RoutineCategory.MIND, 10, 18, RoutineTriggerMode.MANUAL, "Mind reset", "Quiet, no-phone block. Great as a morning or shutdown ritual."),
    RoutineTemplate("sport", "Sport", RoutineCategory.BODY, 45, 40, RoutineTriggerMode.FITNESS, "Workout", "Future candidate for Google Fit or Health Connect verification."),
    RoutineTemplate("stretch", "Stretch", RoutineCategory.BODY, 12, 14, RoutineTriggerMode.MANUAL, "Mobility", "Short reset between deep work blocks."),
    RoutineTemplate("walk", "Walk", RoutineCategory.BODY, 25, 22, RoutineTriggerMode.FITNESS, "Fresh air round", "Low-friction movement routine for daily consistency."),
    RoutineTemplate("room-reset", "Aufraeumen", RoutineCategory.HOME, 20, 24, RoutineTriggerMode.CAPTURE, "Zimmer", "Good candidate for capture before/after evidence."),
    RoutineTemplate("kitchen-reset", "Kitchen Reset", RoutineCategory.HOME, 15, 20, RoutineTriggerMode.CAPTURE, "Kueche", "Fast cleanup round after cooking."),
    RoutineTemplate("garden-round", "Garden Round", RoutineCategory.OUTDOOR, 25, 28, RoutineTriggerMode.CAPTURE, "Garten", "Useful for later plant-care capture recognition."),
    RoutineTemplate("reading", "Reading", RoutineCategory.MIND, 30, 22, RoutineTriggerMode.MANUAL, "Book or article", "Low-friction recurring learning block."),
    RoutineTemplate("journal", "Journaling", RoutineCategory.MIND, 15, 18, RoutineTriggerMode.MANUAL, "Daily notes", "Works well morning or shutdown."),
    RoutineTemplate("sleep-reset", "Sleep Reset", RoutineCategory.MIND, 20, 18, RoutineTriggerMode.MANUAL, "Wind-down", "Low-stimulation evening routine for better recovery."),
    RoutineTemplate("hydration", "Hydration Reset", RoutineCategory.BODY, 5, 8, RoutineTriggerMode.MANUAL, "Water bottle", "Tiny repeatable health baseline that is easy to keep honest."),
    RoutineTemplate("plant-care", "Plant Care", RoutineCategory.OUTDOOR, 12, 14, RoutineTriggerMode.CAPTURE, "Plants", "Small visual maintenance loop that can later use capture verification."),
    RoutineTemplate("laundry", "Laundry", RoutineCategory.HOME, 15, 16, RoutineTriggerMode.MANUAL, "Wash cycle", "Simple recurring household rhythm."),
    RoutineTemplate("shopping-restock", "Shopping Restock", RoutineCategory.LIFE, 20, 16, RoutineTriggerMode.MANUAL, "Groceries", "Pairs well with the shopping-list flow."),
    RoutineTemplate("deep-work", "Deep Work Sprint", RoutineCategory.LIFE, 50, 32, RoutineTriggerMode.MANUAL, "Project block", "Recurring focus slot with deterministic XP."),
    RoutineTemplate("language", "Language Practice", RoutineCategory.MIND, 20, 18, RoutineTriggerMode.MANUAL, "Lesson or vocabulary", "Simple recurring skill-building task."),
    RoutineTemplate("meal-prep", "Meal Prep", RoutineCategory.LIFE, 35, 26, RoutineTriggerMode.MANUAL, "Lunches or dinner prep", "Pairs well with shopping and kitchen reset."),
    RoutineTemplate("finance-review", "Finance Review", RoutineCategory.LIFE, 25, 20, RoutineTriggerMode.MANUAL, "Budget or invoices", "Weekly admin ritual with deterministic XP."),
    RoutineTemplate("inbox-zero", "Inbox Zero", RoutineCategory.LIFE, 20, 18, RoutineTriggerMode.MANUAL, "Email or messages", "Useful for recurring admin cleanup."),
    RoutineTemplate("social-checkin", "Social Check-in", RoutineCategory.MIND, 10, 12, RoutineTriggerMode.MANUAL, "Friend or family ping", "Keeps relational maintenance visible instead of accidental."),
)

private val weekdayOptions = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinePlannerRoute(
    onOpenPlan: (String) -> Unit,
    viewModel: RoutinePlannerViewModel = hiltViewModel(),
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
                title = stringResource(R.string.routine_screen_title),
                subtitle = stringResource(R.string.routine_screen_subtitle),
            ) {
                Text(stringResource(R.string.routine_screen_helper))
            }
        }

        item {
            Text(stringResource(R.string.routine_template_heading), color = TextSecondary)
        }
        items(routineTemplates, key = { it.key }) { template ->
            ScaffoldCard(
                title = template.title,
                subtitle = "${template.durationMinutes} min • ${template.xpReward} XP • ${template.triggerMode.name.lowercase()}",
            ) {
                Text(stringResource(R.string.routine_template_target_format, template.targetLabel))
                Text(template.notes, color = TextSecondary, modifier = Modifier.padding(top = 6.dp))
                Button(
                    onClick = { viewModel.createFromTemplate(template, onOpenPlan) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.routine_template_use))
                }
            }
        }

        item {
            Text(stringResource(R.string.routine_plans_heading), color = TextSecondary)
        }
        if (state.plans.isEmpty()) {
            item {
                ScaffoldCard(
                    title = stringResource(R.string.routine_empty_title),
                    subtitle = stringResource(R.string.routine_empty_subtitle),
                ) {}
            }
        } else {
            items(state.plans, key = { it.id }) { plan ->
                RoutinePlanSummaryCard(
                    plan = plan,
                    onOpen = { onOpenPlan(plan.id) },
                    onComplete = { viewModel.completePlan(plan.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinePlanDetailRoute(
    onOpenMission: (String) -> Unit,
    viewModel: RoutinePlanDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val detailLoaded by viewModel.detailLoaded.collectAsStateWithLifecycle()
    val openMissionId by viewModel.openMissionId.collectAsStateWithLifecycle()
    if (!detailLoaded) {
        LoadingStateCard(
            title = stringResource(R.string.routine_detail_loading_title),
            subtitle = stringResource(R.string.routine_detail_loading_subtitle),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
        return
    }
    val plan = detail ?: run {
        EmptyStateCard(
            title = stringResource(R.string.routine_detail_missing_title),
            subtitle = stringResource(R.string.routine_detail_missing_subtitle),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
        return
    }

    LaunchedEffect(openMissionId) {
        openMissionId?.let { missionId ->
            onOpenMission(missionId)
            viewModel.consumeOpenMission()
        }
    }

    var title by remember(plan.id, plan.title) { mutableStateOf(plan.title) }
    var targetLabel by remember(plan.id, plan.targetLabel) { mutableStateOf(plan.targetLabel) }
    var duration by remember(plan.id, plan.durationMinutes) { mutableStateOf(plan.durationMinutes.toString()) }
    var xpReward by remember(plan.id, plan.xpReward) { mutableStateOf(plan.xpReward.toString()) }
    var hour by remember(plan.id, plan.hour) { mutableStateOf(plan.hour.toString().padStart(2, '0')) }
    var minute by remember(plan.id, plan.minute) { mutableStateOf(plan.minute.toString().padStart(2, '0')) }
    var notes by remember(plan.id, plan.notes) { mutableStateOf(plan.notes) }
    var recurring by remember(plan.id, plan.recurring) { mutableStateOf(plan.recurring) }
    var active by remember(plan.id, plan.active) { mutableStateOf(plan.active) }
    var selectedCategory by remember(plan.id, plan.category) { mutableStateOf(plan.category) }
    var selectedTrigger by remember(plan.id, plan.triggerMode) { mutableStateOf(plan.triggerMode) }
    var showDangerZone by rememberSaveable(plan.id) { mutableStateOf(false) }
    val weekdays = remember(plan.id, plan.weekdays) {
        mutableStateListOf<String>().apply {
            addAll(plan.weekdays.split(',').map { it.trim() }.filter { it.isNotBlank() })
        }
    }
    val updatedPlan = plan.copy(
        title = title.trim().ifBlank { plan.title },
        targetLabel = targetLabel.trim().ifBlank { plan.targetLabel },
        durationMinutes = duration.toIntOrNull() ?: plan.durationMinutes,
        xpReward = xpReward.toIntOrNull() ?: plan.xpReward,
        hour = (hour.toIntOrNull() ?: plan.hour).coerceIn(0, 23),
        minute = (minute.toIntOrNull() ?: plan.minute).coerceIn(0, 59),
        notes = notes.trim(),
        recurring = recurring,
        active = active,
        weekdays = weekdays.joinToString(","),
        category = selectedCategory,
        triggerMode = selectedTrigger,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(
                title = title.ifBlank { stringResource(R.string.routine_detail_fallback_title) },
                subtitle = stringResource(
                    R.string.routine_detail_created_format,
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(plan.createdAt)),
                ),
            ) {
                Text(
                    stringResource(
                        R.string.routine_detail_completion_format,
                        plan.completionCount,
                        plan.completionCount * plan.xpReward,
                    ),
                )
                Text(
                    text = if (plan.active) {
                        stringResource(R.string.routine_detail_active)
                    } else {
                        stringResource(R.string.routine_detail_paused)
                    },
                    color = AccentPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                plan.lastCompletedAt?.let {
                    Text(
                        text = stringResource(
                            R.string.routine_detail_last_completed_format,
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)),
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item {
            ScaffoldCard(
                title = stringResource(R.string.routine_focus_title),
                subtitle = stringResource(R.string.routine_focus_subtitle),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.routine_field_title)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetLabel,
                    onValueChange = { targetLabel = it },
                    label = { Text(stringResource(R.string.routine_field_target)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.routine_field_notes)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    minLines = 3,
                )
                Button(
                    onClick = { viewModel.savePlan(updatedPlan) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.routine_save_action))
                }
            }
        }

        item {
            ScaffoldCard(
                title = stringResource(R.string.routine_schedule_title),
                subtitle = stringResource(R.string.routine_schedule_subtitle),
            ) {
                RowWithSpacing {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.routine_field_minutes)) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = xpReward,
                        onValueChange = { xpReward = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.routine_field_xp)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                RowWithSpacing(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.routine_field_hour)) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.routine_field_minute)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                PreferenceSwitchRow(
                    title = stringResource(R.string.routine_recurring_title),
                    checked = recurring,
                    onCheckedChange = { recurring = it },
                    supportingText = stringResource(R.string.routine_recurring_support),
                    modifier = Modifier.padding(top = 12.dp),
                )
                PreferenceSwitchRow(
                    title = stringResource(R.string.routine_active_title),
                    checked = active,
                    onCheckedChange = { active = it },
                    supportingText = stringResource(R.string.routine_active_support),
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(stringResource(R.string.routine_weekdays_title), modifier = Modifier.padding(top = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    weekdayOptions.forEach { day ->
                        FilterChip(
                            selected = day in weekdays,
                            onClick = {
                                if (day in weekdays) weekdays.remove(day) else weekdays.add(day)
                            },
                            label = { Text(day) },
                        )
                    }
                }
            }
        }

        item {
            ScaffoldCard(
                title = stringResource(R.string.routine_logic_title),
                subtitle = stringResource(R.string.routine_logic_subtitle),
            ) {
                Text(stringResource(R.string.routine_category_title))
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoutineCategory.entries.forEach { category ->
                        FilterChip(
                            selected = category == selectedCategory,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name.lowercase()) },
                        )
                    }
                }

                Text(stringResource(R.string.routine_trigger_title), modifier = Modifier.padding(top = 12.dp))
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoutineTriggerMode.entries.forEach { trigger ->
                        FilterChip(
                            selected = trigger == selectedTrigger,
                            onClick = { selectedTrigger = trigger },
                            label = { Text(trigger.name.lowercase()) },
                        )
                    }
                }
                InlineSupportText(
                    text = stringResource(
                        R.string.routine_logic_summary_format,
                        selectedTrigger.name.lowercase(),
                        selectedCategory.name.lowercase(),
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            ScaffoldCard(
                title = stringResource(R.string.routine_actions_title),
                subtitle = stringResource(R.string.routine_actions_subtitle),
            ) {
                Button(
                    onClick = viewModel::completePlan,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.routine_complete_action))
                }
                OutlinedButton(
                    onClick = viewModel::createQuest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text(stringResource(R.string.routine_create_quest))
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showDangerZone = !showDangerZone },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (showDangerZone) {
                            R.string.routine_danger_hide
                        } else {
                            R.string.routine_danger_show
                        },
                    ),
                )
            }
        }

        if (showDangerZone) {
            item {
                ScaffoldCard(
                    title = stringResource(R.string.routine_danger_title),
                    subtitle = stringResource(R.string.routine_danger_subtitle),
                ) {
                    DestructiveOutlinedButton(
                        label = stringResource(R.string.routine_delete_action),
                        onClick = viewModel::deletePlan,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutinePlanSummaryCard(
    plan: RoutinePlanSummary,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
) {
    ScaffoldCard(
        title = plan.title,
        subtitle = "${plan.durationMinutes} min • ${plan.xpReward} XP • ${plan.weekdays.ifBlank { "one-off" }} @ ${plan.hour.toString().padStart(2, '0')}:${plan.minute.toString().padStart(2, '0')}",
    ) {
        AssistChip(onClick = {}, label = { Text(plan.category.name.lowercase()) })
        Text(
            text = stringResource(
                R.string.routine_summary_trigger_format,
                plan.triggerMode.name.lowercase(),
                plan.completionCount,
            ),
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = if (plan.active) {
                stringResource(R.string.routine_summary_active)
            } else {
                stringResource(R.string.routine_summary_paused)
            },
            color = AccentPrimary,
            modifier = Modifier.padding(top = 6.dp),
        )
        RowWithSpacing(modifier = Modifier.padding(top = 12.dp)) {
            Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.routine_summary_open))
            }
            OutlinedButton(onClick = onComplete, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.routine_summary_complete))
            }
        }
    }
}

@Composable
private fun RowWithSpacing(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

data class RoutinePlannerUiState(
    val plans: List<RoutinePlanSummary> = emptyList(),
)

@HiltViewModel
class RoutinePlannerViewModel @Inject constructor(
    private val routinePlanRepository: RoutinePlanRepository,
) : ViewModel() {
    val state: StateFlow<RoutinePlannerUiState> =
        routinePlanRepository.observePlans()
            .map { plans -> RoutinePlannerUiState(plans) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinePlannerUiState())

    fun createFromTemplate(template: RoutineTemplate, onOpenPlan: (String) -> Unit) {
        viewModelScope.launch {
            val detail = RoutinePlanDetail(
                id = UUID.randomUUID().toString(),
                templateKey = template.key,
                title = template.title,
                category = template.category,
                createdAt = System.currentTimeMillis(),
                durationMinutes = template.durationMinutes,
                recurring = true,
                weekdays = "Mon,Wed,Fri",
                hour = 9,
                minute = 0,
                xpReward = template.xpReward,
                triggerMode = template.triggerMode,
                targetLabel = template.targetLabel,
                notes = template.notes,
                active = true,
                completionCount = 0,
                lastCompletedAt = null,
            )
            routinePlanRepository.upsertPlan(detail)
            onOpenPlan(detail.id)
        }
    }

    fun completePlan(planId: String) {
        viewModelScope.launch {
            routinePlanRepository.completePlan(planId)
        }
    }
}

@HiltViewModel
class RoutinePlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routinePlanRepository: RoutinePlanRepository,
    private val missionRepository: MissionRepository,
) : ViewModel() {
    private val planId: String = checkNotNull(savedStateHandle["planId"])
    private val _openMissionId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val openMissionId: StateFlow<String?> = _openMissionId

    val detail: StateFlow<RoutinePlanDetail?> =
        routinePlanRepository.observePlan(planId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val detailLoaded: StateFlow<Boolean> =
        routinePlanRepository.observePlan(planId)
            .map { true }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun savePlan(detail: RoutinePlanDetail) {
        viewModelScope.launch {
            routinePlanRepository.upsertPlan(detail)
        }
    }

    fun completePlan() {
        viewModelScope.launch {
            routinePlanRepository.completePlan(planId)
        }
    }

    fun deletePlan() {
        viewModelScope.launch {
            routinePlanRepository.deletePlan(planId)
        }
    }

    fun createQuest() {
        viewModelScope.launch {
            _openMissionId.value = missionRepository.createQuestFromRoutinePlan(planId)
        }
    }

    fun consumeOpenMission() {
        _openMissionId.value = null
    }
}
