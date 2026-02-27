package com.workoutlog.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.WorkoutEntry
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEpochMilli
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val workoutTypes: List<WorkoutType> = emptyList(),
    val entries: List<WorkoutEntry> = emptyList(),
    val entriesByDate: Map<LocalDate, List<WorkoutEntry>> = emptyMap(),
    val workoutCount: Int = 0,
    val totalEntries: Int = 0,
    val daysElapsed: Int = 0,
    val workoutPercentage: Int = 0,
    val selectedFilter: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedFilter = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _currentMonth,
        _selectedFilter,
        typeRepository.getAllFlow(),
        _currentMonth.flatMapLatest { month ->
            val startDate = month.atDay(1).toEpochMilli()
            val endDate = month.atEndOfMonth().toEpochMilli()
            entryRepository.getEntriesBetweenDatesFlow(startDate, endDate)
        }
    ) { month, filter, typesEntities, entryEntities ->
        val typeMap = typesEntities.associate { it.id to it.toDomain() }
        val types = typesEntities.map { it.toDomain() }
        val allEntries = entryEntities.map { it.toDomain(typeMap[it.workoutTypeId]) }

        val filteredEntries = filter?.let { filterId ->
            allEntries.filter { it.workoutTypeId == filterId }
        } ?: allEntries

        val entriesByDate = filteredEntries.groupBy { it.date }

        val workoutCount = allEntries.count { it.workoutType?.isRestDay != true }
        val totalEntries = allEntries.size
        val today = LocalDate.now()
        val currentYearMonth = YearMonth.now()
        val daysElapsed = when {
            month > currentYearMonth -> 0
            month == currentYearMonth -> today.dayOfMonth
            else -> month.lengthOfMonth()
        }
        val workoutPercentage = if (daysElapsed > 0) workoutCount * 100 / daysElapsed else 0

        HomeUiState(
            isLoading = false,
            currentMonth = month,
            workoutTypes = types,
            entries = allEntries,
            entriesByDate = entriesByDate,
            workoutCount = workoutCount,
            totalEntries = totalEntries,
            daysElapsed = daysElapsed,
            workoutPercentage = workoutPercentage,
            selectedFilter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    fun setFilter(typeId: Long?) {
        _selectedFilter.value = typeId
    }

    fun deleteEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            entryRepository.deleteById(entry.id)
        }
    }

}
