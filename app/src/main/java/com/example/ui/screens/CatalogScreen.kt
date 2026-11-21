package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ProductCard
import com.example.ui.components.ZmFooter
import com.example.ui.model.ZmCategory
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.model.ZmProduct
import com.example.ui.theme.*
import com.example.ui.viewmodel.PriceFilter
import com.example.ui.viewmodel.SortOption

@Composable
fun CatalogScreen(
    products: List<ZmProduct>,
    categories: List<ZmCategory>,
    selectedCategory: String,
    selectedPriceFilter: PriceFilter,
    selectedSort: SortOption,
    searchQuery: String,
    adminPaymentAccounts: List<ZmPaymentAccount>,
    onSelectCategory: (String) -> Unit,
    onSelectPriceFilter: (PriceFilter) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onProductClick: (String) -> Unit,
    onAddToCart: (ZmProduct) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("catalog_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Category Chips
        item {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = if (selectedCategory == "All") "All Products (${products.size})" else "$selectedCategory (${products.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "Search results for \"$searchQuery\" (product or shop name)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ZmGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "All",
                            onClick = { onSelectCategory("All") },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZmGreen,
                                selectedLabelColor = ZmWhite
                            )
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat.name, ignoreCase = true),
                            onClick = { onSelectCategory(cat.name) },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZmGreen,
                                selectedLabelColor = ZmWhite
                            )
                        )
                    }
                }
            }
        }

        // Price & Sort Controls
        item {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(Icons.Filled.FilterList, contentDescription = null, tint = ZmGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Price:", style = MaterialTheme.typography.labelMedium)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PriceFilter.entries) { pf ->
                        FilterChip(
                            selected = selectedPriceFilter == pf,
                            onClick = { onSelectPriceFilter(pf) },
                            label = { Text(pf.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZmBlack,
                                selectedLabelColor = ZmWhite
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(Icons.Filled.Sort, contentDescription = null, tint = ZmRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort:", style = MaterialTheme.typography.labelMedium)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SortOption.entries) { sort ->
                        FilterChip(
                            selected = selectedSort == sort,
                            onClick = { onSelectSort(sort) },
                            label = { Text(sort.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZmRed,
                                selectedLabelColor = ZmWhite
                            )
                        )
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = ZmMutedText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No matching products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing filters or searching another product or shop name.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZmMutedText
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onClearFilters,
                            colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
                        ) {
                            Text("Reset Filters", color = ZmWhite)
                        }
                    }
                }
            }
        } else {
            val rows = products.chunked(2)
            items(rows) { rowItems ->
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
        }

        item {
            ZmFooter(adminPaymentAccounts = adminPaymentAccounts)
        }
    }
}
