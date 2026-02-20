package com.workoutlog.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    color = if (color != null) Color(color.toInt()) else Color.Gray,
    icon = icon,
    isDefault = isDefault
)

fun WorkoutType.toEntity() = WorkoutTypeEntity(
    id = id,
    name = name,
    color = color.toArgb().toLong(),
    icon = icon,
    isDefault = isDefault
)
