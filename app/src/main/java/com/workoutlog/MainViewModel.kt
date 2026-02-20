package com.workoutlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.datastore.SettingsDataStore
import com.workoutlog.data.datastore.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsDataStore: SettingsDataStore
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val onboardingCompleted: StateFlow<Boolean?> = settingsDataStore.onboardingCompleted
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
