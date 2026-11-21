package com.example.ui.model

import com.example.data.local.*
import com.example.data.remote.*

data class ZmUser(
    val id: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val city: String,
    val role: String = UserRole.CUSTOMER,
    val isEmailVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ZmSeller(
    val id: String,
    val userId: String,
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

data class ZmCategory(
    val id: String,
    val name: String,
    val iconKey: String,
    val accentHex: Long,
    val sortOrder: Int = 0
)

data class ZmProduct(
    val id: String,
    val sellerId: String,
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

data class ZmBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val imageUri: String,
    val targetCategory: String = "",
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

data class ZmCartItem(
    val id: String,
    val productId: String,
    val sellerId: String,
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

data class ZmOrder(
    val id: String,
    val orderCode: String,
    val customerId: String,
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
    val updatedAt: Long = System.currentTimeMillis(),
    val items: List<ZmOrderItem> = emptyList()
)

data class ZmOrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val sellerId: String,
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

data class ZmPaymentAccount(
    val id: String,
    val ownerType: String,
    val ownerId: String,
    val providerName: String,
    val accountNumber: String,
    val accountTitle: String,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ZmNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Room -> UI Mappers (for offline/guest preview mode) ---

fun UserEntity.toZmUser() = ZmUser(
    id = id.toString(),
    fullName = fullName,
    phone = phone,
    email = email,
    city = city,
    role = role,
    isEmailVerified = true,
    createdAt = createdAt
)

fun SellerEntity.toZmSeller() = ZmSeller(
    id = id.toString(),
    userId = userId.toString(),
    shopName = shopName,
    ownerName = ownerName,
    phone = phone,
    cnic = cnic,
    paymentMethod = paymentMethod,
    paymentAccountNumber = paymentAccountNumber,
    paymentAccountTitle = paymentAccountTitle,
    shopAddress = shopAddress,
    status = status,
    createdAt = createdAt
)

fun CategoryEntity.toZmCategory() = ZmCategory(
    id = id.toString(),
    name = name,
    iconKey = iconKey,
    accentHex = accentHex,
    sortOrder = sortOrder
)

fun ProductEntity.toZmProduct() = ZmProduct(
    id = id.toString(),
    sellerId = sellerId.toString(),
    shopName = shopName,
    name = name,
    description = description,
    category = category,
    price = price,
    oldPrice = oldPrice,
    stock = stock,
    rating = rating,
    reviewCount = reviewCount,
    imageUrls = imageUrls,
    isFlashSale = isFlashSale,
    isFeatured = isFeatured,
    allowsCustomName = allowsCustomName,
    approvalStatus = approvalStatus,
    createdAt = createdAt
)

fun BannerEntity.toZmBanner() = ZmBanner(
    id = id.toString(),
    title = title,
    subtitle = subtitle,
    badgeText = badgeText,
    imageUri = imageUri,
    targetCategory = targetCategory,
    isActive = isActive,
    sortOrder = sortOrder
)

fun CartItemEntity.toZmCartItem() = ZmCartItem(
    id = id.toString(),
    productId = productId.toString(),
    sellerId = sellerId.toString(),
    productName = productName,
    shopName = shopName,
    unitPrice = unitPrice,
    oldPrice = oldPrice,
    imageUri = imageUri,
    quantity = quantity,
    customName = customName,
    customPhotoUri = customPhotoUri,
    addedAt = addedAt
)

fun OrderEntity.toZmOrder() = ZmOrder(
    id = id.toString(),
    orderCode = orderCode,
    customerId = customerId.toString(),
    customerName = customerName,
    phone = phone,
    address = address,
    city = city,
    note = note,
    paymentMethod = paymentMethod,
    paymentAccountInfo = paymentAccountInfo,
    paymentScreenshotUri = paymentScreenshotUri,
    subtotal = subtotal,
    deliveryCharges = deliveryCharges,
    grandTotal = grandTotal,
    commissionAmount = commissionAmount,
    sellerEarningAmount = sellerEarningAmount,
    payoutStatus = payoutStatus,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun OrderItemEntity.toZmOrderItem() = ZmOrderItem(
    id = id.toString(),
    orderId = orderId.toString(),
    productId = productId.toString(),
    sellerId = sellerId.toString(),
    shopName = shopName,
    productName = productName,
    imageUri = imageUri,
    unitPrice = unitPrice,
    quantity = quantity,
    lineTotal = lineTotal,
    commissionAmount = commissionAmount,
    sellerEarningAmount = sellerEarningAmount,
    payoutStatus = payoutStatus,
    customName = customName,
    customPhotoUri = customPhotoUri
)

fun PaymentAccountEntity.toZmPaymentAccount() = ZmPaymentAccount(
    id = id.toString(),
    ownerType = ownerType,
    ownerId = ownerId.toString(),
    providerName = providerName,
    accountNumber = accountNumber,
    accountTitle = accountTitle,
    isActive = isActive,
    updatedAt = updatedAt
)

fun NotificationEntity.toZmNotification() = ZmNotification(
    id = id.toString(),
    userId = userId.toString(),
    title = title,
    message = message,
    type = type,
    isRead = isRead,
    createdAt = createdAt
)

// --- Firestore -> UI Mappers ---

fun FsUserProfile.toZmUser(isVerified: Boolean = true) = ZmUser(
    id = id.ifBlank { userId },
    fullName = fullName,
    phone = phone,
    email = email,
    city = city,
    role = role,
    isEmailVerified = isVerified,
    createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun FsSellerProfile.toZmSeller() = ZmSeller(
    id = id.ifBlank { userId },
    userId = userId.ifBlank { id },
    shopName = shopName,
    ownerName = ownerName,
    phone = phone,
    cnic = cnic,
    paymentMethod = paymentMethod,
    paymentAccountNumber = paymentAccountNumber,
    paymentAccountTitle = paymentAccountTitle,
    shopAddress = shopAddress,
    status = status,
    createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun FsCategory.toZmCategory() = ZmCategory(
    id = id,
    name = name,
    iconKey = iconKey,
    accentHex = accentHex,
    sortOrder = sortOrder.toInt()
)

fun FsProduct.toZmProduct() = ZmProduct(
    id = id,
    sellerId = sellerUid,
    shopName = shopName,
    name = name,
    description = description,
    category = category,
    price = price,
    oldPrice = oldPrice,
    stock = stock.toInt(),
    rating = rating,
    reviewCount = reviewCount.toInt(),
    imageUrls = imageUrls,
    isFlashSale = flashSale,
    isFeatured = featured,
    allowsCustomName = allowsCustomName,
    approvalStatus = approvalStatus,
    createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun FsBanner.toZmBanner() = ZmBanner(
    id = id,
    title = title,
    subtitle = subtitle,
    badgeText = badgeText,
    imageUri = imageUri,
    targetCategory = targetCategory,
    isActive = active,
    sortOrder = sortOrder.toInt()
)

fun FsCartItem.toZmCartItem() = ZmCartItem(
    id = id,
    productId = productId,
    sellerId = sellerUid,
    productName = productName,
    shopName = shopName,
    unitPrice = unitPrice,
    oldPrice = oldPrice,
    imageUri = imageUri,
    quantity = quantity.toInt(),
    customName = customName,
    customPhotoUri = customPhotoUri,
    addedAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun FsOrder.toZmOrder(): ZmOrder {
    val mappedItems = parsedItems().mapIndexed { idx, item ->
        ZmOrderItem(
            id = "${id}_$idx",
            orderId = id,
            productId = item.productId,
            sellerId = item.sellerUid,
            shopName = item.shopName,
            productName = item.productName,
            imageUri = item.imageUri,
            unitPrice = item.unitPrice,
            quantity = item.quantity.toInt(),
            lineTotal = item.lineTotal,
            commissionAmount = item.commissionAmount,
            sellerEarningAmount = item.sellerEarningAmount,
            payoutStatus = item.payoutStatus,
            customName = item.customName,
            customPhotoUri = item.customPhotoUri
        )
    }
    return ZmOrder(
        id = id,
        orderCode = orderCode,
        customerId = customerId,
        customerName = customerName,
        phone = phone,
        address = address,
        city = city,
        note = note,
        paymentMethod = paymentMethod,
        paymentAccountInfo = paymentAccountInfo,
        paymentScreenshotUri = paymentScreenshotUri,
        subtotal = subtotal,
        deliveryCharges = deliveryCharges,
        grandTotal = grandTotal,
        commissionAmount = commissionAmount,
        sellerEarningAmount = sellerEarningAmount,
        payoutStatus = payoutStatus,
        status = status,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
        items = mappedItems
    )
}

fun FsPaymentAccount.toZmPaymentAccount() = ZmPaymentAccount(
    id = id,
    ownerType = "ADMIN",
    ownerId = "admin",
    providerName = providerName,
    accountNumber = accountNumber,
    accountTitle = accountTitle,
    isActive = active,
    updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun FsNotification.toZmNotification(uid: String) = ZmNotification(
    id = id,
    userId = uid,
    title = title,
    message = message,
    type = type,
    isRead = read,
    createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)
