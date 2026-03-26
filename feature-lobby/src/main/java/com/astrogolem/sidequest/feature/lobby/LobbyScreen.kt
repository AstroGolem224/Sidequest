package com.astrogolem.sidequest.feature.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.ui.components.ScaffoldCard
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.CardSurfaceStrong
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
    val totalGp = (state.doneCount * 35 + state.openCount * 8).coerceAtLeast(125)
    val level = (totalGp / 180) + 1
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Surface(
                    color = BgGlow,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("L$level", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = AccentSecondary)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("ASTRAEA_VOID", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
                    Text("GRAND EXPLORER • SEASON 4", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = AccentPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        MetricStack("QUESTS", state.doneCount.toString())
                        MetricStack("TOTAL GP", totalGp.toString())
                        MetricStack("RANK", "#${402 - state.doneCount.coerceAtMost(200)}")
                    }
                }
            }
        }

        item {
            Text("CHARACTER ATTRIBUTES", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = TextSecondary)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AttributeCard("Productivity", (state.doneCount * 12 + 40).coerceAtMost(95), "PR")
                AttributeCard("Wellness", (state.openCount * 8 + 30).coerceAtMost(88), "WL")
                AttributeCard("Knowledge", (state.totalCount * 10 + 34).coerceAtMost(96), "KN")
                AttributeCard("Discipline", (state.streakDays * 11 + 22).coerceAtMost(92), "DS")
            }
        }

        item {
            ScaffoldCard(
                title = "THE DIGITAL DETOX ABYSS",
                subtitle = "Current protocol derived from your live objective state.",
            ) {
                Text(
                    "Limit overload and focus on the active queue. Sidequest should feel like a calm command bridge, not an alarm panel.",
                    color = TextSecondary,
                )
                Text(
                    "${state.spotlight.size}/3 focus slots occupied",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = AccentSecondary,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(6.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                        .background(CardSurfaceStrong),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((state.spotlight.size / 3f).coerceIn(0.1f, 1f))
                            .height(6.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                            .background(AccentPrimary),
                    )
                }
            }
        }

        item {
            Text("COLLECTED LOOT", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = TextSecondary)
        }

        items(state.spotlight, key = { it.id }) { mission ->
            ScaffoldCard(title = mission.title, subtitle = "Priority ${mission.priorityScore}") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RewardTile("Relic", mission.title.take(24))
                    RewardTile("Intel", "${mission.priorityScore} score")
                }
                Text(mission.description, modifier = Modifier.padding(top = 12.dp), color = TextSecondary)
            }
        }
    }
}

@Composable
private fun MetricStack(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = TextSecondary)
        Text(value, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = AccentSecondary)
    }
}

@Composable
private fun AttributeCard(
    label: String,
    percentage: Int,
    sigil: String,
) {
    ScaffoldCard(title = label, subtitle = "$percentage%") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Surface(
                color = AccentPrimary.copy(alpha = 0.12f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = sigil,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = AccentPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
            Text("$percentage%", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = AccentSecondary)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                .background(CardSurfaceStrong),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percentage / 100f).coerceIn(0.08f, 1f))
                    .height(6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    .background(AccentPrimary),
            )
        }
    }
}

@Composable
private fun RowScope.RewardTile(
    label: String,
    value: String,
) {
    Surface(
        color = BgGlow,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.weight(1f),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label.uppercase(), style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = AccentPrimary)
            Text(value, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
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
