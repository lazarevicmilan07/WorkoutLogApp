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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val todayEntries: List<WorkoutEntry> = emptyList(),
    val recentEntries: List<WorkoutEntry> = emptyList(),
    val workoutTypes: List<WorkoutType> = emptyList(),
    val totalWorkoutsThisMonth: Int = 0,
    val currentStreak: Int = 0,
    val mostCommonType: WorkoutType? = null,
    val searchQuery: String = "",
    val searchResults: List<WorkoutEntry> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val monthStart = now.withDayOfMonth(1).toEpochMilli()
            val monthEnd = YearMonth.from(now).atEndOfMonth().toEpochMilli()
            val today = now.toEpochMilli()
            val todayEnd = now.plusDays(1).toEpochMilli() - 1

            combine(
                typeRepository.getAllFlow(),
                entryRepository.getEntriesBetweenDatesFlow(monthStart, monthEnd),
                entryRepository.getEntriesBetweenDatesFlow(today, todayEnd)
            ) { types, monthEntries, todayEntries ->
                val typeMap = types.associate { it.id to it.toDomain() }
                val domainTypes = types.map { it.toDomain() }
                val domainMonthEntries = monthEntries.map { it.toDomain(typeMap[it.workoutTypeId]) }
                val domainTodayEntries = todayEntries.map { it.toDomain(typeMap[it.workoutTypeId]) }

                val typeCounts = domainMonthEntries
                    .groupBy { it.workoutTypeId }
                    .mapValues { it.value.size }
                val mostCommonTypeId = typeCounts.maxByOrNull { it.value }?.key
                val mostCommonType = mostCommonTypeId?.let { typeMap[it] }

                HomeUiState(
                    isLoading = false,
                    todayEntries = domainTodayEntries,
                    recentEntries = domainMonthEntries.take(10),
                    workoutTypes = domainTypes,
                    totalWorkoutsThisMonth = domainMonthEntries.size,
                    currentStreak = calculateStreak(domainMonthEntries),
                    mostCommonType = mostCommonType
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
                .collect { _uiState.value = it }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearching = query.isNotEmpty())
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                val types = typeRepository.getAll()
                val typeMap = types.associate { it.id to it.toDomain() }
                entryRepository.searchEntries(query).collect { entries ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = entries.map { it.toDomain(typeMap[it.workoutTypeId]) }
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
        }
    }

    fun deleteEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            entryRepository.deleteById(entry.id)
        }
    }

    private fun calculateStreak(entries: List<WorkoutEntry>): Int {
        if (entries.isEmpty()) return 0
        val dates = entries.map { it.date }.distinct().sorted().reversed()
        var streak = 0
        var expectedDate = LocalDate.now()

        for (date in dates) {
            if (date == expectedDate || date == expectedDate.minusDays(1)) {
                streak++
                expectedDate = date.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }
}
