package com.workoutlog.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.datastore.SettingsDataStore
import com.workoutlog.data.repository.WorkoutTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val workoutTypeRepository: WorkoutTypeRepository
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            workoutTypeRepository.insertDefaults()
            settingsDataStore.setOnboardingCompleted(true)
        }
    }
}
