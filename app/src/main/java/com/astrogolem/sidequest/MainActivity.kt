package com.astrogolem.sidequest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.content.ContextCompat
import com.astrogolem.sidequest.core.data.repo.SecurityService
import com.astrogolem.sidequest.core.ui.theme.SidequestTheme
import com.astrogolem.sidequest.feature.capture.CaptureRoute
import com.astrogolem.sidequest.feature.inbox.InboxRoute
import com.astrogolem.sidequest.feature.lobby.LobbyRoute
import com.astrogolem.sidequest.feature.missions.MissionsRoute
import com.astrogolem.sidequest.feature.search.SearchRoute
import com.astrogolem.sidequest.feature.settings.SettingsRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var securityService: SecurityService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SidequestTheme {
                SidequestApp(activity = this, securityService = securityService)
            }
        }
    }
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
)

@Composable
private fun SidequestApp(
    activity: AppCompatActivity,
    securityService: SecurityService,
) {
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
        TopLevelDestination("missions", "Missions"),
        TopLevelDestination("capture", "Capture"),
        TopLevelDestination("inbox", "Inbox"),
        TopLevelDestination("lobby", "Lobby"),
        TopLevelDestination("search", "Search"),
        TopLevelDestination("settings", "Settings"),
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
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
                        icon = { Text(destination.label.take(1)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "missions",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("missions") { MissionsRoute() }
            composable("capture") { CaptureRoute() }
            composable("inbox") { InboxRoute() }
            composable("lobby") { LobbyRoute() }
            composable("search") { SearchRoute() }
            composable("settings") { SettingsRoute() }
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
