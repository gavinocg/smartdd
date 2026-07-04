package com.smartdd.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartdd.app.data.repository.AuthRepository
import com.smartdd.app.presentation.billing.BillingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val profileState by profileViewModel.state.collectAsStateWithLifecycle()
    val billingState by billingViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showUpgradeDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Perfil") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Default.AccountCircle, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(profileState.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(profileState.email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                Column(Modifier.padding(16.dp)) {
                    InfoRow("Plan", profileState.plan)
                    InfoRow("Rol", profileState.role)
                }
            }

            Spacer(Modifier.weight(1f))

            if (profileState.plan == "FREE") {
                Button(
                    onClick = {
                        showUpgradeDialog = true
                        billingViewModel.loadProducts()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                ) {
                    Icon(Icons.Default.Star, null); Spacer(Modifier.width(8.dp))
                    Text("Actualizar a Pro")
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedButton(onClick = profileViewModel::logout, modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Actualizar plan") },
            text = {
                if (billingState.isLoading) CircularProgressIndicator()
                else if (billingState.purchaseSuccess) Text("¡Plan actualizado con éxito!")
                else if (billingState.error.isNotEmpty()) Text(billingState.error)
                else {
                    Column {
                        billingState.products.forEach { product ->
                            val offer = product.subscriptionOfferDetails?.firstOrNull()
                            val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: product.productId
                            Button(
                                onClick = {
                                    val activity = context as? android.app.Activity
                                    activity?.let { billingViewModel.purchase(it, product) }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) { Text("$price - ${product.name}") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUpgradeDialog = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

data class ProfileUiState(val name: String = "", val email: String = "", val plan: String = "FREE", val role: String = "user")

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        val info = authRepository.getCurrentUserInfo()
        _state.value = ProfileUiState(
            name = info["name"] as? String ?: "",
            email = "",
            plan = info["plan"] as? String ?: "FREE",
            role = info["role"] as? String ?: "user"
        )
    }

    fun logout() { authRepository.logout() }
}
