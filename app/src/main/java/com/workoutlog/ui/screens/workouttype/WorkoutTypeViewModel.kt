package com.workoutlog.ui.screens.workouttype

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutTypesUiState(
    val isLoading: Boolean = true,
    val types: List<WorkoutType> = emptyList()
)

@HiltViewModel
class WorkoutTypesViewModel @Inject constructor(
    private val repository: WorkoutTypeRepository
) : ViewModel() {

    val uiState: StateFlow<WorkoutTypesUiState> = repository.getAllFlow()
        .map { types ->
            WorkoutTypesUiState(
                isLoading = false,
                types = types.map { it.toDomain() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WorkoutTypesUiState())

    fun deleteType(type: WorkoutType) {
        viewModelScope.launch {
            repository.deleteById(type.id)
        }
    }
}

// Separate ViewModel for Add/Edit
data class AddEditTypeUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val typeId: Long = 0,
    val name: String = "",
    val selectedColor: Color = Color.Gray,
    val icon: String = "fitness_center",
    val isSaving: Boolean = false
)

sealed class AddEditTypeEvent {
    data object Saved : AddEditTypeEvent()
    data class Error(val message: String) : AddEditTypeEvent()
}

@HiltViewModel
class AddEditWorkoutTypeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WorkoutTypeRepository
) : ViewModel() {

    private val typeId: Long = savedStateHandle.get<Long>("typeId") ?: -1L

    private val _uiState = MutableStateFlow(AddEditTypeUiState())
    val uiState: StateFlow<AddEditTypeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditTypeEvent>()
    val events = _events.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            if (typeId > 0) {
                val entity = repository.getById(typeId)
                if (entity != null) {
                    val type = entity.toDomain()
                    _uiState.value = AddEditTypeUiState(
                        isLoading = false,
                        isEditing = true,
                        typeId = type.id,
                        name = type.name,
                        selectedColor = type.color,
                        icon = type.icon ?: "fitness_center"
                    )
                    return@launch
                }
            }
            _uiState.value = AddEditTypeUiState(isLoading = false)
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onColorSelected(color: Color) {
        _uiState.value = _uiState.value.copy(selectedColor = color)
    }

    fun onIconSelected(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch { _events.emit(AddEditTypeEvent.Error("Name cannot be empty")) }
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val type = WorkoutType(
                id = if (state.isEditing) state.typeId else 0,
                name = state.name.trim(),
                color = state.selectedColor,
                icon = state.icon
            )

            if (state.isEditing) {
                repository.update(type.toEntity())
            } else {
                repository.insert(type.toEntity())
            }

            _events.emit(AddEditTypeEvent.Saved)
        }
    }
}
