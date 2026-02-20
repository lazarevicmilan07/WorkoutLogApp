package com.workoutlog.di

import android.content.Context
import androidx.room.Room
import com.workoutlog.data.local.WorkoutDatabase
import com.workoutlog.data.local.dao.WorkoutEntryDao
import com.workoutlog.data.local.dao.WorkoutTypeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase {
        return Room.databaseBuilder(
            context,
            WorkoutDatabase::class.java,
            WorkoutDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideWorkoutTypeDao(database: WorkoutDatabase): WorkoutTypeDao {
        return database.workoutTypeDao()
    }

    @Provides
    fun provideWorkoutEntryDao(database: WorkoutDatabase): WorkoutEntryDao {
        return database.workoutEntryDao()
    }
}
