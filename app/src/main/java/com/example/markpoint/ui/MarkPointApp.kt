package com.example.markpoint.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.markpoint.data.AppSettings
import com.example.markpoint.data.ThemeMode
import com.example.markpoint.ui.screens.AboutScreen
import com.example.markpoint.ui.screens.CalculatorScreen
import com.example.markpoint.ui.screens.HistoryScreen
import com.example.markpoint.ui.screens.SettingsScreen
import com.example.markpoint.ui.screens.SplashScreen
import com.example.markpoint.ui.theme.MarkPointTheme
import kotlinx.coroutines.launch

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    CALCULATOR("calculator", "Calculator", Icons.Outlined.Straighten),
    HISTORY("history", "History", Icons.Outlined.History),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings),
    ABOUT("about", "About", Icons.Outlined.Info)
}

@Composable
fun MarkPointApp(viewModel: MarkPointViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val loaded = settings
    if (loaded == null) {
        // Settings load in a few milliseconds; show a plain background so the theme never flashes.
        MarkPointTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
        return
    }

    val darkTheme = when (loaded.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SystemBarAppearance(darkTheme)

    MarkPointTheme(darkTheme = darkTheme, dynamicColor = loaded.dynamicColor) {
        var showSplash by rememberSaveable { mutableStateOf(true) }
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            MainScaffold(viewModel, loaded)
        }
    }
}

@Composable
private fun SystemBarAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

@Composable
private fun MainScaffold(viewModel: MarkPointViewModel, settings: AppSettings) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.CALCULATOR.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            composable(Destination.CALCULATOR.route) {
                CalculatorScreen(viewModel, state, settings, snackbarHostState)
            }
            composable(Destination.HISTORY.route) {
                HistoryScreen(
                    history = history,
                    decimals = settings.decimalPlaces,
                    onOpen = { record ->
                        viewModel.restore(record)
                        navController.navigateToTab(Destination.CALCULATOR.route)
                    },
                    onDelete = { record ->
                        viewModel.deleteHistory(record.id)
                        showMessage("Calculation deleted")
                    },
                    onClearAll = {
                        viewModel.clearHistory()
                        showMessage("History cleared")
                    }
                )
            }
            composable(Destination.SETTINGS.route) {
                SettingsScreen(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    onResetSettings = {
                        viewModel.resetSettings()
                        showMessage("Settings reset")
                    },
                    onClearHistory = {
                        viewModel.clearHistory()
                        showMessage("History cleared")
                    }
                )
            }
            composable(Destination.ABOUT.route) {
                AboutScreen()
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { navController.navigateToTab(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) }
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
