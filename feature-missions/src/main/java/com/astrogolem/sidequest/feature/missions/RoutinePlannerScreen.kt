package com.astrogolem.sidequest.feature.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.astrogolem.sidequest.core.data.repo.RoutinePlanRepository
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
                title = "Routine Tasks",
                subtitle = "Template-based repeatable tasks with duration, weekdays, time, XP reward, and editable completion logic.",
            ) {
                Text("Use templates as a starting point, then edit every field. Only completion earns XP, never creation.")
            }
        }

        item {
            Text("Template Catalog", color = TextSecondary)
        }
        items(routineTemplates, key = { it.key }) { template ->
            ScaffoldCard(
                title = template.title,
                subtitle = "${template.durationMinutes} min • ${template.xpReward} XP • ${template.triggerMode.name.lowercase()}",
            ) {
                Text("Target: ${template.targetLabel}")
                Text(template.notes, color = TextSecondary, modifier = Modifier.padding(top = 6.dp))
                Button(
                    onClick = { viewModel.createFromTemplate(template, onOpenPlan) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Use Template")
                }
            }
        }

        item {
            Text("Your Plans", color = TextSecondary)
        }
        if (state.plans.isEmpty()) {
            item {
                ScaffoldCard(
                    title = "No routine plans yet",
                    subtitle = "Pick a template, then adjust schedule, weekdays, trigger mode, and XP to match real life.",
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
    viewModel: RoutinePlanDetailViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val plan = detail ?: return

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
    val weekdays = remember(plan.id, plan.weekdays) {
        mutableStateListOf<String>().apply {
            addAll(plan.weekdays.split(',').map { it.trim() }.filter { it.isNotBlank() })
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(
                title = title.ifBlank { "Routine Plan" },
                subtitle = "Created ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(plan.createdAt))}",
            ) {
                Text("Completions: ${plan.completionCount} • Earned XP: ${plan.completionCount * plan.xpReward}")
                Text(
                    text = if (plan.active) {
                        "Routine reminders are active and will fire on the next matching schedule."
                    } else {
                        "Routine reminders are paused until you reactivate this plan."
                    },
                    color = AccentPrimary,
                    modifier = Modifier.padding(top = 6.dp),
                )
                plan.lastCompletedAt?.let {
                    Text(
                        text = "Last completed ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))}",
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item {
            ScaffoldCard(
                title = "Edit Routine",
                subtitle = "Title, scope, schedule, trigger mode, recurrence, and XP are all editable.",
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetLabel,
                    onValueChange = { targetLabel = it },
                    label = { Text("Target / what exactly?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter(Char::isDigit) },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = xpReward,
                        onValueChange = { xpReward = it.filter(Char::isDigit) },
                        label = { Text("XP") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Hour") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Minute") },
                        modifier = Modifier.weight(1f),
                    )
                }

                Text("Recurring", modifier = Modifier.padding(top = 12.dp))
                Switch(checked = recurring, onCheckedChange = { recurring = it })

                Text("Active", modifier = Modifier.padding(top = 12.dp))
                Switch(checked = active, onCheckedChange = { active = it })

                Text("Weekdays", modifier = Modifier.padding(top = 12.dp))
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

                Text("Category", modifier = Modifier.padding(top = 12.dp))
                FlowRow(
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

                Text("Trigger", modifier = Modifier.padding(top = 12.dp))
                FlowRow(
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

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / execution plan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    minLines = 3,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            viewModel.savePlan(
                                plan.copy(
                                    title = title.trim(),
                                    targetLabel = targetLabel.trim(),
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
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = viewModel::completePlan,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Complete")
                    }
                }
                OutlinedButton(
                    onClick = viewModel::deletePlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) {
                    Text("Delete Plan")
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
            text = "Trigger: ${plan.triggerMode.name.lowercase()} • completions: ${plan.completionCount}",
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = if (plan.active) "Reminder armed for next schedule." else "Reminder paused.",
            color = AccentPrimary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onOpen, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
            OutlinedButton(onClick = onComplete, modifier = Modifier.weight(1f)) {
                Text("Complete")
            }
        }
    }
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
) : ViewModel() {
    private val planId: String = checkNotNull(savedStateHandle["planId"])

    val detail: StateFlow<RoutinePlanDetail?> =
        routinePlanRepository.observePlan(planId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}
