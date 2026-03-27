package com.astrogolem.sidequest

import android.content.Intent
import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astrogolem.sidequest.core.data.repo.ProcessingOrchestrator
import com.astrogolem.sidequest.core.data.repo.SecurityService
import com.astrogolem.sidequest.core.data.model.UserPreferences
import com.astrogolem.sidequest.core.ui.icons.SidequestIcons
import com.astrogolem.sidequest.core.ui.theme.AccentPrimary
import com.astrogolem.sidequest.core.ui.theme.AccentSecondary
import com.astrogolem.sidequest.core.ui.theme.BgElevated
import com.astrogolem.sidequest.core.ui.theme.BgPrimary
import com.astrogolem.sidequest.core.ui.theme.BgPanel
import com.astrogolem.sidequest.core.ui.theme.SidequestTheme
import com.astrogolem.sidequest.core.ui.theme.ThemePreset
import com.astrogolem.sidequest.core.ui.theme.TextSecondary
import com.astrogolem.sidequest.feature.capture.CaptureDetailRoute
import com.astrogolem.sidequest.feature.capture.CaptureRoute
import com.astrogolem.sidequest.feature.inbox.InboxRoute
import com.astrogolem.sidequest.feature.lobby.InventoryRoute
import com.astrogolem.sidequest.feature.lobby.LobbyRoute
import com.astrogolem.sidequest.feature.lobby.NoteDetailRoute
import com.astrogolem.sidequest.feature.lobby.StatsRoute
import com.astrogolem.sidequest.feature.missions.MissionDetailRoute
import com.astrogolem.sidequest.feature.missions.MissionsRoute
import com.astrogolem.sidequest.feature.missions.RoutinePlanDetailRoute
import com.astrogolem.sidequest.feature.missions.RoutinePlannerRoute
import com.astrogolem.sidequest.feature.missions.ShoppingDetailRoute
import com.astrogolem.sidequest.feature.missions.ShoppingRoute
import com.astrogolem.sidequest.feature.search.SearchRoute
import com.astrogolem.sidequest.feature.settings.SettingsRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

private const val MissionIdExtra = "mission_id"
private const val CaptureIdExtra = "capture_id"
private const val RoutinePlanIdExtra = "routine_plan_id"

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
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            val userPreferences by securityService.observeUserPreferences().collectAsState(initial = UserPreferences())
            val themePreset = ThemePreset.entries.firstOrNull { it.name == userPreferences.themePresetName } ?: ThemePreset.CYBER
            SidequestTheme(themePreset = themePreset) {
                SidequestApp(
                    activity = this,
                    processingOrchestrator = processingOrchestrator,
                    userPreferences = userPreferences,
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
    data class Routine(val planId: String) : DeepLinkTarget
}

@Composable
private fun SidequestApp(
    activity: AppCompatActivity,
    processingOrchestrator: ProcessingOrchestrator,
    userPreferences: UserPreferences,
    pendingDeepLink: DeepLinkTarget?,
    onDeepLinkConsumed: () -> Unit,
) {
    val chromeViewModel: AppChromeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val chrome by chromeViewModel.state.collectAsStateWithLifecycle()
    var unlocked by remember { mutableStateOf(false) }

    if (userPreferences.biometricLockEnabled && !unlocked) {
        BiometricGate(activity = activity, onUnlocked = { unlocked = true })
        return
    }

    val navController = rememberNavController()
    val destinations = listOf(
        TopLevelDestination("missions", "Dashboard", SidequestIcons.Dashboard),
        TopLevelDestination("inbox", "Quests", SidequestIcons.QuestLog),
        TopLevelDestination("capture", "Create", SidequestIcons.Camera),
        TopLevelDestination("inventory", "Inventory", SidequestIcons.Intel),
        TopLevelDestination("lobby", "Profile", SidequestIcons.Profile),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val utilityRoutes = setOf(
        "search",
        "settings",
        "shopping",
        "shopping/{listId}",
        "routines",
        "routine/{planId}",
        "stats",
        "note/{noteId}",
    )
    val showBottomBar = destinations.any { it.route == currentRoute }
    val showTopBar = showBottomBar || currentRoute in utilityRoutes
    val topBarTitle = when (currentRoute) {
        "missions" -> "Dashboard"
        "inbox" -> "Quest Log"
        "capture" -> "Create"
        "inventory" -> "Inventory"
        "lobby" -> "Profile"
        "shopping" -> "Shopping Lists"
        "shopping/{listId}" -> "Shopping List"
        "routines" -> "Routine Tasks"
        "routine/{planId}" -> "Routine Plan"
        "stats" -> "Stats"
        "note/{noteId}" -> "Note"
        "search" -> "Search"
        "settings" -> "Settings"
        else -> "Sidequest"
    }

    LaunchedEffect(pendingDeepLink) {
        when (val deepLink = pendingDeepLink) {
            is DeepLinkTarget.Mission -> navController.navigate("mission/${deepLink.missionId}")
            is DeepLinkTarget.Capture -> navController.navigate("captureDetail/${deepLink.captureId}")
            is DeepLinkTarget.Routine -> navController.navigate("routine/${deepLink.planId}")
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showTopBar) {
                SidequestTopBar(
                    title = topBarTitle,
                    level = chrome.level,
                    subtitle = chrome.title,
                    avatarImagePath = userPreferences.avatarImagePath,
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
            Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
            navController = navController,
            startDestination = "missions",
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(380)) + scaleIn(initialScale = 0.94f, animationSpec = tween(380))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(340)) + scaleOut(targetScale = 0.94f, animationSpec = tween(340))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(380)) + scaleIn(initialScale = 0.94f, animationSpec = tween(380))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(340)) + scaleOut(targetScale = 0.94f, animationSpec = tween(340))
            },
        ) {
            composable("missions") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    MissionsRoute(
                        onOpenMission = { missionId -> navController.navigate("mission/$missionId") },
                        onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") },
                    )
                }
            }
            composable("mission/{missionId}") {
                ScreenSurfaceFrame {
                    MissionDetailRoute(onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") })
                }
            }
            composable("capture") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    wrapInSurface = false,
                ) {
                    CaptureRoute(onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") })
                }
            }
            composable("captureDetail/{captureId}") {
                ScreenSurfaceFrame {
                    CaptureDetailRoute(
                        onOpenMission = { missionId -> navController.navigate("mission/$missionId") },
                        onDeleted = { navController.popBackStack() },
                    )
                }
            }
            composable("inbox") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    InboxRoute(onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") })
                }
            }
            composable("lobby") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    LobbyRoute(
                        onOpenStats = { navController.navigate("stats") },
                    )
                }
            }
            composable("inventory") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    InventoryRoute(
                        onOpenShoppingHub = { navController.navigate("shopping") },
                        onOpenRoutineHub = { navController.navigate("routines") },
                        onOpenShopping = { listId -> navController.navigate("shopping/$listId") },
                        onOpenRoutine = { planId -> navController.navigate("routine/$planId") },
                        onOpenMission = { missionId -> navController.navigate("mission/$missionId") },
                        onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") },
                        onOpenNote = { noteId -> navController.navigate("note/$noteId") },
                    )
                }
            }
            composable("stats") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    StatsRoute()
                }
            }
            composable("note/{noteId}") {
                ScreenSurfaceFrame {
                    NoteDetailRoute(onOpenMission = { missionId -> navController.navigate("mission/$missionId") })
                }
            }
            composable("shopping") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    ShoppingRoute(onOpenList = { listId -> navController.navigate("shopping/$listId") })
                }
            }
            composable("shopping/{listId}") {
                ScreenSurfaceFrame {
                    ShoppingDetailRoute(
                        onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") },
                        onOpenMission = { missionId -> navController.navigate("mission/$missionId") },
                    )
                }
            }
            composable("search") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    SearchRoute(
                        onOpenCapture = { captureId -> navController.navigate("captureDetail/$captureId") },
                        onOpenMission = { missionId -> navController.navigate("mission/$missionId") },
                        onOpenNote = { noteId -> navController.navigate("note/$noteId") },
                    )
                }
            }
            composable("settings") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    SettingsRoute()
                }
            }
            composable("routines") {
                TopLevelScreenContainer(
                    currentRoute = currentRoute,
                    destinations = destinations,
                    onNavigateTo = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                ) {
                    RoutinePlannerRoute(onOpenPlan = { planId -> navController.navigate("routine/$planId") })
                }
            }
            composable("routine/{planId}") {
                ScreenSurfaceFrame {
                    RoutinePlanDetailRoute(onOpenMission = { missionId -> navController.navigate("mission/$missionId") })
                }
            }
        }
    }
}
}

