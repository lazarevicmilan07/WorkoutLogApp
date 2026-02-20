package com.workoutlog.domain.model

import androidx.compose.ui.graphics.Color
import com.workoutlog.data.local.entity.WorkoutTypeEntity

data class WorkoutType(
    val id: Long = 0,
    val name: String,
    val color: Color = Color.Gray,
    val icon: String? = null,
    val isDefault: Boolean = false
)

fun WorkoutTypeEntity.toDomain() = WorkoutType(
    id = id,
    name = name,
    color = if (color != null) Color(color) else Color.Gray,
    icon = icon,
    isDefault = isDefault
)

fun WorkoutType.toEntity() = WorkoutTypeEntity(
    id = id,
    name = name,
    color = color.value.toLong(),
    icon = icon,
    isDefault = isDefault
)
