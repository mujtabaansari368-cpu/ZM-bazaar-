package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.model.ZmProduct
import com.example.ui.model.ZmUser
import com.example.ui.theme.*
import com.example.util.IntentHelper

fun categoryIconFor(iconKey: String): ImageVector = when (iconKey.lowercase()) {
    "clothes" -> Icons.Filled.Checkroom
    "shoes" -> Icons.Filled.DirectionsRun
    "cosmetics" -> Icons.Filled.FaceRetouchingNatural
    "mobile" -> Icons.Filled.PhoneAndroid
    "electronics" -> Icons.Filled.Devices
    "home" -> Icons.Filled.Chair
    "kids" -> Icons.Filled.ChildCare
    "grocery" -> Icons.Filled.LocalGroceryStore
    "custom" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.Category
}

@Composable
fun ZmImage(
    imageSpec: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    when {
        imageSpec.startsWith("drawable:") -> {
            val resId = when (imageSpec.removePrefix("drawable:")) {
                "img_banner_flash_sale" -> R.drawable.img_banner_flash_sale
                "img_banner_custom_gifts" -> R.drawable.img_banner_custom_gifts
                "img_banner_kashmir_bazaar" -> R.drawable.img_banner_kashmir_bazaar
                "img_seller_banner" -> R.drawable.img_seller_banner
                "ic_zmbazaar_logo" -> R.drawable.ic_zmbazaar_logo
                else -> R.drawable.img_banner_kashmir_bazaar
            }
            Image(
                painter = painterResource(id = resId),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        imageSpec.startsWith("preset:") -> {
            ProductPresetGraphic(
                presetKey = imageSpec.removePrefix("preset:"),
                modifier = modifier
            )
        }
        imageSpec.isNotBlank() -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSpec)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        else -> {
            ProductPresetGraphic(presetKey = "default", modifier = modifier)
        }
    }
}

@Composable
fun ProductPresetGraphic(presetKey: String, modifier: Modifier = Modifier) {
    val (bgColors, icon, accentLabel) = when (presetKey) {
        "earbuds" -> Triple(listOf(Color(0xFF0A2E1E), Color(0xFF00A651)), Icons.Filled.Headphones, "AUDIO")
        "custom_mug" -> Triple(listOf(Color(0xFF7F1D1D), Color(0xFFE31E24)), Icons.Filled.Coffee, "CUSTOM MUG")
        "kurta" -> Triple(listOf(Color(0xFF064E3B), Color(0xFF10B981)), Icons.Filled.Checkroom, "FASHION")
        "sneakers" -> Triple(listOf(Color(0xFF1E293B), Color(0xFFE31E24)), Icons.Filled.DirectionsRun, "FOOTWEAR")
        "custom_wallet" -> Triple(listOf(Color(0xFF3E2723), Color(0xFF795548)), Icons.Filled.AccountBalanceWallet, "ENGRAVED")
        "cosmetics" -> Triple(listOf(Color(0xFF831843), Color(0xFFEC4899)), Icons.Filled.FaceRetouchingNatural, "BEAUTY")
        "smartwatch" -> Triple(listOf(Color(0xFF000000), Color(0xFF1F2937)), Icons.Filled.Watch, "SMART TECH")
        "kettle" -> Triple(listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)), Icons.Filled.Kitchen, "HOME")
        "kids_set" -> Triple(listOf(Color(0xFF581C87), Color(0xFFA855F7)), Icons.Filled.ChildCare, "KIDS")
        "honey" -> Triple(listOf(Color(0xFF78350F), Color(0xFFF59E0B)), Icons.Filled.LocalGroceryStore, "KASHMIR PURE")
        "powerbank" -> Triple(listOf(Color(0xFF0F172A), Color(0xFF00A651)), Icons.Filled.BatteryChargingFull, "20000mAh")
        "custom_shirt" -> Triple(listOf(Color(0xFF991B1B), Color(0xFF00A651)), Icons.Filled.AutoAwesome, "CUSTOM PRINT")
        else -> Triple(listOf(Color(0xFF065F46), Color(0xFF00A651)), Icons.Filled.ShoppingBag, "ZM BAZAAR")
    }

    Box(
        modifier = modifier.background(Brush.linearGradient(bgColors)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        ) {
            Text(
                text = accentLabel,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ZmTopHeader(
    currentUser: ZmUser?,
    cartCount: Int,
    unreadNotifications: Int,
    searchQuery: String,
    showSearchBar: Boolean = true,
    showBackButton: Boolean = false,
    pageTitle: String? = null,
    onBackClick: () -> Unit = {},
    onLogoClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    onCartClick: () -> Unit,
    onLoginOrAccountClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Surface(
        color = ZmGreen,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("header_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ZmWhite
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLogoClick() }
                            .padding(end = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_zmbazaar_logo),
                            contentDescription = "ZM Bazaar Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, ZmWhite, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = pageTitle ?: "ZM Bazaar",
                                style = MaterialTheme.typography.titleMedium,
                                color = ZmWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Pakistan Ka No.1 Online Bazaar",
                                style = MaterialTheme.typography.labelSmall,
                                color = ZmWhite.copy(alpha = 0.9f),
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Notification Bell
                    Box {
                        IconButton(
                            onClick = onNotificationsClick,
                            modifier = Modifier.testTag("notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = ZmWhite
                            )
                        }
                        if (unreadNotifications > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(ZmRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadNotifications.coerceAtMost(9).toString(),
                                    color = ZmWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Cart Button with Badge
                    Box {
                        IconButton(
                            onClick = onCartClick,
                            modifier = Modifier.testTag("header_cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Cart",
                                tint = ZmWhite
                            )
                        }
                        if (cartCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                    .clip(CircleShape)
                                    .background(ZmRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cartCount.coerceAtMost(99).toString(),
                                    color = ZmWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 3.dp)
                                )
                            }
                        }
                    }

                    // Login / User Name Pill
                    Surface(
                        onClick = onLoginOrAccountClick,
                        color = if (currentUser != null) ZmBlack else ZmRed,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .testTag("header_auth_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = ZmWhite,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser?.fullName?.split(" ")?.firstOrNull() ?: "Login",
                                style = MaterialTheme.typography.labelMedium,
                                color = ZmWhite,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (showSearchBar) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ZmWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = ZmGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = ZmBlack),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit(searchQuery) }),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search products, shops in ZM Bazaar...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ZmMutedText
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_input")
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    tint = ZmMutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = { onSearchSubmit(searchQuery) },
                            color = ZmRed,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("search_submit_button")
                        ) {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.labelMedium,
                                color = ZmWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ZmProduct,
    onProductClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
            ) {
                ZmImage(
                    imageSpec = product.primaryImage,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (product.discountPercent > 0) {
                    Surface(
                        color = ZmRed,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            color = ZmWhite,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                if (product.allowsCustomName) {
                    Surface(
                        color = ZmBlack.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "Custom Name",
                            color = ZmWhite,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = IntentHelper.formatRs(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = ZmRed,
                        fontWeight = FontWeight.Bold
                    )
                    if (product.oldPrice > product.price) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = IntentHelper.formatRs(product.oldPrice),
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = ZmMutedText,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = ZmGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = product.shopName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = ZmGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", product.rating),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddToCartClick,
                    enabled = product.stock > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZmGreen,
                        contentColor = ZmWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("add_to_cart_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (product.stock > 0) "Add to Cart" else "Out of Stock",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ZmFooter(
    adminPaymentAccounts: List<ZmPaymentAccount>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showShareApkDialog by remember { mutableStateOf(false) }

    val paymentLine = remember(adminPaymentAccounts) {
        if (adminPaymentAccounts.isEmpty()) {
            "Easypaisa 03455368275 | JazzCash 03285690902 | Meezan Bank 12930112996287"
        } else {
            adminPaymentAccounts.filter { it.isActive }.joinToString(" | ") {
                "${it.providerName} ${it.accountNumber}"
            }
        }
    }

    Surface(
        color = ZmBlack,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = ZmGreen, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "ZM",
                        color = ZmWhite,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ZM Bazaar — Pakistan Ka No.1 Online Bazaar",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZmWhite
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ZM Bazaar, Barnala, Azad Kashmir, Pakistan",
                style = MaterialTheme.typography.bodySmall,
                color = ZmWhite.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.testTag("footer_about_button")
                ) {
                    Text("About", color = ZmGreen, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = { showContactDialog = true },
                    modifier = Modifier.testTag("footer_contact_button")
                ) {
                    Text("Contact", color = ZmGreen, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = { IntentHelper.openWhatsApp(context) },
                    modifier = Modifier.testTag("footer_whatsapp_button")
                ) {
                    Text("WhatsApp", color = ZmWhatsAppGreen, style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = { showShareApkDialog = true },
                    modifier = Modifier.testTag("footer_share_apk_button")
                ) {
                    Text("Download / Share", color = ZmRed, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                text = "WhatsApp: https://wa.me/923558279470",
                style = MaterialTheme.typography.bodySmall,
                color = ZmWhatsAppGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { IntentHelper.openWhatsApp(context) }
                    .padding(vertical = 2.dp)
            )
            Text(
                text = "Email: ansariproducts05@gmail.com",
                style = MaterialTheme.typography.bodySmall,
                color = ZmWhite.copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { IntentHelper.openEmail(context) }
                    .padding(vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = ZmCharcoal,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ZmGreen.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Payment: $paymentLine",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZmWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Copyright © 2026 ZM Bazaar",
                style = MaterialTheme.typography.labelSmall,
                color = ZmWhite.copy(alpha = 0.65f)
            )
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About ZM Bazaar") },
            text = {
                Text(
                    "ZM Bazaar is Pakistan Ka No.1 Online Bazaar headquartered in Barnala, Azad Kashmir, Pakistan.\n\n" +
                        "We connect verified local sellers across Clothes, Shoes, Cosmetics, Electronics, Grocery, and Custom Name Products with customers nationwide with Cash on Delivery, Easypaisa, JazzCash, and Meezan Bank."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("Contact ZM Bazaar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Address: ZM Bazaar, Barnala, Azad Kashmir, Pakistan")
                    Text("WhatsApp: 03558279470 (https://wa.me/923558279470)")
                    Text("Email: ansariproducts05@gmail.com")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showContactDialog = false
                        IntentHelper.openWhatsApp(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZmWhatsAppGreen)
                ) {
                    Text("Chat on WhatsApp", color = ZmWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactDialog = false }) { Text("Close") }
            }
        )
    }

    if (showShareApkDialog) {
        ShareAndDownloadAppDialog(onDismiss = { showShareApkDialog = false })
    }
}

@Composable
fun ShareAndDownloadAppDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var downloadLink by remember { mutableStateOf(IntentHelper.getSavedAppDownloadUrl(context)) }

    val saveApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) {
            val ok = IntentHelper.writeInstalledApkToUri(context, uri)
            Toast.makeText(
                context,
                if (ok) "ZM-Bazaar.apk saved to your selected Downloads / Files folder!" else "Could not save APK file.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.InstallMobile, contentDescription = null, tint = ZmGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download & Share App", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Save ZM-Bazaar.apk directly to your Phone Downloads or share the APK file / link to WhatsApp and Files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZmMutedText
                )

                Button(
                    onClick = { saveApkLauncher.launch("ZM-Bazaar.apk") },
                    colors = ButtonDefaults.buttonColors(containerColor = ZmGreen, contentColor = ZmWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("dialog_save_apk_downloads")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save APK to Downloads", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { IntentHelper.shareApkFile(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ZmWhatsAppGreen, contentColor = ZmWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("dialog_share_apk_whatsapp")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share APK (WhatsApp / Files)", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = downloadLink,
                    onValueChange = {
                        downloadLink = it
                        IntentHelper.saveAppDownloadUrl(context, it)
                    },
                    label = { Text("Direct Download Link (Optional)") },
                    placeholder = { Text("https://github.com/user/repo/releases/latest/download/ZM-Bazaar.apk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("app_download_link_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { IntentHelper.openDownloadUrl(context, downloadLink) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Link", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { IntentHelper.shareAppLink(context, downloadLink) },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = ZmWhatsAppGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Link", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun WhatsAppFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = ZmWhatsAppGreen,
        contentColor = ZmWhite,
        icon = {
            Icon(
                imageVector = Icons.Filled.Chat,
                contentDescription = "WhatsApp"
            )
        },
        text = {
            Text("WhatsApp", fontWeight = FontWeight.Bold)
        },
        modifier = modifier.testTag("floating_whatsapp_button")
    )
}
