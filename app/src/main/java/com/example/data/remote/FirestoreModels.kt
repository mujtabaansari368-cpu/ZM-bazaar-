package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FsUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val role: String = "CUSTOMER",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsSellerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val cnic: String = "",
    val paymentMethod: String = "Easypaisa",
    val paymentAccountNumber: String = "",
    val paymentAccountTitle: String = "",
    val shopAddress: String = "",
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val iconKey: String = "clothes",
    val accentHex: Long = 0xFF00A651,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsBanner(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val badgeText: String = "",
    val imageUri: String = "drawable:img_banner_flash_sale",
    val targetCategory: String = "",
    val active: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsPaymentAccount(
    @DocumentId val id: String = "",
    val providerName: String = "",
    val accountNumber: String = "",
    val accountTitle: String = "ZM Bazaar",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsProduct(
    @DocumentId val id: String = "",
    val sellerUid: String = "",
    val shopName: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val oldPrice: Double = 0.0,
    val stock: Long = 0L,
    val rating: Double = 4.7,
    val reviewCount: Long = 20L,
    val imageUrls: String = "preset:default",
    val flashSale: Boolean = false,
    val featured: Boolean = true,
    val allowsCustomName: Boolean = false,
    val approvalStatus: String = "APPROVED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val discountPercent: Int
        get() = if (oldPrice > price && oldPrice > 0.0) {
            (((oldPrice - price) / oldPrice) * 100.0).toInt().coerceIn(1, 95)
        } else 0

    val primaryImage: String
        get() = imageUrls.split("|").firstOrNull { it.isNotBlank() } ?: "preset:default"

    val imageList: List<String>
        get() = imageUrls.split("|").filter { it.isNotBlank() }.ifEmpty { listOf("preset:default") }
}

@Keep
@IgnoreExtraProperties
data class FsCartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val sellerUid: String = "",
    val productName: String = "",
    val shopName: String = "",
    val unitPrice: Double = 0.0,
    val oldPrice: Double = 0.0,
    val imageUri: String = "preset:default",
    val quantity: Long = 1L,
    val customName: String = "",
    val customPhotoUri: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FsOrderItem(
    val productId: String = "",
    val sellerUid: String = "",
    val shopName: String = "",
    val productName: String = "",
    val imageUri: String = "preset:default",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val lineTotal: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val sellerEarningAmount: Double = 0.0,
    val payoutStatus: String = "NONE",
    val customName: String = "",
    val customPhotoUri: String = ""
)

@Keep
@IgnoreExtraProperties
data class FsOrder(
    @DocumentId val id: String = "",
    val orderCode: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val note: String = "",
    val paymentMethod: String = "",
    val paymentAccountInfo: String = "",
    val paymentScreenshotUri: String = "",
    val subtotal: Double = 0.0,
    val deliveryCharges: Double = 0.0,
    val grandTotal: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val sellerEarningAmount: Double = 0.0,
    val payoutStatus: String = "NONE",
    val status: String = "Pending",
    val sellerUids: List<String> = emptyList(),
    val items: List<Map<String, Any?>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedItems(): List<FsOrderItem> = items.map { map ->
        FsOrderItem(
            productId = map["productId"] as? String ?: "",
            sellerUid = map["sellerUid"] as? String ?: "",
            shopName = map["shopName"] as? String ?: "",
            productName = map["productName"] as? String ?: "",
            imageUri = map["imageUri"] as? String ?: "preset:default",
            unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
            quantity = (map["quantity"] as? Number)?.toLong() ?: 1L,
            lineTotal = (map["lineTotal"] as? Number)?.toDouble() ?: 0.0,
            commissionAmount = (map["commissionAmount"] as? Number)?.toDouble() ?: 0.0,
            sellerEarningAmount = (map["sellerEarningAmount"] as? Number)?.toDouble() ?: 0.0,
            payoutStatus = map["payoutStatus"] as? String ?: "NONE",
            customName = map["customName"] as? String ?: "",
            customPhotoUri = map["customPhotoUri"] as? String ?: ""
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FsNotification(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "ORDER",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
