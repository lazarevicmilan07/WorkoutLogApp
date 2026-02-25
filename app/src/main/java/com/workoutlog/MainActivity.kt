package com.workoutlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.workoutlog.data.datastore.ThemeMode
import com.workoutlog.ui.navigation.NavGraph
import com.workoutlog.ui.navigation.Screen
import com.workoutlog.ui.navigation.bottomNavItems
import com.workoutlog.ui.theme.WorkoutLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val onboardingCompleted by mainViewModel.onboardingCompleted.collectAsStateWithLifecycle()

            WorkoutLogTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingCompleted != null) {
                        MainContent(
                            startDestination = if (onboardingCompleted == true)
                                Screen.Home.route else Screen.Onboarding.route
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom bar visible routes
    val bottomBarRoutes = bottomNavItems.map { it.screen.route }
    val showBottomBar = currentRoute in bottomBarRoutes

    // navBarVisible drives AnimatedVisibility:
    //  • Hide immediately (ExitTransition.None) so innerPadding.bottom snaps to 0
    //    before the new screen starts sliding in — prevents buttons jumping on open.
    //  • Show only after a 300 ms delay (matching the screen exit animation) so the
    //    nav bar doesn't reclaim layout space while AddEditEntry is still sliding out
    //    — prevents buttons jumping on close.
    var navBarVisible by remember { mutableStateOf(showBottomBar) }
    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            delay(300L)
            navBarVisible = true
        } else {
            navBarVisible = false
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = navBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = ExitTransition.None
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavGraph(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}
