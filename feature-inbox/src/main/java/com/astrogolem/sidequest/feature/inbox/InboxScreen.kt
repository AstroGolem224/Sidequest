package com.astrogolem.sidequest.feature.inbox

import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.ExtractionCandidate
import com.astrogolem.sidequest.core.data.repo.CaptureRepository
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun InboxRoute(viewModel: InboxViewModel = hiltViewModel()) {
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(candidates, key = { it.id }) { candidate ->
            ScaffoldCard(
                title = candidate.title,
                subtitle = "Confidence ${(candidate.confidence * 100).toInt()}%",
            ) {
                Text(candidate.body)
                Button(
                    onClick = { viewModel.promote(candidate.id) },
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text("Promote to Mission")
                }
            }
        }
    }
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    captureRepository: CaptureRepository,
    private val missionRepository: MissionRepository,
) : ViewModel() {
    val candidates: StateFlow<List<ExtractionCandidate>> =
        captureRepository.observeCandidates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun promote(candidateId: String) {
        viewModelScope.launch {
            missionRepository.promoteCandidate(candidateId)
        }
    }
}
