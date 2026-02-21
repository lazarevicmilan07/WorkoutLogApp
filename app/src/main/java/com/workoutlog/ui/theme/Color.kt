package com.workoutlog.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Primary palette
val Primary = Color(0xFF3B82F6)
val PrimaryDark = Color(0xFF60A5FA)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDBEAFE)
val PrimaryContainerDark = Color(0xFF1E3A5F)
val OnPrimaryContainer = Color(0xFF1E3A5F)
val OnPrimaryContainerDark = Color(0xFFDBEAFE)

// Secondary palette
val Secondary = Color(0xFF10B981)
val SecondaryDark = Color(0xFF34D399)
val SecondaryContainer = Color(0xFFD1FAE5)
val SecondaryContainerDark = Color(0xFF064E3B)

// Tertiary palette
val Tertiary = Color(0xFFF59E0B)
val TertiaryDark = Color(0xFFFBBF24)
val TertiaryContainer = Color(0xFFFEF3C7)
val TertiaryContainerDark = Color(0xFF78350F)

// Error palette
val Error = Color(0xFFEF4444)
val ErrorDark = Color(0xFFF87171)
val ErrorContainer = Color(0xFFFEE2E2)

// Surfaces - Light
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceVariantLight = Color(0xFFF3F4F6)
val OnSurfaceLight = Color(0xFF111827)
val OnSurfaceVariantLight = Color(0xFF6B7280)
val OutlineLight = Color(0xFFD1D5DB)
val BackgroundLight = Color(0xFFFFFFFF)

// Surfaces - Dark
val SurfaceDark = Color(0xFF111827)
val SurfaceVariantDark = Color(0xFF1F2937)
val OnSurfaceDark = Color(0xFFF9FAFB)
val OnSurfaceVariantDark = Color(0xFF9CA3AF)
val OutlineDark = Color(0xFF374151)
val BackgroundDark = Color(0xFF030712)

// Workout type colors
val WorkoutColors = listOf(
    Color(0xFFE53935), // Red
    Color(0xFF1E88E5), // Blue
    Color(0xFF43A047), // Green
    Color(0xFFFB8C00), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFFD81B60), // Pink
    Color(0xFF00ACC1), // Cyan
    Color(0xFF5E35B1), // Deep Purple
    Color(0xFF3949AB), // Indigo
    Color(0xFF00897B), // Teal
    Color(0xFF7CB342), // Light Green
    Color(0xFFFF7043), // Deep Orange
)

val WorkoutIcons: Map<String, ImageVector> = mapOf(
    "fitness_center" to Icons.Default.FitnessCenter,
    "directions_run" to Icons.AutoMirrored.Filled.DirectionsRun,
    "directions_bike" to Icons.AutoMirrored.Filled.DirectionsBike,
    "pool" to Icons.Default.Pool,
    "self_improvement" to Icons.Default.SelfImprovement,
    "sports_gymnastics" to Icons.Default.SportsGymnastics,
    "sports_martial_arts" to Icons.Default.SportsMartialArts,
    "sports_kabaddi" to Icons.Default.SportsKabaddi,
    "hiking" to Icons.Default.Hiking,
    "rowing" to Icons.Default.Rowing,
    "sports_tennis" to Icons.Default.SportsTennis,
    "hotel" to Icons.Default.Hotel,
)

fun getWorkoutIcon(iconName: String?): ImageVector {
    return iconName?.let { WorkoutIcons[it] } ?: Icons.Default.FitnessCenter
}
