package com.astrogolem.sidequest.feature.lobby

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.astrogolem.sidequest.core.data.model.MissionCardModel
import com.astrogolem.sidequest.core.data.repo.MissionRepository
import com.astrogolem.sidequest.core.data.repo.SecurityService
import com.astrogolem.sidequest.core.ui.components.GlassCard
import com.astrogolem.sidequest.core.ui.components.HudRing
import com.astrogolem.sidequest.core.ui.components.HudTone
import com.astrogolem.sidequest.core.ui.components.SegmentedMeter
import com.astrogolem.sidequest.core.ui.components.StatusPill
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgGlow
import com.astrogolem.sidequest.core.ui.theme.BgPanel
import com.astrogolem.sidequest.core.ui.theme.CardStroke
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
fun LobbyRoute(
    onOpenStats: () -> Unit,
    viewModel: LobbyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteAvatarDialog by remember { mutableStateOf(false) }
    var milestonesExpanded by rememberSaveable { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveAvatar(uri)
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            ProfileHero(
                state = state,
                onPickAvatar = {
                    avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClearAvatar = { showDeleteAvatarDialog = true },
            )
        }

        item {
            StatsEntryCard(onOpenStats = onOpenStats)
        }

        item { ProgressSnapshotCard(state) }

        item {
            LobbyCollapsibleSectionHeader(
                title = "Milestones",
                subtitle = "Unlocks tied to real usage and completion history.",
                expanded = milestonesExpanded,
                onToggle = { milestonesExpanded = !milestonesExpanded },
            )
        }

        if (milestonesExpanded) {
            items(state.loot.chunked(2)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { loot ->
                        LootCard(loot = loot, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
    if (showDeleteAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAvatarDialog = false },
            title = { Text("Delete avatar?") },
            text = { Text("This removes the local profile image and restores the default profile glyph.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAvatarDialog = false
                        viewModel.clearAvatar()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAvatarDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun LobbyCollapsibleSectionHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        SectionTitle(title, subtitle)
        OutlinedIconButton(
            onClick = onToggle,
            border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.9f)),
            colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = AccentSecondary),
        ) {
            Icon(
                imageVector = if (expanded) SidequestIcons.Minus else SidequestIcons.Plus,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            )
        }
    }
}

@Composable
private fun ProfileHero(
    state: LobbyUiState,
    onPickAvatar: () -> Unit,
    onClearAvatar: () -> Unit,
) {
    val animatedXpProgress = animateFloatAsState(
        targetValue = state.xpProgress,
        animationSpec = tween(durationMillis = 520),
        label = "profile-xp-progress",
    )
    GlassCard(
        title = "Profile",
        subtitle = state.rankTitle.uppercase(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(104.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                HudRing(
                    progress = animatedXpProgress.value,
                    tone = HudTone.Amber,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    color = BgGlow,
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp),
                ) {
                    AvatarCore(
                        avatarImagePath = state.avatarImagePath,
                        level = state.level,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(
                    text = "${state.streakDays}-day streak",
                    tone = HudTone.Violet,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AvatarActionButton(
                        icon = SidequestIcons.Gallery,
                        contentDescription = if (state.avatarImagePath == null) "Choose avatar" else "Change avatar",
                        onClick = onPickAvatar,
                    )
                    if (state.avatarImagePath != null) {
                        AvatarActionButton(
                            icon = SidequestIcons.Delete,
                            contentDescription = "Delete avatar",
                            onClick = onClearAvatar,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MetricStack("Collected", state.totalCount.toString(), Modifier.weight(1f))
            MetricStack("Completed", state.doneCount.toString(), Modifier.weight(1f))
            MetricStack("Streak", state.streakDays.toString(), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        SegmentedMeter(
            progress = animatedXpProgress.value,
            tone = HudTone.Amber,
        )

        Text(
            "${state.xpCurrent}/180 xp synced to next level",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun StatsEntryCard(
    onOpenStats: () -> Unit,
) {
    GlassCard(
        title = "Stats",
        subtitle = "Open the analytics surface when you want metric explanations, not when you are just checking identity or progress.",
    ) {
        OutlinedButton(
            onClick = onOpenStats,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Stats")
        }
    }
}

@Composable
private fun AvatarActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = AccentPrimary,
        ),
        border = BorderStroke(1.dp, CardStroke.copy(alpha = 0.9f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AvatarCore(
    avatarImagePath: String?,
    level: Int,
) {
    val bitmap = remember(avatarImagePath) {
        avatarImagePath?.let(BitmapFactory::decodeFile)
    }
    Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = SidequestIcons.Profile,
                    contentDescription = "Profile",
                    tint = AccentPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "LV $level",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = AccentSecondary,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title.uppercase(), style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = TextSecondary)
        Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun MetricStack(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = TextSecondary)
        Text(value, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = AccentSecondary)
    }
}

@Composable
private fun ProgressSnapshotCard(state: LobbyUiState) {
    GlassCard(
        title = "Progress Snapshot",
        subtitle = "Profile tracks identity, level, and momentum. Archived assets and deep analytics now live elsewhere.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricStack("Level", state.level.toString(), Modifier.weight(1f))
            MetricStack("Active", state.openCount.toString(), Modifier.weight(1f))
            MetricStack("Resolved", state.doneCount.toString(), Modifier.weight(1f))
        }

        Text(
            text = "XP only lands on completion, so this surface reflects real follow-through instead of queue size. Use Inventory for saved assets and Stats for metric breakdowns.",
            color = TextSecondary,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun LootCard(
    loot: LootCardModel,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        title = loot.title,
        subtitle = loot.subtitle,
        modifier = modifier.heightIn(min = 220.dp),
    ) {
        Surface(
            color = AccentPrimary.copy(alpha = if (loot.unlocked) 0.14f else 0.08f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                Icon(
                    imageVector = loot.icon,
                    contentDescription = loot.title,
                    tint = if (loot.unlocked) AccentPrimary else TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Spacer(modifier = Modifier.weight(1f, fill = true))
        StatusPill(
            text = if (loot.unlocked) loot.rarity else "locked",
            tone = if (loot.unlocked) HudTone.Amber else HudTone.Neutral,
        )
    }
}

data class LootCardModel(
    val title: String,
    val subtitle: String,
    val rarity: String,
    val unlocked: Boolean,
    val icon: ImageVector,
)

data class LobbyUiState(
    val openCount: Int = 0,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val streakDays: Int = 0,
    val totalGp: Int = 125,
    val level: Int = 1,
    val xpCurrent: Int = 0,
    val xpProgress: Float = 0.1f,
    val rankLabel: String = "",
    val rankTitle: String = "Ready",
    val avatarImagePath: String? = null,
    val systemOverload: Boolean = false,
    val loot: List<LootCardModel> = emptyList(),
)

@HiltViewModel
class LobbyViewModel @Inject constructor(
    missionRepository: MissionRepository,
    private val securityService: SecurityService,
) : ViewModel() {
    val state: StateFlow<LobbyUiState> =
        combine(
            missionRepository.observeMissions(),
            securityService.observeUserPreferences(),
        ) { missions, userPreferences ->
                val openCount = missions.count { it.status.name == "OPEN" || it.status.name == "ACTIVE" }
                val doneCount = missions.count { it.status.name == "DONE" }
                val totalCount = missions.size
                val totalGp = missions.sumOf(::profileRewardGp).coerceAtLeast(125)
                val xpCurrent = totalGp % 180
                LobbyUiState(
                    openCount = openCount,
                    doneCount = doneCount,
                    totalCount = totalCount,
                    streakDays = doneCount.coerceAtLeast(if (totalCount > 0) 1 else 0),
                    totalGp = totalGp,
                    level = (totalGp / 180) + 1,
                    xpCurrent = xpCurrent,
                    xpProgress = (xpCurrent / 180f).coerceIn(0.1f, 1f),
                    rankLabel = doneCount.toString(),
                    rankTitle = rankTitle(doneCount),
                    avatarImagePath = userPreferences.avatarImagePath,
                    systemOverload = openCount > 10,
                    loot = buildLoot(doneCount, totalCount, openCount),
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LobbyUiState())

    fun saveAvatar(uri: Uri) {
        viewModelScope.launch {
            securityService.saveAvatarImage(uri)
        }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            securityService.clearAvatarImage()
        }
    }
}

private fun buildLoot(
    doneCount: Int,
    totalCount: Int,
    openCount: Int,
): List<LootCardModel> {
    return listOf(
        LootCardModel(
            title = "First Capture",
            subtitle = if (totalCount > 0) "At least one scan stored locally" else "Run your first capture",
            rarity = "Rare",
            unlocked = totalCount > 0,
            icon = SidequestIcons.Camera,
        ),
        LootCardModel(
            title = "First Completion",
            subtitle = if (doneCount > 0) "At least one mission completed" else "Complete one mission",
            rarity = "Epic",
            unlocked = doneCount > 0,
            icon = SidequestIcons.Spark,
        ),
        LootCardModel(
            title = "Archive Depth",
            subtitle = if (totalCount >= 10) "Ten or more scans retained" else "Reach 10 saved scans",
            rarity = "Rare",
            unlocked = totalCount >= 10,
            icon = SidequestIcons.Intel,
        ),
        LootCardModel(
            title = "Healthy Queue",
            subtitle = if (openCount in 1..5) "Healthy active load maintained" else "Keep 1-5 active missions",
            rarity = "Legendary",
            unlocked = openCount in 1..5,
            icon = SidequestIcons.Dashboard,
        ),
    )
}

private fun rankTitle(doneCount: Int): String {
    return when {
        doneCount >= 40 -> "Established"
        doneCount >= 20 -> "Consistent"
        doneCount >= 8 -> "In Motion"
        else -> "Starting Out"
    }
}

private fun profileRewardGp(mission: MissionCardModel): Int {
    val base = (mission.priorityScore / 5).coerceAtLeast(4)
    return when (mission.status.name) {
        "DONE" -> base + 12
        "ACTIVE" -> base / 2 + 2
        else -> base / 2
    }
}
