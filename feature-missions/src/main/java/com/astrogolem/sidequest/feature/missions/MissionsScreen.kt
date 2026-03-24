package com.astrogolem.sidequest.feature.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.MissionAction
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun MissionsRoute(viewModel: MissionsViewModel = hiltViewModel()) {
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(missions, key = { it.id }) { mission ->
            ScaffoldCard(
                title = mission.title,
                subtitle = "Priority ${mission.priorityScore} • ${mission.status.name.lowercase()}",
            ) {
                Text(mission.description)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Button(onClick = { viewModel.complete(mission.id) }) { Text("Complete") }
                    Button(onClick = { viewModel.archive(mission.id) }) { Text("Archive") }
                }
            }
        }
    }
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

    fun archive(missionId: String) {
        viewModelScope.launch { missionRepository.applyAction(MissionAction.Archive(missionId)) }
    }
}
