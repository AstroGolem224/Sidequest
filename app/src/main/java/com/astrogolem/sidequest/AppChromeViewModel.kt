package com.astrogolem.sidequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AppChromeUiState(
    val totalGp: Int = 0,
    val level: Int = 1,
    val title: String = "Astral Navigator",
)

@HiltViewModel
class AppChromeViewModel @Inject constructor(
    missionRepository: MissionRepository,
) : ViewModel() {
    val state: StateFlow<AppChromeUiState> =
        missionRepository.observeMissions()
            .map(::toChromeState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppChromeUiState())
}

private fun toChromeState(missions: List<MissionCardModel>): AppChromeUiState {
    val completed = missions.count { it.status.name == "DONE" }
    val totalGp = missions.sumOf { mission ->
        val base = (mission.priorityScore / 5).coerceAtLeast(4)
        if (mission.status.name == "DONE") base + 12 else base / 2
    }.coerceAtLeast(0)
    val level = (totalGp / 180).coerceAtLeast(0) + 1
    val title = when {
        completed >= 40 -> "Grand Explorer"
        completed >= 20 -> "Stellar Navigator"
        completed >= 8 -> "Quest Pilot"
        else -> "Field Operative"
    }
    return AppChromeUiState(
        totalGp = totalGp,
        level = level,
        title = title,
    )
}
