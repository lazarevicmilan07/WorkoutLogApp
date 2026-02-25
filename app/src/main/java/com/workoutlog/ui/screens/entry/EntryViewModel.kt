package com.workoutlog.ui.screens.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEntity
import com.workoutlog.domain.model.WorkoutEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class EntryUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val entryId: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val selectedTypeId: Long? = null,
    val note: String = "",
    val durationMinutes: String = "",
    val caloriesBurned: String = "",
    val workoutTypes: List<WorkoutType> = emptyList(),
    val isSaving: Boolean = false
)

sealed class EntryEvent {
    data object Saved : EntryEvent()
    data object Deleted : EntryEvent()
    data class Error(val message: String) : EntryEvent()
}

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EntryEvent>()
    val events = _events.asSharedFlow()

    fun setup(entryId: Long, dateArg: String) {
        _uiState.value = EntryUiState(isLoading = true)
        viewModelScope.launch {
            val types = typeRepository.getAll().map { it.toDomain() }

            if (entryId > 0) {
                val entry = entryRepository.getById(entryId)
                if (entry != null) {
                    _uiState.value = EntryUiState(
                        isLoading = false,
                        isEditing = true,
                        entryId = entry.id,
                        date = entry.toDomain().date,
                        selectedTypeId = entry.workoutTypeId,
                        note = entry.note ?: "",
                        durationMinutes = entry.durationMinutes?.toString() ?: "",
                        caloriesBurned = entry.caloriesBurned?.toString() ?: "",
                        workoutTypes = types
                    )
                    return@launch
                }
            }

            val initialDate = if (dateArg.isNotEmpty()) {
                try { LocalDate.parse(dateArg) } catch (_: Exception) { LocalDate.now() }
            } else LocalDate.now()

            _uiState.value = EntryUiState(
                isLoading = false,
                date = initialDate,
                workoutTypes = types
            )
        }
    }

    fun onDateChanged(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onTypeSelected(typeId: Long) {
        _uiState.value = _uiState.value.copy(selectedTypeId = typeId)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onDurationChanged(duration: String) {
        if (duration.isEmpty() || duration.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(durationMinutes = duration)
        }
    }

    fun onCaloriesChanged(calories: String) {
        if (calories.isEmpty() || calories.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(caloriesBurned = calories)
        }
    }

    fun delete() {
        val state = _uiState.value
        if (!state.isEditing) return
        viewModelScope.launch {
            entryRepository.deleteById(state.entryId)
            _events.emit(EntryEvent.Deleted)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.selectedTypeId == null) {
            viewModelScope.launch { _events.emit(EntryEvent.Error("Please select a workout type")) }
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val entry = WorkoutEntry(
                id = if (state.isEditing) state.entryId else 0,
                date = state.date,
                workoutTypeId = state.selectedTypeId,
                note = state.note.takeIf { it.isNotBlank() },
                durationMinutes = state.durationMinutes.toIntOrNull(),
                caloriesBurned = state.caloriesBurned.toIntOrNull()
            )

            if (state.isEditing) {
                entryRepository.update(entry.toEntity())
            } else {
                entryRepository.insert(entry.toEntity())
            }

            _events.emit(EntryEvent.Saved)
        }
    }
}
