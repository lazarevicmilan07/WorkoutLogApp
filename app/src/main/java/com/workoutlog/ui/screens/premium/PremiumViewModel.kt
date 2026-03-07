package com.workoutlog.ui.screens.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.billing.BillingManager
import com.workoutlog.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PremiumEvent>()
    val events = _events.asSharedFlow()

    init {
        loadPremiumDetails()
        observeBillingEvents()
    }

    private fun loadPremiumDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val price = billingManager.getPremiumPrice()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                price = price ?: "$2.99"
            )
        }
    }

    private fun observeBillingEvents() {
        viewModelScope.launch {
            billingManager.purchaseState.collect { state ->
                when (state) {
                    is BillingManager.PurchaseState.Success -> {
                        settingsDataStore.setPremium(true)
                        _events.emit(PremiumEvent.PurchaseSuccess)
                    }
                    is BillingManager.PurchaseState.Error -> {
                        _events.emit(PremiumEvent.PurchaseError(state.message))
                    }
                    is BillingManager.PurchaseState.Canceled -> {
                        _events.emit(PremiumEvent.PurchaseCanceled)
                    }
                    else -> {}
                }
            }
        }
    }

    fun purchasePremium(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val launched = billingManager.launchBillingFlow(activity)
            if (!launched) {
                _events.emit(PremiumEvent.PurchaseError("Unable to connect to Google Play. Please try again."))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isPremium = billingManager.checkPurchases()
            if (isPremium) {
                settingsDataStore.setPremium(true)
                _events.emit(PremiumEvent.PurchaseSuccess)
            } else {
                _events.emit(PremiumEvent.PurchaseError("No previous purchase found"))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}

data class PremiumUiState(
    val isLoading: Boolean = false,
    val price: String? = null
)

sealed class PremiumEvent {
    data object PurchaseSuccess : PremiumEvent()
    data class PurchaseError(val message: String) : PremiumEvent()
    data object PurchaseCanceled : PremiumEvent()
}
