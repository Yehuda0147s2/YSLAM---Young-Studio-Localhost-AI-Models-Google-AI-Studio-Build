package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.ui.components.AuthDialog
import com.example.ui.components.TopStatusHeader
import com.example.ui.screens.ApiKeysScreen
import com.example.ui.screens.ChatHistoryDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.PlaygroundScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.Inter
import com.example.ui.theme.YslamBackground
import com.example.ui.theme.YslamBorder
import com.example.ui.theme.YslamCyan
import com.example.ui.theme.YslamSurface
import com.example.ui.theme.YslamTextMuted
import com.example.ui.theme.YslamTextSecondary
import com.example.ui.theme.YslamTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.YslamViewModel

enum class NavTab(val title: String) {
    CHAT("Chat"),
    PLAYGROUND("Playground"),
    SETTINGS("Models & Config")
}

class MainActivity : ComponentActivity() {
    private val viewModel: YslamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // Initialize notification channel for background task completion alerts
        com.example.util.permission.NotificationHelper.ensureChannel(this)

        // Attempt silent auto-sign-in on app startup
        viewModel.authManager.attemptAutoSignIn(lifecycleScope)

        enableEdgeToEdge()

        setContent {
            YslamTheme {
                YslamApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun YslamApp(viewModel: YslamViewModel) {
    var currentTab by remember { mutableStateOf(NavTab.CHAT) }
    var showApiKeysScreen by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val activeBackend by viewModel.activeBackend.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val healthStatus by viewModel.healthStatus.collectAsState()
    val currentUser by viewModel.authManager.currentUser.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    // Handle system back gesture
    if (showApiKeysScreen) {
        BackHandler {
            showApiKeysScreen = false
        }
    } else if (currentTab != NavTab.CHAT) {
        BackHandler {
            currentTab = NavTab.CHAT
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopStatusHeader(
                activeBackend = activeBackend,
                activeModel = activeModel,
                healthStatus = healthStatus,
                userDisplayName = currentUser?.displayName ?: currentUser?.email,
                onSwitchBackend = { viewModel.setBackend(it) },
                onOpenAuth = { showAuthDialog = true },
                onOpenApiKeys = { showApiKeysScreen = true },
                onOpenHistory = { showHistoryDialog = true },
                onOpenSettings = {
                    showApiKeysScreen = false
                    currentTab = NavTab.SETTINGS
                }
            )
        },
        bottomBar = {
            if (!showApiKeysScreen) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(YslamSurface)
                        .border(1.dp, YslamBorder)
                        .testTag("bottom_nav_bar"),
                    containerColor = YslamSurface,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = currentTab == NavTab.CHAT,
                        onClick = { currentTab = NavTab.CHAT },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == NavTab.CHAT) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Chat"
                            )
                        },
                        label = {
                            Text(
                                text = "Chat",
                                fontFamily = Inter,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YslamBackground,
                            selectedTextColor = YslamCyan,
                            indicatorColor = YslamCyan,
                            unselectedIconColor = YslamTextMuted,
                            unselectedTextColor = YslamTextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_chat")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavTab.PLAYGROUND,
                        onClick = { currentTab = NavTab.PLAYGROUND },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == NavTab.PLAYGROUND) Icons.Filled.Code else Icons.Outlined.Code,
                                contentDescription = "Playground"
                            )
                        },
                        label = {
                            Text(
                                text = "Playground",
                                fontFamily = Inter,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YslamBackground,
                            selectedTextColor = YslamCyan,
                            indicatorColor = YslamCyan,
                            unselectedIconColor = YslamTextMuted,
                            unselectedTextColor = YslamTextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_playground")
                    )

                    NavigationBarItem(
                        selected = currentTab == NavTab.SETTINGS,
                        onClick = { currentTab = NavTab.SETTINGS },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == NavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Config"
                            )
                        },
                        label = {
                            Text(
                                text = "Config",
                                fontFamily = Inter,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = YslamBackground,
                            selectedTextColor = YslamCyan,
                            indicatorColor = YslamCyan,
                            unselectedIconColor = YslamTextMuted,
                            unselectedTextColor = YslamTextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showApiKeysScreen) {
                ApiKeysScreen(
                    keyStorage = viewModel.secureKeyStorage
                )
            } else {
                when (currentTab) {
                    NavTab.CHAT -> ChatScreen(
                        viewModel = viewModel,
                        onNavigateToPlaygroundWithCode = { code, lang ->
                            viewModel.loadPlaygroundTemplate("From Chat", code, lang)
                            currentTab = NavTab.PLAYGROUND
                        }
                    )
                    NavTab.PLAYGROUND -> PlaygroundScreen(
                        viewModel = viewModel
                    )
                    NavTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToApiKeys = { showApiKeysScreen = true },
                        onOpenAuth = { showAuthDialog = true }
                    )
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            authManager = viewModel.authManager,
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false }
        )
    }

    if (showHistoryDialog) {
        ChatHistoryDialog(
            sessions = sessions,
            currentSessionId = currentSessionId,
            isAuthenticated = currentUser != null,
            onSelectSession = { sessionId ->
                viewModel.selectSession(sessionId)
                currentTab = NavTab.CHAT
            },
            onNewSession = {
                viewModel.createNewSession("New Chat")
                currentTab = NavTab.CHAT
            },
            onDeleteSession = { sessionId ->
                viewModel.deleteSession(sessionId)
            },
            onRenameSession = { sessionId, newTitle ->
                viewModel.renameSession(sessionId, newTitle)
            },
            onSyncSessionToCloud = { session ->
                viewModel.syncSessionToCloud(session)
            },
            onDismiss = { showHistoryDialog = false }
        )
    }
}
