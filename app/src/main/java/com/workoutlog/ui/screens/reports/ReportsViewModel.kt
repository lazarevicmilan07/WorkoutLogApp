package com.workoutlog.ui.screens.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.DailyCountData
import com.workoutlog.domain.model.MonthlyCountData
import com.workoutlog.domain.model.MonthlyReport
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.WorkoutTypeCountData
import com.workoutlog.domain.model.YearlyReport
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEpochMilli
import com.workoutlog.util.ExportUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ReportsUiState(
    val isLoading: Boolean = true,
    val isMonthly: Boolean = true,
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val monthlyReport: MonthlyReport? = null,
    val yearlyReport: YearlyReport? = null,
    val isExporting: Boolean = false
)

sealed class ReportsEvent {
    data class ExportSuccess(val message: String) : ReportsEvent()
    data class ExportError(val message: String) : ReportsEvent()
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReportsEvent>()
    val events = _events.asSharedFlow()

    init {
        loadReport()
    }

    fun setMonthly(isMonthly: Boolean) {
        _uiState.value = _uiState.value.copy(isMonthly = isMonthly)
        loadReport()
    }

    fun setMonth(month: Int) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
        loadReport()
    }

    fun setYear(year: Int) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
        loadReport()
    }

    fun previousPeriod() {
        val state = _uiState.value
        if (state.isMonthly) {
            val ym = YearMonth.of(state.selectedYear, state.selectedMonth).minusMonths(1)
            _uiState.value = state.copy(selectedYear = ym.year, selectedMonth = ym.monthValue)
        } else {
            _uiState.value = state.copy(selectedYear = state.selectedYear - 1)
        }
        loadReport()
    }

    fun nextPeriod() {
        val state = _uiState.value
        if (state.isMonthly) {
            val ym = YearMonth.of(state.selectedYear, state.selectedMonth).plusMonths(1)
            _uiState.value = state.copy(selectedYear = ym.year, selectedMonth = ym.monthValue)
        } else {
            _uiState.value = state.copy(selectedYear = state.selectedYear + 1)
        }
        loadReport()
    }

    private fun loadReport() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val types = typeRepository.getAll().map { it.toDomain() }
            val typeMap = types.associateBy { it.id }
            val state = _uiState.value

            if (state.isMonthly) {
                loadMonthlyReport(state.selectedYear, state.selectedMonth, typeMap)
            } else {
                loadYearlyReport(state.selectedYear, typeMap)
            }
        }
    }

    private suspend fun loadMonthlyReport(year: Int, month: Int, typeMap: Map<Long, WorkoutType>) {
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1).toEpochMilli()
        val endDate = yearMonth.atEndOfMonth().toEpochMilli()

        val entries = entryRepository.getEntriesBetweenDates(startDate, endDate)
        val typeCounts = entryRepository.getWorkoutTypeCountsBetween(startDate, endDate)
        val dailyCounts = entryRepository.getDailyCountsBetween(startDate, endDate)

        val daysWithWorkouts = entries.map { it.date }.distinct().size
        val totalDuration = entries.sumOf { it.durationMinutes ?: 0 }
        val totalCalories = entries.sumOf { it.caloriesBurned ?: 0 }

        val report = MonthlyReport(
            year = year,
            month = month,
            totalWorkouts = entries.size,
            totalRestDays = yearMonth.lengthOfMonth() - daysWithWorkouts,
            totalDuration = totalDuration,
            totalCalories = totalCalories,
            workoutTypeCounts = typeCounts.mapNotNull { tc ->
                typeMap[tc.workoutTypeId]?.let { WorkoutTypeCountData(it, tc.count) }
            },
            dailyCounts = dailyCounts.map { dc ->
                val day = java.time.Instant.ofEpochMilli(dc.date)
                    .atZone(java.time.ZoneId.systemDefault()).dayOfMonth
                DailyCountData(day, dc.count)
            }
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            monthlyReport = report
        )
    }

    private suspend fun loadYearlyReport(year: Int, typeMap: Map<Long, WorkoutType>) {
        val startDate = LocalDate.of(year, 1, 1).toEpochMilli()
        val endDate = LocalDate.of(year, 12, 31).toEpochMilli()

        val entries = entryRepository.getEntriesBetweenDates(startDate, endDate)
        val typeCounts = entryRepository.getWorkoutTypeCountsBetween(startDate, endDate)

        val monthlyGroups = entries.groupBy {
            java.time.Instant.ofEpochMilli(it.date)
                .atZone(java.time.ZoneId.systemDefault()).monthValue
        }

        val monthlyCounts = (1..12).map { month ->
            MonthlyCountData(month, monthlyGroups[month]?.size ?: 0)
        }

        val daysWithWorkouts = entries.map { it.date }.distinct().size

        val report = YearlyReport(
            year = year,
            totalWorkouts = entries.size,
            totalRestDays = (if (java.time.Year.of(year).isLeap) 366 else 365) - daysWithWorkouts,
            monthlyCounts = monthlyCounts,
            workoutTypeCounts = typeCounts.mapNotNull { tc ->
                typeMap[tc.workoutTypeId]?.let { WorkoutTypeCountData(it, tc.count) }
            }
        )

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            yearlyReport = report
        )
    }

    fun exportToExcel() {
        _uiState.value = _uiState.value.copy(isExporting = true)
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val uri = if (state.isMonthly && state.monthlyReport != null) {
                    ExportUtil.exportMonthlyToExcel(context, state.monthlyReport)
                } else if (!state.isMonthly && state.yearlyReport != null) {
                    ExportUtil.exportYearlyToExcel(context, state.yearlyReport)
                } else null

                if (uri != null) {
                    _events.emit(ReportsEvent.ExportSuccess("Excel exported successfully"))
                } else {
                    _events.emit(ReportsEvent.ExportError("No data to export"))
                }
            } catch (e: Exception) {
                _events.emit(ReportsEvent.ExportError("Export failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }

    fun exportToPdf() {
        _uiState.value = _uiState.value.copy(isExporting = true)
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val uri = if (state.isMonthly && state.monthlyReport != null) {
                    ExportUtil.exportMonthlyToPdf(context, state.monthlyReport)
                } else if (!state.isMonthly && state.yearlyReport != null) {
                    ExportUtil.exportYearlyToPdf(context, state.yearlyReport)
                } else null

                if (uri != null) {
                    _events.emit(ReportsEvent.ExportSuccess("PDF exported successfully"))
                } else {
                    _events.emit(ReportsEvent.ExportError("No data to export"))
                }
            } catch (e: Exception) {
                _events.emit(ReportsEvent.ExportError("Export failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }
}
