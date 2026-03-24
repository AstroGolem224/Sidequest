package com.astrogolem.sidequest.feature.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
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
                subtitle = "A lighter-weight command center view over your active missions.",
            ) {
                Text("Open missions: ${state.openCount}")
                Text("Completed missions: ${state.doneCount}")
            }
        }
        items(state.spotlight, key = { it.id }) { mission ->
            ScaffoldCard(title = mission.title, subtitle = "Priority ${mission.priorityScore}") {
                Text(mission.description)
            }
        }
    }
}

data class LobbyUiState(
    val openCount: Int = 0,
    val doneCount: Int = 0,
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
                    spotlight = missions.take(3),
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LobbyUiState())
}
