package com.workoutlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.workoutlog.data.local.dao.WorkoutEntryDao
import com.workoutlog.data.local.dao.WorkoutTypeDao
import com.workoutlog.data.local.entity.WorkoutEntryEntity
import com.workoutlog.data.local.entity.WorkoutTypeEntity

@Database(
    entities = [
        WorkoutTypeEntity::class,
        WorkoutEntryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutTypeDao(): WorkoutTypeDao
    abstract fun workoutEntryDao(): WorkoutEntryDao

    companion object {
        const val DATABASE_NAME = "workout_log.db"
    }
}
