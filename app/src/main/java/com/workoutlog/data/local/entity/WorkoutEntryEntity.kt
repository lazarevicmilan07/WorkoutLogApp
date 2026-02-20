package com.workoutlog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workoutTypeId"]),
        Index(value = ["date"]),
        Index(value = ["date", "workoutTypeId"])
    ]
)
data class WorkoutEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val workoutTypeId: Long,
    val note: String? = null,
    val durationMinutes: Int? = null,
    val caloriesBurned: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
