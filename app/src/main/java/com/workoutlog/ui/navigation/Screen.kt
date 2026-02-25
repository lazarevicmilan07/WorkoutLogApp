package com.workoutlog.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Overview : Screen("overview")
    data object StatsMonthly : Screen("stats_monthly")
    data object StatsYearly : Screen("stats_yearly")
    data object WorkoutTypes : Screen("workout_types")
    data object Settings : Screen("settings")
    data object AddEditEntry : Screen("add_edit_entry?entryId={entryId}&date={date}") {
        fun createRoute(entryId: Long? = null, date: String? = null): String {
            return "add_edit_entry?entryId=${entryId ?: -1}&date=${date ?: ""}"
        }
    }
    data object AddEditWorkoutType : Screen("add_edit_workout_type?typeId={typeId}") {
        fun createRoute(typeId: Long? = null): String {
            return "add_edit_workout_type?typeId=${typeId ?: -1}"
        }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.StatsMonthly, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BottomNavItem(Screen.WorkoutTypes, "Types", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)
