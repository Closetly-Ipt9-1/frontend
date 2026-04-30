package com.closetly.myapp.premium.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.closetly.myapp.premium.data.PremiumRepository
import com.closetly.myapp.premium.model.PremiumOffer
import com.closetly.myapp.premium.model.PremiumUiState
import com.closetly.myapp.premium.model.SubscriptionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PremiumViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState

    private val repository = PremiumRepository(
        context = application,
        onPurchasesChanged = { refreshPurchases() },
        onBillingMessage = { message ->
            _uiState.value = _uiState.value.copy(message = message)
        }
    )

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, message = null)

        repository.loadSubscriptionStatus(
            onSuccess = { status ->
                startBilling(status)
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = message
                )
            }
        )
    }

    fun buy(activity: Activity, offer: PremiumOffer) {
        repository.launchPurchase(
            activity = activity,
            offer = offer,
            onError = { message ->
                _uiState.value = _uiState.value.copy(message = message)
            }
        )
    }

    private fun startBilling(status: SubscriptionStatus) {
        repository.startBillingConnection(
            onReady = {
                _uiState.value = _uiState.value.copy(isBillingReady = true)
                repository.loadOffers(
                    onSuccess = { offers ->
                        _uiState.value = _uiState.value.copy(offers = offers)
                        syncPurchases(status)
                    },
                    onError = { message ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            status = status,
                            message = message
                        )
                    }
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    status = status,
                    isBillingReady = false,
                    message = message
                )
            }
        )
    }

    private fun refreshPurchases() {
        val currentStatus = _uiState.value.status
        syncPurchases(currentStatus)
    }

    private fun syncPurchases(status: SubscriptionStatus) {
        repository.syncActivePurchases(
            existingStatus = status,
            onSuccess = { syncedStatus ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    status = syncedStatus,
                    message = if (syncedStatus.isPremium) "Premium is active." else null
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    status = status,
                    message = message
                )
            }
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }
}