@Composable
private fun TopLevelScreenContainer(
    currentRoute: String?,
    destinations: List<TopLevelDestination>,
    onNavigateTo: (String) -> Unit,
    wrapInSurface: Boolean = true,
    content: @Composable () -> Unit,
) {
    val currentIndex = destinations.indexOfFirst { it.route == currentRoute }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(currentRoute) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (currentIndex >= 0 && kotlin.math.abs(totalDrag) > 120f) {
                            val targetIndex = when {
                                totalDrag < 0 && currentIndex < destinations.lastIndex -> currentIndex + 1
                                totalDrag > 0 && currentIndex > 0 -> currentIndex - 1
                                else -> -1
                            }
                            if (targetIndex >= 0) {
                                onNavigateTo(destinations[targetIndex].route)
                            }
                        }
                        totalDrag = 0f
                    },
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                }
            }
    ) {
        if (wrapInSurface) {
            ScreenSurfaceFrame {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
private fun ScreenSurfaceFrame(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary),
        ) {
            content()
        }
    }
}

@Composable
private fun SidequestTopBar(
    title: String,
    level: Int,
    subtitle: String,
    avatarImagePath: String?,
    showBack: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        color = BgElevated,
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
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = SidequestIcons.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(onClick = onSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    TopBarAvatarBadge(
                        avatarImagePath = avatarImagePath,
                        level = level,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarAvatarBadge(
    avatarImagePath: String?,
    level: Int,
) {
    val bitmap = remember(avatarImagePath) {
        avatarImagePath?.let(BitmapFactory::decodeFile)
    }
    Box(
        modifier = Modifier
            .size(54.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(AccentPrimary.copy(alpha = 0.18f)),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Open settings",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccentPrimary.copy(alpha = 0.24f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SidequestIcons.Profile,
                        contentDescription = "Open settings",
                        tint = AccentSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 2.dp)
                .zIndex(1f),
            color = BgPrimary,
            shape = CircleShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Text(
                text = "L$level",
                style = MaterialTheme.typography.labelSmall,
                color = AccentSecondary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
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
    intent.getStringExtra(RoutinePlanIdExtra)?.let { return DeepLinkTarget.Routine(it) }
    return null
}
