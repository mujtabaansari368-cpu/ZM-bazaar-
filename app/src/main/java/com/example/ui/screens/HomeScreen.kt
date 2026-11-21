package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SellerStatus
import com.example.ui.components.*
import com.example.ui.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    banners: List<ZmBanner>,
    categories: List<ZmCategory>,
    products: List<ZmProduct>,
    flashSaleSecondsLeft: Int,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    currentSeller: ZmSeller?,
    onCategoryClick: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onAddToCart: (ZmProduct) -> Unit,
    onViewAllShopClick: () -> Unit,
    onBecomeSellerClick: () -> Unit,
    onSellerDashboardClick: () -> Unit
) {
    val flashProducts = remember(products) { products.filter { it.isFlashSale } }
    val featuredProducts = remember(products) { products.filter { it.isFeatured }.ifEmpty { products } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Home Slider with 3 Banners
        item {
            if (banners.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { banners.size })
                LaunchedEffect(banners.size) {
                    if (banners.size > 1) {
                        while (true) {
                            delay(4000L)
                            val next = (pagerState.currentPage + 1) % banners.size
                            pagerState.animateScrollToPage(next)
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) { page ->
                        val banner = banners[page]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (banner.targetCategory.isNotBlank()) {
                                        onCategoryClick(banner.targetCategory)
                                    } else {
                                        onViewAllShopClick()
                                    }
                                }
                                .testTag("home_banner_$page")
                        ) {
                            ZmImage(
                                imageSpec = banner.imageUri,
                                contentDescription = banner.title,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.78f),
                                                Color.Black.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(16.dp)
                                    .fillMaxWidth(0.75f)
                            ) {
                                Surface(
                                    color = ZmRed,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = banner.badgeText,
                                        color = ZmWhite,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = banner.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ZmWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = banner.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ZmWhite.copy(alpha = 0.9f),
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(banners.size) { index ->
                            val active = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (active) 20.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) ZmGreen else ZmWhite.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }
        }

        // 2. Categories Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onViewAllShopClick,
                        modifier = Modifier.testTag("view_all_categories_btn")
                    ) {
                        Text("Shop All", color = ZmGreen, fontWeight = FontWeight.Bold)
                    }
                }

                val rows = categories.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowCats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCats.forEach { cat ->
                                val accent = Color(cat.accentHex)
                                Surface(
                                    onClick = { onCategoryClick(cat.name) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("category_card_${cat.name}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(accent.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = categoryIconFor(cat.iconKey),
                                                contentDescription = cat.name,
                                                tint = accent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            minLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            repeat(3 - rowCats.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Flash Sale Section with Live Timer
        if (flashProducts.isNotEmpty()) {
            item {
                val hours = flashSaleSecondsLeft / 3600
                val mins = (flashSaleSecondsLeft % 3600) / 60
                val secs = flashSaleSecondsLeft % 60

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Surface(
                        color = ZmRed,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = ZmWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FLASH SALE",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ZmWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Ends in",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ZmWhite.copy(alpha = 0.9f)
                                )
                                TimerBox("%02d".format(hours))
                                Text(":", color = ZmWhite, fontWeight = FontWeight.Bold)
                                TimerBox("%02d".format(mins))
                                Text(":", color = ZmWhite, fontWeight = FontWeight.Bold)
                                TimerBox("%02d".format(secs))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(flashProducts, key = { "flash_${it.id}" }) { product ->
                            ProductCard(
                                product = product,
                                onProductClick = { onProductClick(product.id) },
                                onAddToCartClick = { onAddToCart(product) },
                                modifier = Modifier.width(172.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Featured Products Grid Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Featured Products",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onViewAllShopClick) {
                    Text("See All (${products.size})", color = ZmGreen, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2-column Featured Grid
        val productRows = featuredProducts.chunked(2)
        items(productRows) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { product ->
                    ProductCard(
                        product = product,
                        onProductClick = { onProductClick(product.id) },
                        onAddToCartClick = { onAddToCart(product) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 5. Become a Seller Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ZmBlack)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    ZmImage(
                        imageSpec = "drawable:img_seller_banner",
                        contentDescription = "Become a Seller on ZM Bazaar",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ZmBlack.copy(alpha = 0.88f),
                                        ZmGreenDark.copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Surface(color = ZmGreen, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "MULTI-VENDOR MARKETPLACE",
                                color = ZmWhite,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentSeller?.status == SellerStatus.APPROVED) {
                                "Manage Your Store: ${currentSeller.shopName}"
                            } else {
                                "Sell on ZM Bazaar — Grow Across Pakistan!"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = ZmWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Open your online shop in Barnala & all Pakistan. Only 10% commission on delivered orders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZmWhite.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (currentSeller?.status == SellerStatus.APPROVED) {
                                    onSellerDashboardClick()
                                } else {
                                    onBecomeSellerClick()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZmRed, contentColor = ZmWhite),
                            modifier = Modifier.testTag("home_become_seller_button")
                        ) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentSeller?.status == SellerStatus.APPROVED) "Open Seller Dashboard" else "Become a Seller",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 6. Footer on every page
        item {
            ZmFooter(adminPaymentAccounts = adminPaymentAccounts)
        }
    }
}

@Composable
private fun TimerBox(value: String) {
    Surface(
        color = ZmBlack,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = value,
            color = ZmWhite,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
