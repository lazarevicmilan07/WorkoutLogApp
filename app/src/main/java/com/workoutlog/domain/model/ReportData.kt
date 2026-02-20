package com.workoutlog.domain.model

data class MonthlyReport(
    val year: Int,
    val month: Int,
    val totalWorkouts: Int,
    val totalRestDays: Int,
    val totalDuration: Int,
    val totalCalories: Int,
    val workoutTypeCounts: List<WorkoutTypeCountData>,
    val dailyCounts: List<DailyCountData>
)

data class YearlyReport(
    val year: Int,
    val totalWorkouts: Int,
    val totalRestDays: Int,
    val monthlyCounts: List<MonthlyCountData>,
    val workoutTypeCounts: List<WorkoutTypeCountData>
)

data class WorkoutTypeCountData(
    val workoutType: WorkoutType,
    val count: Int
)

data class DailyCountData(
    val day: Int,
    val count: Int
)

data class MonthlyCountData(
    val month: Int,
    val count: Int
)
