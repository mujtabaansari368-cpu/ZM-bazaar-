package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object UserRole {
    const val ADMIN = "ADMIN"
    const val SELLER = "SELLER"
    const val CUSTOMER = "CUSTOMER"
}

object SellerStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object ProductApproval {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

object OrderStatus {
    const val PENDING = "Pending"
    const val CONFIRMED = "Confirmed"
    const val SHIPPED = "Shipped"
    const val DELIVERED = "Delivered"
    const val CANCELLED = "Cancelled"

    val trackingSteps = listOf(PENDING, CONFIRMED, SHIPPED, DELIVERED)
    val allStatuses = listOf(PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
}

object PayoutStatus {
    const val NONE = "NONE"
    const val PENDING_PAYOUT = "PENDING_PAYOUT"
    const val PAID = "PAID"
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val city: String,
    val role: String = UserRole.CUSTOMER,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sellers")
data class SellerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val cnic: String,
    val paymentMethod: String,
    val paymentAccountNumber: String,
    val paymentAccountTitle: String,
    val shopAddress: String,
    val status: String = SellerStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val accentHex: Long,
    val sortOrder: Int = 0
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sellerId: Long,
    val shopName: String,
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val oldPrice: Double = 0.0,
    val stock: Int,
    val rating: Double = 4.7,
    val reviewCount: Int = 24,
    val imageUrls: String,
    val isFlashSale: Boolean = false,
    val isFeatured: Boolean = true,
    val allowsCustomName: Boolean = false,
    val approvalStatus: String = ProductApproval.APPROVED,
    val createdAt: Long = System.currentTimeMillis()
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

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val imageUri: String,
    val targetCategory: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val sellerId: Long,
    val productName: String,
    val shopName: String,
    val unitPrice: Double,
    val oldPrice: Double = 0.0,
    val imageUri: String,
    val quantity: Int,
    val customName: String = "",
    val customPhotoUri: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderCode: String,
    val customerId: Long,
    val customerName: String,
    val phone: String,
    val address: String,
    val city: String,
    val note: String = "",
    val paymentMethod: String,
    val paymentAccountInfo: String = "",
    val paymentScreenshotUri: String = "",
    val subtotal: Double,
    val deliveryCharges: Double,
    val grandTotal: Double,
    val commissionAmount: Double = 0.0,
    val sellerEarningAmount: Double = 0.0,
    val payoutStatus: String = PayoutStatus.NONE,
    val status: String = OrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val sellerId: Long,
    val shopName: String,
    val productName: String,
    val imageUri: String,
    val unitPrice: Double,
    val quantity: Int,
    val lineTotal: Double,
    val commissionAmount: Double = 0.0,
    val sellerEarningAmount: Double = 0.0,
    val payoutStatus: String = PayoutStatus.NONE,
    val customName: String = "",
    val customPhotoUri: String = ""
)

@Entity(tableName = "payment_accounts")
data class PaymentAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: String, // "ADMIN" or "SELLER"
    val ownerId: Long,     // 0L for ADMIN, sellerId for SELLER
    val providerName: String,
    val accountNumber: String,
    val accountTitle: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long, // 0L = Admin/System, >0 = specific user
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
