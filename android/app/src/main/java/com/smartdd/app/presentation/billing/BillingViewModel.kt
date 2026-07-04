package com.smartdd.app.presentation.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.UpgradeRequest
import com.smartdd.app.data.repository.BillingRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingUiState(
    val products: List<ProductDetails> = emptyList(),
    val isLoading: Boolean = false,
    val purchaseSuccess: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val api: SmartDDApi
) : ViewModel() {
    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            billingRepository.purchaseResult.collect { result ->
                when (result) {
                    is Result.Success -> {
                        val upgrade = api.upgradePlan(UpgradeRequest(result.data, "smartdd_monthly"))
                        if (upgrade.isSuccessful) {
                            _state.value = _state.value.copy(purchaseSuccess = true, isLoading = false)
                        } else {
                            _state.value = _state.value.copy(error = "Error al verificar compra", isLoading = false)
                        }
                    }
                    is Result.Error -> {
                        _state.value = _state.value.copy(error = result.message, isLoading = false)
                    }
                }
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = billingRepository.queryProducts()) {
                is Result.Success -> _state.value = _state.value.copy(products = result.data, isLoading = false)
                is Result.Error -> _state.value = _state.value.copy(error = result.message, isLoading = false)
            }
        }
    }

    fun purchase(activity: Activity, product: ProductDetails) {
        _state.value = _state.value.copy(isLoading = true)
        billingRepository.launchPurchase(activity, product)
    }
}
