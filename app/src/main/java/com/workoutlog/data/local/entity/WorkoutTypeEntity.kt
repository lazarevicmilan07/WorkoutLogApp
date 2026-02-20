package com.workoutlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_types")
data class WorkoutTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long? = null,
    val icon: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false
)
