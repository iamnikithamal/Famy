package com.famy.tree.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.famy.tree.ui.navigation.BottomNavItems
import com.famy.tree.ui.navigation.FamyNavGraph
import com.famy.tree.ui.navigation.NavRoutes
import com.famy.tree.ui.theme.FamyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val ComponentActivity.dataStore: DataStore<Preferences> by preferencesDataStore(name = "famy_prefs")

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        val onboardingCompleted = runBlocking {
            dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }.first()
        }

        enableEdgeToEdge()

        setContent {
            var darkMode by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                dataStore.data.map { it[KEY_DARK_MODE] }.collect { darkMode = it }
            }

            LaunchedEffect(Unit) {
                keepSplashScreen = false
            }

            FamyTheme(
                darkTheme = darkMode ?: androidx.compose.foundation.isSystemInDarkTheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FamyApp(
                        startDestination = if (onboardingCompleted) {
                            NavRoutes.Home.route
                        } else {
                            NavRoutes.Onboarding.route
                        },
                        onOnboardingComplete = {
                            lifecycleScope.launch {
                                dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = true }
                            }
                        },
                        onDarkModeChange = { enabled ->
                            lifecycleScope.launch {
                                dataStore.edit { it[KEY_DARK_MODE] = enabled }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FamyApp(
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var selectedTreeId by rememberSaveable { mutableLongStateOf(0L) }

    val bottomNavRoutes = remember {
        setOf(
            NavRoutes.Home.route,
            "tree/",
            "members/",
            NavRoutes.Search.route
        )
    }

    val showBottomNav = remember(currentDestination?.route) {
        currentDestination?.route?.let { route ->
            when {
                route == NavRoutes.Home.route -> true
                route.startsWith("tree/") -> true
                route.startsWith("members/") -> true
                route == NavRoutes.Search.route -> true
                else -> false
            }
        } ?: false
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav && selectedTreeId > 0L,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FamyBottomNavBar(
                    currentRoute = currentDestination?.route,
                    treeId = selectedTreeId,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FamyNavGraph(
                navController = navController,
                startDestination = startDestination,
                onTreeSelected = { treeId ->
                    treeId?.let { selectedTreeId = it }
                }
            )
        }
    }
}

@Composable
private fun FamyBottomNavBar(
    currentRoute: String?,
    treeId: Long,
    onNavigate: (String) -> Unit
) {
    val items = remember(treeId) { BottomNavItems.getItems(treeId) }

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute?.let { route ->
                when (item.label) {
                    "Home" -> route == NavRoutes.Home.route
                    "Tree" -> route.startsWith("tree/")
                    "Members" -> route.startsWith("members/")
                    "Search" -> route == NavRoutes.Search.route
                    else -> false
                }
            } ?: false

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
