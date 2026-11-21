package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ZmFooter
import com.example.ui.components.ZmImage
import com.example.ui.model.ZmCartItem
import com.example.ui.model.ZmOrder
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.model.ZmUser
import com.example.ui.theme.*
import com.example.util.ImageStorageHelper
import com.example.util.IntentHelper

@Composable
fun CheckoutScreen(
    currentUser: ZmUser?,
    cartItems: List<ZmCartItem>,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onPlaceOrder: (
        fullName: String,
        phone: String,
        address: String,
        city: String,
        note: String,
        paymentMethod: String,
        paymentAccountInfo: String,
        paymentScreenshotUri: String,
        onSuccess: (ZmOrder) -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onOrderPlaced: (ZmOrder) -> Unit,
    onRequireLogin: () -> Unit
) {
    val context = LocalContext.current

    if (currentUser == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = ZmRed, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Login Required to Checkout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Please sign in to place your ZM Bazaar order.", style = MaterialTheme.typography.bodyMedium, color = ZmMutedText)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRequireLogin,
                colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
            ) {
                Text("Go to Login", color = ZmWhite, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    var fullName by remember(currentUser.id) { mutableStateOf(currentUser.fullName) }
    var phone by remember(currentUser.id) { mutableStateOf(currentUser.phone) }
    var address by remember { mutableStateOf("") }
    var city by remember(currentUser.id) { mutableStateOf(currentUser.city) }
    var note by remember { mutableStateOf("") }

    val activeAccounts = remember(adminPaymentAccounts) {
        adminPaymentAccounts.filter { it.isActive }
    }
    var selectedMethod by remember { mutableStateOf("Cash on Delivery (COD)") }
    var screenshotUri by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            screenshotUri = ImageStorageHelper.persistPickedImage(context, uri, "payment_screenshots") ?: uri.toString()
        }
    }

    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val delivery = if (subtotal >= 3000.0 || cartItems.isEmpty()) 0.0 else 150.0
    val grandTotal = subtotal + delivery

    val selectedAccount = activeAccounts.firstOrNull {
        "${it.providerName}: ${it.accountNumber} - ${it.accountTitle}" == selectedMethod ||
            it.providerName.equals(selectedMethod, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("checkout_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }

        // Delivery Details
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Delivery Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_name")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_phone")
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Complete Address *") },
                        leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("checkout_address")
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City *") },
                        leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_city")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Order Note (Optional)") },
                        leadingIcon = { Icon(Icons.Filled.Note, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("checkout_note")
                    )
                }
            }
        }

        // Payment Methods
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val options = buildList {
                        add("Cash on Delivery (COD)" to "Pay cash at your doorstep")
                        activeAccounts.forEach { acc ->
                            add("${acc.providerName}: ${acc.accountNumber} - ${acc.accountTitle}" to "Send ${IntentHelper.formatRs(grandTotal)} & upload screenshot")
                        }
                    }

                    options.forEach { (title, subtitle) ->
                        val isSelected = selectedMethod == title
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) ZmGreen else MaterialTheme.colorScheme.outline
                            ),
                            color = if (isSelected) ZmGreenLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedMethod = title }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedMethod = title },
                                    colors = RadioButtonDefaults.colors(selectedColor = ZmGreen)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                                }
                            }
                        }
                    }

                    // Payment Info Box & Screenshot Upload when non-COD
                    if (selectedMethod != "Cash on Delivery (COD)") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ZmBlack,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Payment Info Box",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ZmGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedMethod,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ZmWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Payable Amount: ${IntentHelper.formatRs(grandTotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ZmRed
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (selectedAccount != null) {
                                    OutlinedButton(
                                        onClick = {
                                            IntentHelper.copyToClipboard(
                                                context,
                                                selectedAccount.providerName,
                                                selectedAccount.accountNumber
                                            )
                                        },
                                        border = BorderStroke(1.dp, ZmGreen),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZmWhite)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Account Number")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    screenshotPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ZmBlack, contentColor = ZmWhite),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("upload_payment_screenshot_button")
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (screenshotUri.isBlank()) "Upload Payment Screenshot" else "Screenshot Attached ✓")
                            }
                            if (screenshotUri.isNotBlank()) {
                                ZmImage(
                                    imageSpec = screenshotUri,
                                    contentDescription = "Payment screenshot",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Order Totals + Place Order
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Items (${cartItems.sumOf { it.quantity }})")
                        Text(IntentHelper.formatRs(subtotal))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Charges")
                        Text(if (delivery == 0.0) "FREE" else IntentHelper.formatRs(delivery))
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            IntentHelper.formatRs(grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            color = ZmRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage!!, color = ZmRed, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            errorMessage = null
                            if (selectedMethod != "Cash on Delivery (COD)" && screenshotUri.isBlank()) {
                                errorMessage = "Please upload your payment screenshot for $selectedMethod."
                                return@Button
                            }
                            val accountInfo = selectedAccount?.let {
                                "${it.providerName}: ${it.accountNumber} (${it.accountTitle})"
                            } ?: "Cash on Delivery"
                            onPlaceOrder(
                                fullName,
                                phone,
                                address,
                                city,
                                note,
                                selectedMethod,
                                accountInfo,
                                screenshotUri,
                                { order -> onOrderPlaced(order) },
                                { err -> errorMessage = err }
                            )
                        },
                        enabled = cartItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ZmGreen, contentColor = ZmWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .testTag("place_order_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Place Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            ZmFooter(adminPaymentAccounts = adminPaymentAccounts)
        }
    }
}
