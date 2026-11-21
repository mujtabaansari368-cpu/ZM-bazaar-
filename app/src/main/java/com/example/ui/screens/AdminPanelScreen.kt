package com.example.ui.screens

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
import com.example.data.local.*
import com.example.ui.components.ZmFooter
import com.example.ui.components.ZmImage
import com.example.ui.model.*
import com.example.ui.theme.*
import com.example.util.IntentHelper

private enum class AdminSection(val label: String) {
    OVERVIEW("Overview"),
    ORDERS("Orders"),
    SELLERS("Sellers"),
    PRODUCTS("Products"),
    CATEGORIES("Categories"),
    CUSTOMERS("Customers"),
    BANNERS("Banners"),
    PAYMENTS("My Payment Accounts")
}

@Composable
fun AdminPanelScreen(
    currentUser: ZmUser?,
    orders: List<ZmOrder>,
    orderItems: List<ZmOrderItem>,
    sellers: List<ZmSeller>,
    products: List<ZmProduct>,
    categories: List<ZmCategory>,
    users: List<ZmUser>,
    banners: List<ZmBanner>,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onUpdateOrderStatus: (String, String) -> Unit,
    onApproveSeller: (ZmSeller) -> Unit,
    onRejectSeller: (ZmSeller) -> Unit,
    onSendPaymentToSeller: (ZmSeller, Double) -> Unit,
    onUpdateProductApproval: (String, String) -> Unit,
    onDeleteProduct: (ZmProduct) -> Unit,
    onSaveCategory: (ZmCategory) -> Unit,
    onDeleteCategory: (ZmCategory) -> Unit,
    onSaveBanner: (ZmBanner) -> Unit,
    onDeleteBanner: (ZmBanner) -> Unit,
    onSaveAdminPaymentAccount: (ZmPaymentAccount) -> Unit,
    onDeleteAdminPaymentAccount: (ZmPaymentAccount) -> Unit
) {
    // Strict Admin Guard: ONLY ansariproducts05@gmail.com with verified email
    if (currentUser == null || !currentUser.email.equals(ZmBazaarDatabase.ADMIN_EMAIL, ignoreCase = true) || !currentUser.isEmailVerified) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = ZmRed, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Admin Access Restricted", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Only ${ZmBazaarDatabase.ADMIN_EMAIL} can open the Admin Panel.", style = MaterialTheme.typography.bodyMedium, color = ZmMutedText)
        }
        return
    }

    var activeSection by remember { mutableStateOf(AdminSection.OVERVIEW) }
    var screenshotPreviewUri by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ZmCategory?>(null) }
    var showBannerDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<ZmBanner?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<ZmPaymentAccount?>(null) }

    val customers = remember(users) { users.filter { it.role == UserRole.CUSTOMER } }
    val totalSales = remember(orders) { orders.filter { it.status != OrderStatus.CANCELLED }.sumOf { it.subtotal } }
    val totalCommission = remember(orders) { orders.sumOf { it.commissionAmount } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("admin_panel_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Surface(color = ZmBlack, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = ZmRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ZM Bazaar Admin Panel", style = MaterialTheme.typography.titleLarge, color = ZmWhite, fontWeight = FontWeight.Bold)
                    }
                    Text("Authorized: ${currentUser.email}", style = MaterialTheme.typography.bodySmall, color = ZmGreen)
                }
            }
        }

        // Section Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AdminSection.entries) { sec ->
                    FilterChip(
                        selected = activeSection == sec,
                        onClick = { activeSection = sec },
                        label = { Text(sec.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZmGreen,
                            selectedLabelColor = ZmWhite
                        ),
                        modifier = Modifier.testTag("admin_tab_${sec.name}")
                    )
                }
            }
        }

        when (activeSection) {
            AdminSection.OVERVIEW -> {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiMiniCard("Total Orders", orders.size.toString(), ZmBlack, Modifier.weight(1f))
                            KpiMiniCard("Total Sellers", sellers.size.toString(), ZmGreen, Modifier.weight(1f))
                            KpiMiniCard("Customers", customers.size.toString(), ZmGreenDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiMiniCard("Total Sales", IntentHelper.formatRs(totalSales), ZmBlack, Modifier.weight(1f))
                            KpiMiniCard("Total Commission (10%)", IntentHelper.formatRs(totalCommission), ZmRed, Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Text(
                        text = "Seller Commission & Payouts (90% to Seller / 10% ZM Bazaar)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }

                items(sellers, key = { "payout_${it.id}" }) { seller ->
                    val sellerItems = orderItems.filter { it.sellerId == seller.id }
                    val pendingPayout = sellerItems.filter { it.payoutStatus == PayoutStatus.PENDING_PAYOUT }.sumOf { it.sellerEarningAmount }
                    val paidPayout = sellerItems.filter { it.payoutStatus == PayoutStatus.PAID }.sumOf { it.sellerEarningAmount }
                    val sellerCommission = sellerItems.sumOf { it.commissionAmount }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(seller.shopName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Account: ${seller.paymentMethod} - ${seller.paymentAccountNumber}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("10% Comm: ${IntentHelper.formatRs(sellerCommission)}", style = MaterialTheme.typography.labelMedium, color = ZmRed)
                                Text("Paid: ${IntentHelper.formatRs(paidPayout)}", style = MaterialTheme.typography.labelMedium, color = ZmGreenDark)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { onSendPaymentToSeller(seller, pendingPayout) },
                                enabled = pendingPayout > 0.0,
                                colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                                modifier = Modifier.fillMaxWidth().testTag("send_payment_seller_${seller.id}")
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (pendingPayout > 0.0) {
                                        "Send Payment to Seller (${IntentHelper.formatRs(pendingPayout)})"
                                    } else {
                                        "No Pending Payout"
                                    },
                                    color = ZmWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            AdminSection.ORDERS -> {
                items(orders, key = { "ord_${it.id}" }) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.orderCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                StatusPill(order.status)
                            }
                            Text("${order.customerName} • ${order.phone} • ${order.city}", style = MaterialTheme.typography.bodySmall)
                            Text("Address: ${order.address}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            Text("Total: ${IntentHelper.formatRs(order.grandTotal)} • ${order.paymentMethod}", style = MaterialTheme.typography.labelMedium, color = ZmRed)
                            if (order.commissionAmount > 0) {
                                Text(
                                    "10% Commission: ${IntentHelper.formatRs(order.commissionAmount)} | Seller Earning: ${IntentHelper.formatRs(order.sellerEarningAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZmGreenDark
                                )
                            }
                            if (order.paymentScreenshotUri.isNotBlank()) {
                                TextButton(onClick = { screenshotPreviewUri = order.paymentScreenshotUri }) {
                                    Icon(Icons.Filled.Image, contentDescription = null, tint = ZmGreen)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("View Payment Screenshot", color = ZmGreen)
                                }
                            }
                            Text("Change Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(OrderStatus.allStatuses) { st ->
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

            AdminSection.SELLERS -> {
                items(sellers, key = { "sel_${it.id}" }) { seller ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(seller.shopName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = if (seller.status == SellerStatus.APPROVED) ZmGreenLight else ZmRedLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        seller.status,
                                        color = if (seller.status == SellerStatus.APPROVED) ZmGreenDark else ZmRedDark,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text("Owner: ${seller.ownerName} • Phone: ${seller.phone}", style = MaterialTheme.typography.bodySmall)
                            Text("CNIC: ${seller.cnic}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            Text("Address: ${seller.shopAddress}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            Surface(color = ZmBlack, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    text = "Seller Payment Account: ${seller.paymentMethod} | ${seller.paymentAccountNumber} (${seller.paymentAccountTitle})",
                                    color = ZmWhite,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApproveSeller(seller) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Approve", color = ZmWhite) }
                                Button(
                                    onClick = { onRejectSeller(seller) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ZmRed),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reject", color = ZmWhite) }
                            }
                        }
                    }
                }
            }

            AdminSection.PRODUCTS -> {
                items(products, key = { "aprod_${it.id}" }) { prod ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ZmImage(
                                    imageSpec = prod.primaryImage,
                                    contentDescription = prod.name,
                                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${prod.shopName} • ${prod.category} • ${IntentHelper.formatRs(prod.price)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Approval: ${prod.approvalStatus}", style = MaterialTheme.typography.labelSmall, color = ZmGreenDark)
                                }
                                IconButton(onClick = { onDeleteProduct(prod) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete product", tint = ZmRed)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                OutlinedButton(
                                    onClick = { onUpdateProductApproval(prod.id, ProductApproval.APPROVED) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Approve", color = ZmGreen) }
                                OutlinedButton(
                                    onClick = { onUpdateProductApproval(prod.id, ProductApproval.REJECTED) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reject", color = ZmRed) }
                            }
                        }
                    }
                }
            }

            AdminSection.CATEGORIES -> {
                item {
                    Button(
                        onClick = { editingCategory = null; showCategoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Category", color = ZmWhite)
                    }
                }
                items(categories, key = { "cat_${it.id}" }) { cat ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editingCategory = cat; showCategoryDialog = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit category", tint = ZmGreen)
                            }
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete category", tint = ZmRed)
                            }
                        }
                    }
                }
            }

            AdminSection.CUSTOMERS -> {
                items(customers, key = { "cust_${it.id}" }) { cust ->
                    val count = orders.count { it.customerId == cust.id }
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(cust.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${cust.email} • ${cust.phone}", style = MaterialTheme.typography.bodySmall)
                            Text("${cust.city} • Orders: $count", style = MaterialTheme.typography.labelSmall, color = ZmGreenDark)
                        }
                    }
                }
            }

            AdminSection.BANNERS -> {
                item {
                    Button(
                        onClick = { editingBanner = null; showBannerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Slider Banner", color = ZmWhite)
                    }
                }
                items(banners, key = { "ban_${it.id}" }) { ban ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            ZmImage(
                                imageSpec = ban.imageUri,
                                contentDescription = ban.title,
                                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ban.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(ban.subtitle, style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                                }
                                IconButton(onClick = { editingBanner = ban; showBannerDialog = true }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Change banner", tint = ZmGreen)
                                }
                                IconButton(onClick = { onDeleteBanner(ban) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete banner", tint = ZmRed)
                                }
                            }
                        }
                    }
                }
            }

            AdminSection.PAYMENTS -> {
                item {
                    Button(
                        onClick = { editingAccount = null; showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                        modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("admin_add_account_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Payment Account", color = ZmWhite, fontWeight = FontWeight.Bold)
                    }
                }
                items(adminPaymentAccounts, key = { "acc_${it.id}" }) { acc ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        border = BorderStroke(1.dp, ZmGreen.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acc.providerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(if (acc.isActive) "ACTIVE" else "HIDDEN", style = MaterialTheme.typography.labelSmall, color = if (acc.isActive) ZmGreen else ZmRed)
                            }
                            Text("Number: ${acc.accountNumber}", style = MaterialTheme.typography.bodyLarge, color = ZmGreenDark, fontWeight = FontWeight.Bold)
                            Text("Title: ${acc.accountTitle}", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { editingAccount = acc; showPaymentDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ZmBlack),
                                    modifier = Modifier.weight(1f).testTag("admin_change_account_${acc.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Account", color = ZmWhite)
                                }
                                OutlinedButton(
                                    onClick = { onDeleteAdminPaymentAccount(acc) },
                                    modifier = Modifier.weight(0.5f)
                                ) {
                                    Text("Delete", color = ZmRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { ZmFooter(adminPaymentAccounts = adminPaymentAccounts) }
    }

    if (screenshotPreviewUri != null) {
        AlertDialog(
            onDismissRequest = { screenshotPreviewUri = null },
            title = { Text("Payment Screenshot") },
            text = {
                ZmImage(
                    imageSpec = screenshotPreviewUri!!,
                    contentDescription = "Customer payment screenshot",
                    modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(10.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { screenshotPreviewUri = null }) { Text("Close") }
            }
        )
    }

    if (showCategoryDialog) {
        AdminCategoryDialog(
            existing = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onSave = { cat ->
                onSaveCategory(cat)
                showCategoryDialog = false
            }
        )
    }

    if (showBannerDialog) {
        AdminBannerDialog(
            existing = editingBanner,
            onDismiss = { showBannerDialog = false },
            onSave = { ban ->
                onSaveBanner(ban)
                showBannerDialog = false
            }
        )
    }

    if (showPaymentDialog) {
        AdminPaymentAccountDialog(
            existing = editingAccount,
            onDismiss = { showPaymentDialog = false },
            onSave = { acc ->
                onSaveAdminPaymentAccount(acc)
                showPaymentDialog = false
            }
        )
    }
}
