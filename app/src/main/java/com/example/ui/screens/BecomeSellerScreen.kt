package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SellerStatus
import com.example.ui.components.ZmFooter
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.model.ZmSeller
import com.example.ui.model.ZmUser
import com.example.ui.theme.*

@Composable
fun BecomeSellerScreen(
    currentUser: ZmUser?,
    currentSeller: ZmSeller?,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onSubmitApplication: (
        shopName: String,
        ownerName: String,
        phone: String,
        cnic: String,
        paymentMethod: String,
        paymentNumber: String,
        shopAddress: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onLoginRequired: () -> Unit,
    onOpenSellerDashboard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("become_seller_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (currentUser == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = ZmGreen, modifier = Modifier.size(56.dp))
                        Text("Become a Seller on ZM Bazaar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Login or Sign Up first to submit your shop application.", style = MaterialTheme.typography.bodyMedium, color = ZmMutedText)
                        Button(onClick = onLoginRequired, colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)) {
                            Text("Login / Sign Up", color = ZmWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (currentSeller?.status == SellerStatus.APPROVED) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = ZmGreen, modifier = Modifier.size(56.dp))
                        Text("${currentSeller.shopName} is Approved!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("You can now add products, manage orders, and track earnings in your Seller Dashboard.", style = MaterialTheme.typography.bodyMedium, color = ZmMutedText)
                        Button(
                            onClick = onOpenSellerDashboard,
                            colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                            modifier = Modifier.testTag("open_seller_dashboard_btn")
                        ) {
                            Text("Open Seller Dashboard", color = ZmWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                var shopName by remember(currentSeller) { mutableStateOf(currentSeller?.shopName ?: "") }
                var ownerName by remember(currentUser, currentSeller) { mutableStateOf(currentSeller?.ownerName ?: currentUser.fullName) }
                var phone by remember(currentUser, currentSeller) { mutableStateOf(currentSeller?.phone ?: currentUser.phone) }
                var cnic by remember(currentSeller) { mutableStateOf(currentSeller?.cnic ?: "") }
                var paymentMethod by remember(currentSeller) { mutableStateOf(currentSeller?.paymentMethod ?: "Easypaisa") }
                var paymentNumber by remember(currentSeller) { mutableStateOf(currentSeller?.paymentAccountNumber ?: "") }
                var shopAddress by remember(currentSeller) { mutableStateOf(currentSeller?.shopAddress ?: "") }
                var errorMsg by remember { mutableStateOf<String?>(null) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Become a Seller", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Only 10% commission on delivered orders. 90% paid directly to your Easypaisa / JazzCash / Bank.", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)

                        if (currentSeller?.status == SellerStatus.PENDING) {
                            Surface(color = ZmGreenLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Application Status: PENDING ADMIN APPROVAL (${currentSeller.shopName})",
                                    color = ZmGreenDark,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else if (currentSeller?.status == SellerStatus.REJECTED) {
                            Surface(color = ZmRedLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Previous application was rejected. You may update details and re-submit.",
                                    color = ZmRedDark,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("Shop Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("seller_shop_name")
                        )
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("seller_owner_name")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("seller_phone")
                        )
                        OutlinedTextField(
                            value = cnic,
                            onValueChange = { cnic = it },
                            label = { Text("CNIC (e.g., 81101-1234567-1) *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("seller_cnic")
                        )

                        Text("Payout Method (Visible only to Admin):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Easypaisa", "JazzCash", "Meezan Bank").forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ZmGreen,
                                        selectedLabelColor = ZmWhite
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = paymentNumber,
                            onValueChange = { paymentNumber = it },
                            label = { Text("Easypaisa / JazzCash / Bank Number *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("seller_payment_number")
                        )
                        OutlinedTextField(
                            value = shopAddress,
                            onValueChange = { shopAddress = it },
                            label = { Text("Shop Address *") },
                            modifier = Modifier.fillMaxWidth().testTag("seller_address")
                        )

                        if (!errorMsg.isNullOrBlank()) {
                            Text(errorMsg!!, color = ZmRed, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = {
                                errorMsg = null
                                onSubmitApplication(
                                    shopName, ownerName, phone, cnic, paymentMethod, paymentNumber, shopAddress,
                                    {},
                                    { err -> errorMsg = err }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("seller_submit_button")
                        ) {
                            Text("Submit Seller Application", color = ZmWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { ZmFooter(adminPaymentAccounts = adminPaymentAccounts) }
    }
}
