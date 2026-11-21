package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.credentials.CredentialManager
import com.example.data.local.SellerStatus
import com.example.data.local.ZmBazaarDatabase
import com.example.data.remote.GoogleSignInButton
import com.example.data.remote.signOutFromFirebase
import com.example.ui.components.ShareAndDownloadAppDialog
import com.example.ui.components.ZmFooter
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.model.ZmSeller
import com.example.ui.model.ZmUser
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onLoginSuccess: () -> Unit,
    onBecomeSellerClick: () -> Unit = {}
) {
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("login_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ZmGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = ZmGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Text(
                        "Sign in to ZM Bazaar",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Sign in securely with your Google account to place orders, sync your cart across devices, track deliveries, apply as a Seller, or manage ZM Bazaar Admin HQ.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZmMutedText
                    )

                    if (!errorMsg.isNullOrBlank()) {
                        Surface(
                            color = ZmRedLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg!!,
                                color = ZmRedDark,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    GoogleSignInButton(
                        label = "Continue with Google",
                        onAuthSuccess = {
                            errorMsg = null
                            onLoginSuccess()
                        },
                        onAuthError = { msg -> errorMsg = msg }
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Role Access (Automatic via Google):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("• Customer: Instant access to checkout & order tracking", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            Text("• Seller: Apply in Become a Seller -> Admin Approval", style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
                            Text("• Admin HQ: ${ZmBazaarDatabase.ADMIN_EMAIL}", style = MaterialTheme.typography.bodySmall, color = ZmGreenDark)
                        }
                    }

                    OutlinedButton(
                        onClick = onBecomeSellerClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Learn About Selling on ZM Bazaar")
                    }
                }
            }
        }
        item { ZmFooter(adminPaymentAccounts = adminPaymentAccounts) }
    }
}

@Composable
fun AccountScreen(
    currentUser: ZmUser?,
    currentSeller: ZmSeller?,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onLoginClick: () -> Unit,
    onMyOrdersClick: () -> Unit,
    onBecomeSellerClick: () -> Unit,
    onSellerDashboardClick: () -> Unit,
    onAdminPanelClick: () -> Unit,
    onUpdateProfile: (fullName: String, phone: String, city: String) -> Unit,
    onLogoutComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showShareAppDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("account_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (currentUser == null) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = ZmGreen, modifier = Modifier.size(64.dp))
                        Text("Welcome to ZM Bazaar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Sign in with Google to place orders, track deliveries, apply as a Seller, or manage your store.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ZmMutedText
                        )
                        Button(
                            onClick = onLoginClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ZmGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("account_login_button")
                        ) {
                            Icon(Icons.Filled.Login, contentDescription = null, tint = ZmWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", color = ZmWhite, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        OutlinedButton(
                            onClick = onBecomeSellerClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Filled.Storefront, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Become a Seller Info")
                        }
                        OutlinedButton(
                            onClick = { showShareAppDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("guest_share_download_app_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = ZmGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download / Share App (APK & WhatsApp)")
                        }
                    }
                }
            }
        } else {
            val isAdmin = currentUser.email.equals(ZmBazaarDatabase.ADMIN_EMAIL, ignoreCase = true) && currentUser.isEmailVerified
            val isApprovedSeller = currentSeller?.status == SellerStatus.APPROVED

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ZmBlack)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ZmGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.fullName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = ZmWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentUser.fullName, style = MaterialTheme.typography.titleLarge, color = ZmWhite, fontWeight = FontWeight.Bold)
                            Text(currentUser.email, style = MaterialTheme.typography.bodySmall, color = ZmWhite.copy(alpha = 0.8f))
                            val contactLine = listOf(currentUser.phone, currentUser.city).filter { it.isNotBlank() }.joinToString(" • ")
                            if (contactLine.isNotBlank()) {
                                Text(contactLine, style = MaterialTheme.typography.bodySmall, color = ZmWhite.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(color = if (isAdmin) ZmRed else ZmGreen, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = when {
                                        isAdmin -> "ADMIN"
                                        isApprovedSeller -> "VERIFIED SELLER"
                                        else -> currentUser.role
                                    },
                                    color = ZmWhite,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showEditProfileDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit profile", tint = ZmWhite)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccountActionTile(
                        title = "Edit Phone & Delivery City",
                        subtitle = if (currentUser.phone.isBlank()) "Add your phone number for faster checkout" else "${currentUser.phone} • ${currentUser.city}",
                        icon = Icons.Filled.EditLocationAlt,
                        accent = ZmGreenDark,
                        tag = "account_edit_profile_tile",
                        onClick = { showEditProfileDialog = true }
                    )

                    AccountActionTile(
                        title = "My Orders & Tracking",
                        subtitle = "Track Pending, Confirmed, Shipped & Delivered orders",
                        icon = Icons.Filled.LocalShipping,
                        accent = ZmGreen,
                        tag = "account_my_orders_button",
                        onClick = onMyOrdersClick
                    )

                    AccountActionTile(
                        title = "Download & Share ZM Bazaar App",
                        subtitle = "Save ZM-Bazaar.apk to Phone Downloads or Share to WhatsApp / Files",
                        icon = Icons.Filled.DownloadForOffline,
                        accent = ZmWhatsAppGreen,
                        tag = "account_share_download_app_button",
                        onClick = { showShareAppDialog = true }
                    )

                    if (isApprovedSeller) {
                        AccountActionTile(
                            title = "Seller Dashboard (${currentSeller?.shopName ?: ""})",
                            subtitle = "Manage products, orders, earnings & payment account",
                            icon = Icons.Filled.Storefront,
                            accent = ZmGreenDark,
                            tag = "account_seller_dashboard_button",
                            onClick = onSellerDashboardClick
                        )
                    } else {
                        AccountActionTile(
                            title = "Become a Seller",
                            subtitle = when (currentSeller?.status) {
                                SellerStatus.PENDING -> "Application Pending Admin Approval"
                                SellerStatus.REJECTED -> "Application Status: Rejected (Tap to Re-apply)"
                                else -> "Open your store on ZM Bazaar (10% Commission)"
                            },
                            icon = Icons.Filled.Storefront,
                            accent = ZmBlack,
                            tag = "account_become_seller_button",
                            onClick = onBecomeSellerClick
                        )
                    }

                    // STRICT: Only verified ansariproducts05@gmail.com can see and open Admin Panel
                    if (isAdmin) {
                        AccountActionTile(
                            title = "Admin Panel (ZM Bazaar HQ)",
                            subtitle = "Orders, 10% Commission, Sellers, Products, Categories, Banners, Payment Accounts",
                            icon = Icons.Filled.AdminPanelSettings,
                            accent = ZmRed,
                            tag = "account_admin_panel_button",
                            onClick = onAdminPanelClick
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            signOutFromFirebase(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onLogoutComplete
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZmRed, contentColor = ZmWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 50.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item { ZmFooter(adminPaymentAccounts = adminPaymentAccounts) }
    }

    if (showEditProfileDialog && currentUser != null) {
        var name by remember { mutableStateOf(currentUser.fullName) }
        var phone by remember { mutableStateOf(currentUser.phone) }
        var city by remember { mutableStateOf(currentUser.city) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Update Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onUpdateProfile(name.trim(), phone.trim(), city.trim())
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
                ) { Text("Save", color = ZmWhite) }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showShareAppDialog) {
        ShareAndDownloadAppDialog(onDismiss = { showShareAppDialog = false })
    }
}

@Composable
private fun AccountActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ZmMutedText)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ZmMutedText)
        }
    }
}
