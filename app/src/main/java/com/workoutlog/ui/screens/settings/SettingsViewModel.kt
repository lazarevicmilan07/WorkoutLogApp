package com.workoutlog.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.datastore.SettingsDataStore
import com.workoutlog.data.datastore.ThemeMode
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.util.BackupUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showCalories: Boolean = true,
    val showDuration: Boolean = true,
    val totalWorkouts: Int = 0,
    val totalTypes: Int = 0,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false
)

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val typeRepository: WorkoutTypeRepository,
    private val entryRepository: WorkoutEntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val theme = settingsDataStore.themeMode.first()
            val showCal = settingsDataStore.showCalories.first()
            val showDur = settingsDataStore.showDuration.first()
            val totalW = entryRepository.getCount()
            val totalT = typeRepository.getCount()

            _uiState.value = SettingsUiState(
                themeMode = theme,
                showCalories = showCal,
                showDuration = showDur,
                totalWorkouts = totalW,
                totalTypes = totalT
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    fun setShowCalories(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowCalories(show)
            _uiState.value = _uiState.value.copy(showCalories = show)
        }
    }

    fun setShowDuration(show: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setShowDuration(show)
            _uiState.value = _uiState.value.copy(showDuration = show)
        }
    }

    fun backup() {
        _uiState.value = _uiState.value.copy(isBackingUp = true)
        viewModelScope.launch {
            try {
                val types = typeRepository.getAll()
                val entries = entryRepository.getAll()
                val uri = BackupUtil.createBackup(context, types, entries)
                _events.emit(SettingsEvent.Message("Backup saved successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message("Backup failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isBackingUp = false)
            }
        }
    }

    fun restore(uri: Uri) {
        _uiState.value = _uiState.value.copy(isRestoring = true)
        viewModelScope.launch {
            try {
                val backupData = BackupUtil.readBackup(context, uri)
                if (backupData != null) {
                    BackupUtil.restoreBackup(
                        backupData,
                        typeRepository,
                        entryRepository
                    )
                    loadSettings()
                    _events.emit(SettingsEvent.Message("Data restored successfully"))
                } else {
                    _events.emit(SettingsEvent.Message("Invalid backup file"))
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message("Restore failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isRestoring = false)
            }
        }
    }
}
