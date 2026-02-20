package com.workoutlog.domain.model

import com.workoutlog.data.local.entity.WorkoutEntryEntity
import java.time.LocalDate
import java.time.ZoneId

data class WorkoutEntry(
    val id: Long = 0,
    val date: LocalDate,
    val workoutTypeId: Long,
    val workoutType: WorkoutType? = null,
    val note: String? = null,
    val durationMinutes: Int? = null,
    val caloriesBurned: Int? = null
)

fun WorkoutEntryEntity.toDomain(workoutType: WorkoutType? = null) = WorkoutEntry(
    id = id,
    date = java.time.Instant.ofEpochMilli(date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate(),
    workoutTypeId = workoutTypeId,
    workoutType = workoutType,
    note = note,
    durationMinutes = durationMinutes,
    caloriesBurned = caloriesBurned
)

fun WorkoutEntry.toEntity() = WorkoutEntryEntity(
    id = id,
    date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    workoutTypeId = workoutTypeId,
    note = note,
    durationMinutes = durationMinutes,
    caloriesBurned = caloriesBurned
)

fun LocalDate.toEpochMilli(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
