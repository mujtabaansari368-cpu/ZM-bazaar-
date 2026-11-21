package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.OrderStatus
import com.example.ui.components.ZmFooter
import com.example.ui.components.ZmImage
import com.example.ui.model.*
import com.example.ui.theme.*
import com.example.util.IntentHelper

@Composable
fun OrderSuccessScreen(
    orderCode: String,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onTrackOrders: () -> Unit,
    onContinueShopping: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("order_success_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, ZmGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ZmGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Order placed",
                            tint = ZmGreen,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Order Placed Successfully!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(color = ZmBlack, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "Order ID: $orderCode",
                            color = ZmWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Thank you for shopping with ZM Bazaar — Pakistan Ka No.1 Online Bazaar. Track your order status live from Pending to Delivered.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ZmMutedText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onTrackOrders,
                        colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("success_track_orders_button")
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Track My Orders", color = ZmWhite, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            IntentHelper.openWhatsApp(
                                context,
                                "Assalam-o-Alaikum ZM Bazaar! I just placed Order $orderCode. Please confirm."
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZmWhatsAppGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("success_whatsapp_button")
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Order on WhatsApp", color = ZmWhite, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onContinueShopping,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("success_continue_shopping_button")
                    ) {
                        Text("Continue Shopping")
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
fun MyOrdersScreen(
    currentUser: ZmUser?,
    orders: List<ZmOrder>,
    allOrderItems: List<ZmOrderItem>,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onLoginClick: () -> Unit,
    onShopNowClick: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("my_orders_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "My Orders",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }

        if (currentUser == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = ZmGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Login to View Your Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
                        ) {
                            Text("Login / Sign Up", color = ZmWhite)
                        }
                    }
                }
            }
        } else if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = ZmMutedText, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No Orders Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Once you place an order, track its status right here.", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onShopNowClick, colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)) {
                            Text("Start Shopping", color = ZmWhite)
                        }
                    }
                }
            }
        } else {
            items(orders, key = { it.id }) { order ->
                val itemsForOrder = if (order.items.isNotEmpty()) order.items else allOrderItems.filter { it.orderId == order.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(order.orderCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(IntentHelper.formatDateTime(order.createdAt), style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            }
                            StatusPill(order.status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OrderTrackingStepper(currentStatus = order.status)
                        Spacer(modifier = Modifier.height(10.dp))

                        itemsForOrder.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ZmImage(
                                    imageSpec = item.imageUri,
                                    contentDescription = item.productName,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${item.shopName} • Qty: ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                                    if (item.customName.isNotBlank()) {
                                        Text("Custom Name: ${item.customName}", style = MaterialTheme.typography.labelSmall, color = ZmGreenDark)
                                    }
                                }
                                Text(IntentHelper.formatRs(item.lineTotal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Payment: ${order.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                                Text("Grand Total: ${IntentHelper.formatRs(order.grandTotal)}", style = MaterialTheme.typography.titleSmall, color = ZmRed, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    IntentHelper.openWhatsApp(
                                        context,
                                        "Assalam-o-Alaikum ZM Bazaar! Order ${order.orderCode} status is ${order.status}. Please assist."
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, tint = ZmWhatsAppGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp")
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
fun StatusPill(status: String) {
    val (bg, fg) = when (status) {
        OrderStatus.DELIVERED -> ZmGreenLight to ZmGreenDark
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.CONFIRMED -> ZmBlack to ZmWhite
        OrderStatus.CANCELLED -> ZmRedLight to ZmRedDark
        else -> ZmRedLight to ZmRed
    }
    Surface(color = bg, shape = RoundedCornerShape(16.dp)) {
        Text(
            text = status,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderTrackingStepper(currentStatus: String) {
    if (currentStatus == OrderStatus.CANCELLED) {
        Surface(color = ZmRedLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Order Cancelled",
                color = ZmRedDark,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
        return
    }
    val steps = OrderStatus.trackingSteps
    val activeIdx = steps.indexOf(currentStatus).coerceAtLeast(0)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val reached = index <= activeIdx
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (reached) ZmGreen else ZmBorderLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (reached) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = ZmWhite, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) ZmGreenDark else ZmMutedText,
                    fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
