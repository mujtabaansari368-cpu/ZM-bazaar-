package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.ZmFooter
import com.example.ui.components.ZmImage
import com.example.ui.model.ZmCartItem
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.model.ZmUser
import com.example.ui.theme.*
import com.example.util.IntentHelper

@Composable
fun CartScreen(
    cartItems: List<ZmCartItem>,
    currentUser: ZmUser?,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onUpdateQuantity: (ZmCartItem, Int) -> Unit,
    onRemoveItem: (ZmCartItem) -> Unit,
    onContinueShopping: () -> Unit,
    onProceedToCheckout: () -> Unit,
    onRequireLogin: () -> Unit
) {
    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val deliveryCharges = if (cartItems.isEmpty() || subtotal >= 3000.0) 0.0 else 150.0
    val grandTotal = subtotal + deliveryCharges

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("cart_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Shopping Cart (${cartItems.sumOf { it.quantity }})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (subtotal > 0 && subtotal < 3000.0) {
                    Surface(color = ZmGreenLight, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "Free delivery at Rs. 3,000+",
                            style = MaterialTheme.typography.labelSmall,
                            color = ZmGreenDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (cartItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = null,
                            tint = ZmGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your ZM Bazaar Cart is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Explore Flash Sales, Electronics, Fashion & Custom Name Products!",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZmMutedText
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onContinueShopping,
                            colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                            modifier = Modifier.testTag("empty_cart_shop_button")
                        ) {
                            Text("Continue Shopping", color = ZmWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(cartItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZmImage(
                            imageSpec = item.imageUri,
                            contentDescription = item.productName,
                            modifier = Modifier
                                .size(82.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.shopName,
                                style = MaterialTheme.typography.bodySmall,
                                color = ZmMutedText
                            )
                            if (item.customName.isNotBlank()) {
                                Surface(
                                    color = ZmGreenLight,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Custom: ${item.customName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ZmGreenDark,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (item.customPhotoUri.isNotBlank()) {
                                Text(
                                    text = "+ Custom photo attached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZmRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = IntentHelper.formatRs(item.unitPrice * item.quantity),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ZmRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateQuantity(item, item.quantity - 1) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("cart_minus_${item.id}")
                                    ) {
                                        Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = item.quantity.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    IconButton(
                                        onClick = { onUpdateQuantity(item, item.quantity + 1) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("cart_plus_${item.id}")
                                    ) {
                                        Icon(Icons.Filled.AddCircleOutline, contentDescription = "Increase", tint = ZmGreen)
                                    }
                                    IconButton(
                                        onClick = { onRemoveItem(item) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("cart_remove_${item.id}")
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = ZmRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Order Price Breakdown & Checkout CTA
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Order Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SummaryRow("Total Price (Subtotal)", IntentHelper.formatRs(subtotal))
                        SummaryRow(
                            "Delivery Charges",
                            if (deliveryCharges == 0.0) "FREE" else IntentHelper.formatRs(deliveryCharges)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                IntentHelper.formatRs(grandTotal),
                                style = MaterialTheme.typography.titleLarge,
                                color = ZmRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        if (currentUser == null) {
                            Surface(
                                color = ZmRedLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Text(
                                    text = "Please login or sign up to complete checkout.",
                                    color = ZmRedDark,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onContinueShopping,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("continue_shopping_button")
                            ) {
                                Text("Continue Shopping")
                            }
                            Button(
                                onClick = {
                                    if (currentUser == null) onRequireLogin() else onProceedToCheckout()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ZmGreen, contentColor = ZmWhite),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("checkout_button")
                            ) {
                                Text(
                                    text = if (currentUser == null) "Login to Checkout" else "Checkout",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            ZmFooter(adminPaymentAccounts = adminPaymentAccounts)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ZmMutedText)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
