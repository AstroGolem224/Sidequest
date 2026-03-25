package com.astrogolem.sidequest.feature.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.components.HudRing
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.components.SegmentedMeter
import com.astrogolem.sidequest.core.ui.components.StatusPill
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
fun LobbyRoute(viewModel: LobbyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScaffoldCard(
                title = "Life Lobby",
                subtitle = "Psychological wrapper over your live objective state.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    HudRing(
                        progress = if (state.totalCount == 0) 0.1f else state.doneCount / state.totalCount.toFloat(),
                        modifier = Modifier.weight(0.35f),
                        tone = HudTone.Amber,
                    )
                    Column(
                        modifier = Modifier.weight(0.65f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusPill("streak ${state.streakDays} days", tone = HudTone.Amber)
                        Text("Open missions: ${state.openCount}")
                        Text("Completed missions: ${state.doneCount}", color = TextSecondary)
                        SegmentedMeter(progress = if (state.totalCount == 0) 0.1f else state.doneCount / state.totalCount.toFloat())
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                ScaffoldCard(
                    title = "System Load",
                    subtitle = if (state.openCount > 10) "Warning: system overload" else "Operational",
                    modifier = Modifier.weight(1f),
                ) {
                    Text("${state.openCount} live")
                }
                ScaffoldCard(
                    title = "Focus Slots",
                    subtitle = "Top 3 missions pinned",
                    modifier = Modifier.weight(1f),
                ) {
                    Text("${state.spotlight.size} occupied")
                }
            }
        }
        items(state.spotlight, key = { it.id }) { mission ->
            ScaffoldCard(title = mission.title, subtitle = "Priority ${mission.priorityScore}") {
                StatusPill("focus", tone = HudTone.Cyan)
                Text(mission.description, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

data class LobbyUiState(
    val openCount: Int = 0,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val streakDays: Int = 0,
    val spotlight: List<MissionCardModel> = emptyList(),
)

@HiltViewModel
class LobbyViewModel @Inject constructor(
    missionRepository: MissionRepository,
) : ViewModel() {
    val state: StateFlow<LobbyUiState> =
        missionRepository.observeMissions()
            .map { missions ->
                LobbyUiState(
                    openCount = missions.count { it.status.name == "OPEN" || it.status.name == "ACTIVE" },
                    doneCount = missions.count { it.status.name == "DONE" },
                    totalCount = missions.size,
                    streakDays = missions.count { it.status.name == "DONE" }.coerceAtLeast(1),
                    spotlight = missions.take(3),
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LobbyUiState())
}
