package com.workoutlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workoutlog.data.local.dao.WorkoutEntryDao
import com.workoutlog.data.local.dao.WorkoutTypeDao
import com.workoutlog.data.local.entity.WorkoutEntryEntity
import com.workoutlog.data.local.entity.WorkoutTypeEntity

@Database(
    entities = [
        WorkoutTypeEntity::class,
        WorkoutEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutTypeDao(): WorkoutTypeDao
    abstract fun workoutEntryDao(): WorkoutEntryDao

    companion object {
        const val DATABASE_NAME = "workout_log.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_types ADD COLUMN isRestDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE workout_types SET isRestDay = 1 WHERE LOWER(name) = 'rest day'")
            }
        }
    }
}
