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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.ui.components.ProductCard
import com.example.ui.components.ZmFooter
import com.example.ui.components.ZmImage
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.model.ZmProduct
import com.example.ui.theme.*
import com.example.util.ImageStorageHelper
import com.example.util.IntentHelper

@Composable
fun ProductDetailScreen(
    product: ZmProduct?,
    relatedProducts: List<ZmProduct>,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onAddToCart: (ZmProduct, Int, String, String) -> Unit,
    onBuyNow: (ZmProduct, Int, String, String) -> Unit,
    onRelatedProductClick: (String) -> Unit
) {
    val context = LocalContext.current

    if (product == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ZmGreen)
        }
        return
    }

    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var customName by remember(product.id) { mutableStateOf("") }
    var customPhotoUri by remember(product.id) { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.persistPickedImage(context, uri, "custom_uploads")
            customPhotoUri = savedPath ?: uri.toString()
        }
    }

    val images = product.imageList
    val pagerState = rememberPagerState(pageCount = { images.size })

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("product_detail_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Big Product Image Gallery
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ZmImage(
                        imageSpec = images[page],
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (product.discountPercent > 0) {
                    Surface(
                        color = ZmRed,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "-${product.discountPercent}% OFF",
                            color = ZmWhite,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(images.size) { i ->
                            val selected = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 9.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) ZmGreen else ZmWhite.copy(alpha = 0.7f))
                            )
                        }
                    }
                }
            }
        }

        // 2. Price, Title, Stock, Shop Name
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = IntentHelper.formatRs(product.price),
                                style = MaterialTheme.typography.headlineMedium,
                                color = ZmRed,
                                fontWeight = FontWeight.Bold
                            )
                            if (product.oldPrice > product.price) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = IntentHelper.formatRs(product.oldPrice),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = ZmMutedText
                                )
                            }
                        }
                        Surface(
                            color = if (product.stock > 0) ZmGreenLight else ZmRedLight,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (product.stock > 0) "In Stock: ${product.stock}" else "Out of Stock",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (product.stock > 0) ZmGreenDark else ZmRedDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = ZmGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = product.shopName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = ZmGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${String.format("%.1f", product.rating)} (${product.reviewCount} reviews)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Custom Name Box & Upload Photo Option
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ZmGreen.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ZmGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom Name & Photo Printing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Want your name or custom photo printed on this product? Enter below (optional).",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZmMutedText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Type Custom Name for Printing") },
                        placeholder = { Text("e.g., Mujtaba Ansari") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_name_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("upload_custom_photo_button")
                        ) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (customPhotoUri.isBlank()) "Upload Photo" else "Change Photo")
                        }
                        if (customPhotoUri.isNotBlank()) {
                            Box {
                                ZmImage(
                                    imageSpec = customPhotoUri,
                                    contentDescription = "Uploaded custom photo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                IconButton(
                                    onClick = { customPhotoUri = "" },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .background(ZmRed, CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove photo",
                                        tint = ZmWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Quantity Selector + Add to Cart + Buy Now + WhatsApp Order
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("qty_minus_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                            }
                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            FilledTonalIconButton(
                                onClick = { if (quantity < product.stock) quantity++ },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("qty_plus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onAddToCart(product, quantity, customName, customPhotoUri) },
                            enabled = product.stock > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = ZmGreen, contentColor = ZmWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("detail_add_to_cart_button")
                        ) {
                            Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to Cart", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onBuyNow(product, quantity, customName, customPhotoUri) },
                            enabled = product.stock > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = ZmRed, contentColor = ZmWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("detail_buy_now_button")
                        ) {
                            Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buy Now", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val customPart = if (customName.isNotBlank()) "\nCustom Name: $customName" else ""
                            val msg = "Assalam-o-Alaikum ZM Bazaar!\nI want to order:\nProduct: ${product.name}\nShop: ${product.shopName}\nPrice: ${IntentHelper.formatRs(product.price)}\nQuantity: $quantity$customPart"
                            IntentHelper.openWhatsApp(context, msg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZmWhatsAppGreen, contentColor = ZmWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("detail_whatsapp_order_button")
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp Order (03558279470)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Related Products
        if (relatedProducts.isNotEmpty()) {
            item {
                Text(
                    text = "Related Products",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(relatedProducts, key = { "rel_${it.id}" }) { rel ->
                        ProductCard(
                            product = rel,
                            onProductClick = { onRelatedProductClick(rel.id) },
                            onAddToCartClick = { onAddToCart(rel, 1, "", "") },
                            modifier = Modifier.width(168.dp)
                        )
                    }
                }
            }
        }

        item {
            ZmFooter(adminPaymentAccounts = adminPaymentAccounts)
        }
    }
}
