package com.workoutlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.workoutlog.data.local.entity.WorkoutEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutEntryDao {

    @Query("SELECT * FROM workout_entries ORDER BY date DESC")
    fun getAllFlow(): Flow<List<WorkoutEntryEntity>>

    @Query("SELECT * FROM workout_entries ORDER BY date DESC")
    suspend fun getAll(): List<WorkoutEntryEntity>

    @Query("SELECT * FROM workout_entries WHERE id = :id")
    suspend fun getById(id: Long): WorkoutEntryEntity?

    @Query("SELECT * FROM workout_entries WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: Long): WorkoutEntryEntity?

    @Query("SELECT * FROM workout_entries WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<WorkoutEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WorkoutEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<WorkoutEntryEntity>)

    @Update
    suspend fun update(entry: WorkoutEntryEntity)

    @Delete
    suspend fun delete(entry: WorkoutEntryEntity)

    @Query("DELETE FROM workout_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM workout_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getEntriesBetweenDatesFlow(startDate: Long, endDate: Long): Flow<List<WorkoutEntryEntity>>

    @Query("SELECT * FROM workout_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getEntriesBetweenDates(startDate: Long, endDate: Long): List<WorkoutEntryEntity>

    @Query("""
        SELECT workoutTypeId, COUNT(*) as count
        FROM workout_entries
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY workoutTypeId
        ORDER BY count DESC
    """)
    suspend fun getWorkoutTypeCountsBetween(startDate: Long, endDate: Long): List<WorkoutTypeCount>

    @Query("""
        SELECT date, COUNT(*) as count
        FROM workout_entries
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    suspend fun getDailyCountsBetween(startDate: Long, endDate: Long): List<DailyCount>

    @Query("""
        SELECT * FROM workout_entries
        WHERE note LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    fun searchEntries(query: String): Flow<List<WorkoutEntryEntity>>

    @Query("SELECT COUNT(*) FROM workout_entries")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM workout_entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getCountBetween(startDate: Long, endDate: Long): Int

    @Query("DELETE FROM workout_entries")
    suspend fun deleteAll()
}

data class WorkoutTypeCount(
    val workoutTypeId: Long,
    val count: Int
)

data class DailyCount(
    val date: Long,
    val count: Int
)
