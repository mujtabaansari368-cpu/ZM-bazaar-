package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.components.ZmImage
import com.example.ui.model.ZmBanner
import com.example.ui.model.ZmCategory
import com.example.ui.model.ZmPaymentAccount
import com.example.ui.theme.ZmGreen
import com.example.ui.theme.ZmWhite
import com.example.util.ImageStorageHelper

@Composable
fun AdminCategoryDialog(
    existing: ZmCategory?,
    onDismiss: () -> Unit,
    onSave: (ZmCategory) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: "clothes") }
    val iconKeys = listOf("clothes", "shoes", "cosmetics", "mobile", "electronics", "home", "kids", "grocery", "custom")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Icon Style:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(iconKeys) { key ->
                        FilterChip(
                            selected = iconKey == key,
                            onClick = { iconKey = key },
                            label = { Text(key) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            ZmCategory(
                                id = existing?.id ?: "",
                                name = name.trim(),
                                iconKey = iconKey,
                                accentHex = existing?.accentHex ?: 0xFF00A651,
                                sortOrder = existing?.sortOrder ?: 10
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
            ) { Text("Save", color = ZmWhite) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminBannerDialog(
    existing: ZmBanner?,
    onDismiss: () -> Unit,
    onSave: (ZmBanner) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var subtitle by remember { mutableStateOf(existing?.subtitle ?: "") }
    var badgeText by remember { mutableStateOf(existing?.badgeText ?: "PROMO") }
    var targetCategory by remember { mutableStateOf(existing?.targetCategory ?: "") }
    var imageUri by remember { mutableStateOf(existing?.imageUri ?: "drawable:img_banner_flash_sale") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            imageUri = ImageStorageHelper.persistPickedImage(context, uri, "banners") ?: uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Slider Banner" else "Edit Banner") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ZmImage(
                    imageSpec = imageUri,
                    contentDescription = "Banner preview",
                    modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(8.dp))
                )
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Banner Image")
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Title *") }, singleLine = true)
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") })
                OutlinedTextField(value = badgeText, onValueChange = { badgeText = it }, label = { Text("Badge Text") }, singleLine = true)
                OutlinedTextField(value = targetCategory, onValueChange = { targetCategory = it }, label = { Text("Target Category (optional)") }, singleLine = true)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Show on Home Slider")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            ZmBanner(
                                id = existing?.id ?: "",
                                title = title.trim(),
                                subtitle = subtitle.trim(),
                                badgeText = badgeText.trim().ifBlank { "OFFER" },
                                imageUri = imageUri,
                                targetCategory = targetCategory.trim(),
                                isActive = isActive,
                                sortOrder = existing?.sortOrder ?: 10
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
            ) { Text("Save Banner", color = ZmWhite) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminPaymentAccountDialog(
    existing: ZmPaymentAccount?,
    onDismiss: () -> Unit,
    onSave: (ZmPaymentAccount) -> Unit
) {
    var providerName by remember { mutableStateOf(existing?.providerName ?: "Easypaisa") }
    var accountNumber by remember { mutableStateOf(existing?.accountNumber ?: "") }
    var accountTitle by remember { mutableStateOf(existing?.accountTitle ?: "ZM Bazaar") }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Payment Account" else "Change Payment Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Easypaisa", "JazzCash", "Meezan Bank", "Bank Account")) { p ->
                        FilterChip(
                            selected = providerName == p,
                            onClick = { providerName = p },
                            label = { Text(p) }
                        )
                    }
                }
                OutlinedTextField(
                    value = providerName,
                    onValueChange = { providerName = it },
                    label = { Text("Provider Name *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number *") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = accountTitle,
                    onValueChange = { accountTitle = it },
                    label = { Text("Account Title *") },
                    singleLine = true
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Active on Checkout & Footer")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (providerName.isNotBlank() && accountNumber.isNotBlank()) {
                        onSave(
                            ZmPaymentAccount(
                                id = existing?.id ?: "",
                                ownerType = "ADMIN",
                                ownerId = "admin",
                                providerName = providerName.trim(),
                                accountNumber = accountNumber.trim(),
                                accountTitle = accountTitle.trim().ifBlank { "ZM Bazaar" },
                                isActive = isActive
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZmGreen)
            ) { Text("Save Account", color = ZmWhite) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
