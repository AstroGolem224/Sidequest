package com.astrogolem.sidequest

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astrogolem.sidequest.core.data.repo.ProcessingOrchestrator
import com.astrogolem.sidequest.core.data.repo.SecurityService
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.AccentCyan
import com.astrogolem.sidequest.core.ui.theme.BgPrimary
import com.astrogolem.sidequest.core.ui.theme.BgPanel
import com.astrogolem.sidequest.core.ui.theme.SidequestTheme
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import com.astrogolem.sidequest.feature.capture.CaptureDetailRoute
import com.astrogolem.sidequest.feature.capture.CaptureRoute
import com.astrogolem.sidequest.feature.inbox.InboxRoute
import com.astrogolem.sidequest.feature.lobby.LobbyRoute
import com.astrogolem.sidequest.feature.missions.MissionDetailRoute
import com.astrogolem.sidequest.feature.missions.MissionsRoute
import com.astrogolem.sidequest.feature.search.SearchRoute
import com.astrogolem.sidequest.feature.settings.SettingsRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val MissionIdExtra = "mission_id"
private const val CaptureIdExtra = "capture_id"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var securityService: SecurityService
    @Inject
    lateinit var processingOrchestrator: ProcessingOrchestrator

    private var pendingDeepLink by mutableStateOf<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLink = intent.toDeepLinkTarget()
        enableEdgeToEdge()
        setContent {
            SidequestTheme {
                SidequestApp(
                    activity = this,
                    securityService = securityService,
                    processingOrchestrator = processingOrchestrator,
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkConsumed = { pendingDeepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.toDeepLinkTarget()
    }
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private sealed interface DeepLinkTarget {
    data class Mission(val missionId: String) : DeepLinkTarget
    data class Capture(val captureId: String) : DeepLinkTarget
}

@Composable
private fun SidequestApp(
    activity: AppCompatActivity,
    securityService: SecurityService,
    processingOrchestrator: ProcessingOrchestrator,
    pendingDeepLink: DeepLinkTarget?,
    onDeepLinkConsumed: () -> Unit,
) {
    val chromeViewModel: AppChromeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val chrome by chromeViewModel.state.collectAsStateWithLifecycle()
    val biometricEnabled by produceState<Boolean?>(initialValue = null) {
        value = securityService.isBiometricLockEnabled()
    }
    var unlocked by remember { mutableStateOf(false) }

    if (biometricEnabled == true && !unlocked) {
        BiometricGate(activity = activity, onUnlocked = { unlocked = true })
        return
    }

    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination("missions", "Dashboard", SidequestIcons.Dashboard),
        TopLevelDestination("inbox", "Quests", SidequestIcons.QuestLog),
        TopLevelDestination("capture", "Create", SidequestIcons.Camera),
        TopLevelDestination("lobby", "Profile", SidequestIcons.Profile),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val utilityRoutes = setOf("search", "settings")
    val showBottomBar = destinations.any { it.route == currentRoute }
    val showTopBar = showBottomBar || currentRoute in utilityRoutes
    val topBarTitle = when (currentRoute) {
        "missions" -> "Quest Log"
        "inbox" -> "Quest Intake"
        "capture" -> "Create"
        "lobby" -> "Profile"
        "search" -> "Search"
        "settings" -> "Settings"
        else -> "Sidequest"
    }

    LaunchedEffect(pendingDeepLink) {
        when (val deepLink = pendingDeepLink) {
            is DeepLinkTarget.Mission -> navController.navigate("mission/${deepLink.missionId}")
            is DeepLinkTarget.Capture -> navController.navigate("captureDetail/${deepLink.captureId}")
            null -> Unit
        }
        if (pendingDeepLink != null) {
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(Unit) {
        processingOrchestrator.recoverPendingCaptures()
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                SidequestTopBar(
                    title = topBarTitle,
                    gp = chrome.totalGp,
                    level = chrome.level,
                    subtitle = chrome.title,
                    showBack = currentRoute in utilityRoutes,
                    onBack = { navController.popBackStack() },
                    onSearch = {
                        if (currentRoute != "search") {
                            navController.navigate("search")
                        }
                    },
                    onSettings = {
                        if (currentRoute != "settings") {
                            navController.navigate("settings")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = BgPanel.copy(alpha = 0.96f)) {
                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text(destination.label) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BgPrimary,
                                selectedTextColor = AccentSecondary,
                                indicatorColor = AccentPrimary.copy(alpha = 0.18f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "missions",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("missions") {
                TopLevelScreenContainer {
                    MissionsRoute(
                        onOpenMission = { missionId -> navController.navigate("mission/$missionId") },
                        onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") },
                    )
                }
            }
            composable("mission/{missionId}") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MissionDetailRoute(onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") })
                }
            }
            composable("capture") {
                CaptureRoute(onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") })
            }
            composable("captureDetail/{captureId}") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CaptureDetailRoute(
                        onOpenMission = { missionId -> navController.navigate("mission/$missionId") },
                        onDeleted = { navController.popBackStack() },
                    )
                }
            }
            composable("inbox") {
                TopLevelScreenContainer {
                    InboxRoute(onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") })
                }
            }
            composable("lobby") {
                TopLevelScreenContainer {
                    LobbyRoute()
                }
            }
            composable("search") {
                TopLevelScreenContainer {
                    SearchRoute(onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") })
                }
            }
            composable("settings") {
                TopLevelScreenContainer {
                    SettingsRoute()
                }
            }
        }
    }
}

@Composable
private fun TopLevelScreenContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AccentPrimary.copy(alpha = 0.16f), AccentSecondary.copy(alpha = 0.05f), BgPrimary),
                    radius = 1800f,
                ),
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.86f),
        ) {
            content()
        }
    }
}

@Composable
private fun SidequestTopBar(
    title: String,
    gp: Int,
    level: Int,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        color = BgPanel.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = SidequestIcons.Back,
                            contentDescription = "Back",
                            tint = AccentSecondary,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("SIDEQUEST", style = MaterialTheme.typography.titleSmall, color = AccentPrimary)
                    Text(
                        text = if (showBack) title else subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.18f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = SidequestIcons.Coin,
                            contentDescription = "GP",
                            tint = AccentPrimary,
                        )
                        Text("$gp GP", style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
                    }
                }
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = SidequestIcons.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                    )
                }
                Surface(
                    color = AccentPrimary.copy(alpha = 0.14f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                ) {
                    IconButton(onClick = onSettings) {
                        Text("L$level", style = MaterialTheme.typography.titleSmall, color = AccentSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BiometricGate(
    activity: AppCompatActivity,
    onUnlocked: () -> Unit,
) {
    var message by remember { mutableStateOf("Authenticate to unlock Sidequest.") }

    fun authenticate() {
        val canAuthenticate = BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            message = "Biometric lock is enabled, but no supported authenticator is available."
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    message = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    message = "Authentication failed. Try again."
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Sidequest")
                .setSubtitle("Your local-first vault is protected on this device.")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build(),
        )
    }

    LaunchedEffect(Unit) {
        authenticate()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Sidequest Locked")
            Text(message)
            Button(onClick = { authenticate() }) {
                Text("Try again")
            }
        }
    }
}

private fun Intent?.toDeepLinkTarget(): DeepLinkTarget? {
    val intent = this ?: return null
    intent.getStringExtra(MissionIdExtra)?.let { return DeepLinkTarget.Mission(it) }
    intent.getStringExtra(CaptureIdExtra)?.let { return DeepLinkTarget.Capture(it) }
    return null
}
