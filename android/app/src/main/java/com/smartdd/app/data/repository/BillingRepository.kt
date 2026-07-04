package com.smartdd.app.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _purchaseResult = Channel<Result<String>>(Channel.BUFFERED)
    val purchaseResult: Flow<Result<String>> = _purchaseResult.receiveAsFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    _purchaseResult.trySend(Result.Success(purchase.purchaseToken))
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseResult.trySend(Result.Error("Compra cancelada"))
        } else {
            _purchaseResult.trySend(Result.Error("Error en compra: ${billingResult.debugMessage}"))
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    suspend fun queryProducts(): Result<List<ProductDetails>> = suspendCancellableCoroutine { continuation ->
        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        fetchProducts(continuation)
                    } else {
                        continuation.resume(Result.Error("Fallo conexión billing: ${result.debugMessage}"))
                    }
                }

                override fun onBillingServiceDisconnected() {
                    continuation.resume(Result.Error("Servicio de facturación desconectado"))
                }
            })
        } else {
            fetchProducts(continuation)
        }
    }

    private fun fetchProducts(continuation: kotlin.coroutines.Continuation<Result<List<ProductDetails>>>) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("smartdd_monthly")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("smartdd_yearly")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )).build()

        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(Result.Success(details))
            } else {
                continuation.resume(Result.Error("Error al obtener productos"))
            }
        }
    }

    fun launchPurchase(activity: Activity, product: ProductDetails) {
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            _purchaseResult.trySend(Result.Error("Sin oferta disponible"))
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product)
                    .setOfferToken(offerToken)
                    .build()
            )).build()
        billingClient.launchBillingFlow(activity, params)
    }
}
