package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.OrderStatus
import com.example.data.local.ProductApproval
import com.example.data.local.SellerStatus
import com.example.ui.components.ZmFooter
import com.example.ui.components.ZmImage
import com.example.ui.model.*
import com.example.ui.theme.*
import com.example.util.ImageStorageHelper
import com.example.util.IntentHelper

@Composable
fun SellerDashboardScreen(
    seller: ZmSeller?,
    sellerProducts: List<ZmProduct>,
    sellerOrderItems: List<ZmOrderItem>,
    allOrders: List<ZmOrder>,
    categories: List<ZmCategory>,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onSaveProduct: (ZmProduct) -> Unit,
    onDeleteProduct: (ZmProduct) -> Unit,
    onUpdateOrderStatus: (String, String) -> Unit,
    onChangePaymentAccount: (sellerId: String, method: String, number: String, title: String) -> Unit,
    onNavigateToBecomeSeller: () -> Unit
) {
    if (seller == null || seller.status != SellerStatus.APPROVED) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Storefront, contentDescription = null, tint = ZmRed, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Seller Dashboard Locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Apply and get Admin approval to access your Seller Dashboard.", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onNavigateToBecomeSeller, colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)) {
                Text("Go to Become a Seller", color = ZmWhite)
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<ZmProduct?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showChangeAccountDialog by remember { mutableStateOf(false) }

    val totalSales = sellerOrderItems.sumOf { it.lineTotal }
    val totalEarnings = sellerOrderItems.sumOf { it.sellerEarningAmount }
    val distinctOrderIds = sellerOrderItems.map { it.orderId }.distinct()
    val sellerOrders = allOrders.filter { it.id in distinctOrderIds }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("seller_dashboard_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(seller.shopName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Seller Dashboard • 90% Seller Earning (10% ZM Bazaar Commission)", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMiniCard("Total Sales", IntentHelper.formatRs(totalSales), ZmBlack, Modifier.weight(1f))
                    KpiMiniCard("Total Earnings", IntentHelper.formatRs(totalEarnings), ZmGreen, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMiniCard("Total Orders", sellerOrders.size.toString(), ZmRed, Modifier.weight(1f))
                    KpiMiniCard("Total Products", sellerProducts.size.toString(), ZmGreenDark, Modifier.weight(1f))
                }
            }
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ZmGreen
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Products") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Orders (${sellerOrders.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Payment Account") })
            }
        }

        when (selectedTab) {
            0 -> {
                item {
                    Button(
                        onClick = {
                            editingProduct = null
                            showProductDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("seller_add_product_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Product", color = ZmWhite, fontWeight = FontWeight.Bold)
                    }
                }
                items(sellerProducts, key = { it.id }) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ZmImage(
                                imageSpec = prod.primaryImage,
                                contentDescription = prod.name,
                                modifier = Modifier.size(66.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${prod.category} • Stock: ${prod.stock}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                                Text(IntentHelper.formatRs(prod.price), style = MaterialTheme.typography.labelLarge, color = ZmRed)
                                Text("Status: ${prod.approvalStatus}", style = MaterialTheme.typography.labelSmall, color = ZmGreenDark)
                            }
                            IconButton(onClick = {
                                editingProduct = prod
                                showProductDialog = true
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit product", tint = ZmGreen)
                            }
                            IconButton(onClick = { onDeleteProduct(prod) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete product", tint = ZmRed)
                            }
                        }
                    }
                }
            }
            1 -> {
                if (sellerOrders.isEmpty()) {
                    item {
                        Text("No customer orders for your shop yet.", modifier = Modifier.padding(24.dp), color = ZmMutedText)
                    }
                } else {
                    items(sellerOrders, key = { it.id }) { order ->
                        val items = sellerOrderItems.filter { it.orderId == order.id }.ifEmpty {
                            order.items.filter { it.sellerId == seller.id }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(order.orderCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    StatusPill(order.status)
                                }
                                Text("Customer: ${order.customerName} (${order.city})", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                                items.forEach { itItem ->
                                    Text(
                                        "• ${itItem.productName} x${itItem.quantity} = ${IntentHelper.formatRs(itItem.lineTotal)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (itItem.customName.isNotBlank()) {
                                        Text("  Custom Name: ${itItem.customName}", style = MaterialTheme.typography.labelSmall, color = ZmGreenDark)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Change Order Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(OrderStatus.trackingSteps) { st ->
                                        FilterChip(
                                            selected = order.status == st,
                                            onClick = { onUpdateOrderStatus(order.id, st) },
                                            label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ZmGreen)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("My Payment Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Private: Only visible to you and ZM Bazaar Admin (hidden from customers).",
                                style = MaterialTheme.typography.bodySmall,
                                color = ZmMutedText
                            )
                            HorizontalDivider()
                            Text("Method: ${seller.paymentMethod}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Account Number: ${seller.paymentAccountNumber}", style = MaterialTheme.typography.titleMedium, color = ZmGreen, fontWeight = FontWeight.Bold)
                            Text("Account Title: ${seller.paymentAccountTitle}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { showChangeAccountDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ZmBlack),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("seller_change_account_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = ZmWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Account", color = ZmWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item { ZmFooter(adminPaymentAccounts = adminPaymentAccounts) }
    }

    if (showProductDialog) {
        SellerProductEditorDialog(
            seller = seller,
            existing = editingProduct,
            categories = categories,
            onDismiss = { showProductDialog = false },
            onSave = { product ->
                onSaveProduct(product)
                showProductDialog = false
            }
        )
    }

    if (showChangeAccountDialog) {
        var method by remember { mutableStateOf(seller.paymentMethod) }
        var number by remember { mutableStateOf(seller.paymentAccountNumber) }
        var title by remember { mutableStateOf(seller.paymentAccountTitle) }

        AlertDialog(
            onDismissRequest = { showChangeAccountDialog = false },
            title = { Text("Change Seller Payment Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Easypaisa", "JazzCash", "Bank").forEach { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                        }
                    }
                    OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Account / Wallet Number") }, singleLine = true)
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Account Title") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (number.isNotBlank()) {
                            onChangePaymentAccount(seller.id, method, number, title)
                            showChangeAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
                ) { Text("Save Account", color = ZmWhite) }
            },
            dismissButton = {
                TextButton(onClick = { showChangeAccountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun KpiMiniCard(title: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = ZmWhite.copy(alpha = 0.85f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = ZmWhite, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SellerProductEditorDialog(
    seller: ZmSeller,
    existing: ZmProduct?,
    categories: List<ZmCategory>,
    onDismiss: () -> Unit,
    onSave: (ZmProduct) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var price by remember { mutableStateOf(existing?.price?.toInt()?.toString() ?: "") }
    var oldPrice by remember { mutableStateOf(if ((existing?.oldPrice ?: 0.0) > 0) existing!!.oldPrice.toInt().toString() else "") }
    var stock by remember { mutableStateOf(existing?.stock?.toString() ?: "10") }
    var category by remember { mutableStateOf(existing?.category ?: categories.firstOrNull()?.name ?: "Clothes") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var imageUri by remember { mutableStateOf(existing?.primaryImage ?: "preset:default") }
    var allowsCustomName by remember { mutableStateOf(existing?.allowsCustomName ?: false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = ImageStorageHelper.persistPickedImage(context, uri, "product_images") ?: uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pick Product Photo")
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (Rs.) *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = oldPrice, onValueChange = { oldPrice = it }, label = { Text("Old Price") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock *") }, singleLine = true)
                Text("Category:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = category == cat.name, onClick = { category = cat.name }, label = { Text(cat.name) })
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowsCustomName, onCheckedChange = { allowsCustomName = it })
                    Text("Allow Custom Name Printing", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && p > 0) {
                        onSave(
                            ZmProduct(
                                id = existing?.id ?: "",
                                sellerId = seller.id,
                                shopName = seller.shopName,
                                name = name.trim(),
                                description = description.trim().ifBlank { "Quality product from ${seller.shopName}." },
                                category = category,
                                price = p,
                                oldPrice = oldPrice.toDoubleOrNull() ?: 0.0,
                                stock = stock.toIntOrNull() ?: 10,
                                imageUrls = imageUri,
                                allowsCustomName = allowsCustomName || category == "Custom Name Products",
                                approvalStatus = existing?.approvalStatus ?: ProductApproval.PENDING
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
            ) {
                Text("Save", color = ZmWhite)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
