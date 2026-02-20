package com.workoutlog.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.workoutlog.ui.screens.entry.AddEditEntryScreen
import com.workoutlog.ui.screens.home.HomeScreen
import com.workoutlog.ui.screens.onboarding.OnboardingScreen
import com.workoutlog.ui.screens.overview.OverviewScreen
import com.workoutlog.ui.screens.reports.ReportsScreen
import com.workoutlog.ui.screens.settings.SettingsScreen
import com.workoutlog.ui.screens.workouttype.AddEditWorkoutTypeScreen
import com.workoutlog.ui.screens.workouttype.WorkoutTypesScreen

private const val ANIM_DURATION = 300

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(ANIM_DURATION)) },
        exitTransition = { fadeOut(tween(ANIM_DURATION)) }
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onAddEntry = { date ->
                    navController.navigate(Screen.AddEditEntry.createRoute(date = date))
                },
                onEditEntry = { entryId ->
                    navController.navigate(Screen.AddEditEntry.createRoute(entryId = entryId))
                },
                onNavigateToOverview = {
                    navController.navigate(Screen.Overview.route)
                }
            )
        }

        composable(Screen.Overview.route) {
            OverviewScreen(
                onAddEntry = { date ->
                    navController.navigate(Screen.AddEditEntry.createRoute(date = date))
                },
                onEditEntry = { entryId ->
                    navController.navigate(Screen.AddEditEntry.createRoute(entryId = entryId))
                }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen()
        }

        composable(Screen.WorkoutTypes.route) {
            WorkoutTypesScreen(
                onAddType = {
                    navController.navigate(Screen.AddEditWorkoutType.createRoute())
                },
                onEditType = { typeId ->
                    navController.navigate(Screen.AddEditWorkoutType.createRoute(typeId = typeId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(
            route = Screen.AddEditEntry.route,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("date") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(ANIM_DURATION))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(ANIM_DURATION))
            }
        ) {
            AddEditEntryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddEditWorkoutType.route,
            arguments = listOf(
                navArgument("typeId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(ANIM_DURATION))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(ANIM_DURATION))
            }
        ) {
            AddEditWorkoutTypeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
