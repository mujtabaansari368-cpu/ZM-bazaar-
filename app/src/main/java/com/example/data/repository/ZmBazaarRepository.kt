package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.util.IntentHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

class ZmBazaarRepository(
    private val context: Context,
    private val db: ZmBazaarDatabase,
    private val sessionManager: SessionManager
) {
    private val userDao = db.userDao()
    private val sellerDao = db.sellerDao()
    private val categoryDao = db.categoryDao()
    private val productDao = db.productDao()
    private val bannerDao = db.bannerDao()
    private val cartDao = db.cartDao()
    private val orderDao = db.orderDao()
    private val paymentAccountDao = db.paymentAccountDao()
    private val notificationDao = db.notificationDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> = sessionManager.loggedInUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else userDao.observeUserById(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSeller: Flow<SellerEntity?> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(null) else sellerDao.observeSellerByUserId(user.id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUserNotifications: Flow<List<NotificationEntity>> = currentUser.flatMapLatest { user ->
        notificationDao.observeNotificationsForUser(user?.id ?: 0L)
    }

    val categories: Flow<List<CategoryEntity>> = categoryDao.observeCategories()
    val approvedProducts: Flow<List<ProductEntity>> = productDao.observeApprovedProducts()
    val allProducts: Flow<List<ProductEntity>> = productDao.observeAllProducts()
    val activeBanners: Flow<List<BannerEntity>> = bannerDao.observeActiveBanners()
    val allBanners: Flow<List<BannerEntity>> = bannerDao.observeAllBanners()
    val cartItems: Flow<List<CartItemEntity>> = cartDao.observeCartItems()
    val allOrders: Flow<List<OrderEntity>> = orderDao.observeAllOrders()
    val allOrderItems: Flow<List<OrderItemEntity>> = orderDao.observeAllOrderItems()
    val allSellers: Flow<List<SellerEntity>> = sellerDao.observeAllSellers()
    val allUsers: Flow<List<UserEntity>> = userDao.observeAllUsers()
    val adminPaymentAccounts: Flow<List<PaymentAccountEntity>> = paymentAccountDao.observeAdminAccounts()

    fun observeProductById(productId: Long): Flow<ProductEntity?> = productDao.observeProductById(productId)
    fun observeCustomerOrders(customerId: Long): Flow<List<OrderEntity>> = orderDao.observeCustomerOrders(customerId)
    fun observeSellerProducts(sellerId: Long): Flow<List<ProductEntity>> = productDao.observeSellerProducts(sellerId)
    fun observeSellerOrderItems(sellerId: Long): Flow<List<OrderItemEntity>> = orderDao.observeSellerOrderItems(sellerId)
    fun observeSellerPaymentAccounts(sellerId: Long): Flow<List<PaymentAccountEntity>> = paymentAccountDao.observeSellerAccounts(sellerId)

    // --- Authentication ---

    suspend fun login(email: String, password: String): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email and password are required."))
        }
        val user = userDao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found with this email."))
        val hash = ZmBazaarDatabase.sha256(password)
        if (user.passwordHash != hash) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        sessionManager.saveUserId(user.id)
        return Result.success(user)
    }

    suspend fun signup(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        city: String
    ): Result<UserEntity> {
        val cleanEmail = email.trim().lowercase()
        if (fullName.isBlank() || phone.isBlank() || cleanEmail.isBlank() || password.length < 6 || city.isBlank()) {
            return Result.failure(IllegalArgumentException("Fill all fields. Password must be at least 6 characters."))
        }
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }
        val role = if (cleanEmail == ZmBazaarDatabase.ADMIN_EMAIL.lowercase()) UserRole.ADMIN else UserRole.CUSTOMER
        val newUser = UserEntity(
            fullName = fullName.trim(),
            phone = phone.trim(),
            email = cleanEmail,
            passwordHash = ZmBazaarDatabase.sha256(password),
            city = city.trim(),
            role = role
        )
        val userId = userDao.insertUser(newUser)
        sessionManager.saveUserId(userId)
        val saved = newUser.copy(id = userId)
        notificationDao.insertNotification(
            NotificationEntity(
                userId = userId,
                title = "Welcome to ZM Bazaar, ${saved.fullName}!",
                message = "Your account is active. Enjoy shopping across Pakistan & Azad Kashmir.",
                type = "ORDER"
            )
        )
        return Result.success(saved)
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Enter valid email and password (min 6 chars)."))
        }
        val updated = userDao.updatePasswordByEmail(cleanEmail, ZmBazaarDatabase.sha256(newPassword))
        return if (updated > 0) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("No account found with email $cleanEmail."))
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    // --- Cart ---

    suspend fun addToCart(
        product: ProductEntity,
        quantity: Int,
        customName: String = "",
        customPhotoUri: String = ""
    ): Result<Unit> {
        if (product.stock <= 0) {
            return Result.failure(IllegalStateException("This product is currently out of stock."))
        }
        val existing = cartDao.findMatchingCartItem(product.id, customName.trim(), customPhotoUri.trim())
        if (existing != null) {
            val newQty = (existing.quantity + quantity).coerceAtMost(product.stock)
            cartDao.updateCartItem(existing.copy(quantity = newQty))
        } else {
            cartDao.insertCartItem(
                CartItemEntity(
                    productId = product.id,
                    sellerId = product.sellerId,
                    productName = product.name,
                    shopName = product.shopName,
                    unitPrice = product.price,
                    oldPrice = product.oldPrice,
                    imageUri = product.primaryImage,
                    quantity = quantity.coerceIn(1, product.stock),
                    customName = customName.trim(),
                    customPhotoUri = customPhotoUri.trim()
                )
            )
        }
        return Result.success(Unit)
    }

    suspend fun updateCartQuantity(item: CartItemEntity, newQty: Int) {
        if (newQty <= 0) {
            cartDao.deleteCartItem(item)
        } else {
            val product = productDao.getProductById(item.productId)
            val maxStock = product?.stock ?: 99
            cartDao.updateCartItem(item.copy(quantity = newQty.coerceAtMost(maxStock.coerceAtLeast(1))))
        }
    }

    suspend fun removeCartItem(item: CartItemEntity) {
        cartDao.deleteCartItem(item)
    }

    // --- Orders & 10% Commission ---

    suspend fun placeOrder(
        customer: UserEntity,
        fullName: String,
        phone: String,
        address: String,
        city: String,
        note: String,
        paymentMethod: String,
        paymentAccountInfo: String,
        paymentScreenshotUri: String
    ): Result<OrderEntity> {
        val cart = cartDao.getCartItemsSnapshot()
        if (cart.isEmpty()) {
            return Result.failure(IllegalStateException("Your cart is empty."))
        }
        if (fullName.isBlank() || phone.isBlank() || address.isBlank() || city.isBlank()) {
            return Result.failure(IllegalArgumentException("Please fill Full Name, Phone, Address, and City."))
        }
        val subtotal = cart.sumOf { it.unitPrice * it.quantity }
        val deliveryCharges = if (subtotal >= 3000.0) 0.0 else 150.0
        val grandTotal = subtotal + deliveryCharges
        val orderCode = "ZMB-2026-${(1000..9999).random()}"

        val order = OrderEntity(
            orderCode = orderCode,
            customerId = customer.id,
            customerName = fullName.trim(),
            phone = phone.trim(),
            address = address.trim(),
            city = city.trim(),
            note = note.trim(),
            paymentMethod = paymentMethod,
            paymentAccountInfo = paymentAccountInfo,
            paymentScreenshotUri = paymentScreenshotUri,
            subtotal = subtotal,
            deliveryCharges = deliveryCharges,
            grandTotal = grandTotal,
            status = OrderStatus.PENDING
        )

        val orderId = orderDao.insertOrder(order)
        val orderItems = cart.map { c ->
            OrderItemEntity(
                orderId = orderId,
                productId = c.productId,
                sellerId = c.sellerId,
                shopName = c.shopName,
                productName = c.productName,
                imageUri = c.imageUri,
                unitPrice = c.unitPrice,
                quantity = c.quantity,
                lineTotal = c.unitPrice * c.quantity,
                customName = c.customName,
                customPhotoUri = c.customPhotoUri
            )
        }
        orderDao.insertOrderItems(orderItems)
        cart.forEach { productDao.decrementStock(it.productId, it.quantity) }
        cartDao.clearCart()

        val title = "Order Placed: $orderCode"
        val message = "Total ${IntentHelper.formatRs(grandTotal)} via $paymentMethod. Status: Pending."
        notificationDao.insertNotification(
            NotificationEntity(userId = customer.id, title = title, message = message, type = "ORDER")
        )
        notificationDao.insertNotification(
            NotificationEntity(userId = 0L, title = "New Order $orderCode", message = "${fullName.trim()} ($city) ordered ${IntentHelper.formatRs(grandTotal)}", type = "ORDER")
        )
        NotificationHelper.showNotification(context, title, message)

        return Result.success(order.copy(id = orderId))
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: String) {
        val order = orderDao.getOrderById(orderId) ?: return
        val items = orderDao.getOrderItemsByOrderId(orderId)

        var updatedOrder = order.copy(
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )

        if (newStatus == OrderStatus.DELIVERED && order.commissionAmount <= 0.0) {
            val totalCommission = ((order.subtotal * 0.10) * 100.0).roundToInt() / 100.0
            val totalSellerEarning = ((order.subtotal - totalCommission) * 100.0).roundToInt() / 100.0
            updatedOrder = updatedOrder.copy(
                commissionAmount = totalCommission,
                sellerEarningAmount = totalSellerEarning,
                payoutStatus = PayoutStatus.PENDING_PAYOUT
            )
            items.forEach { item ->
                val itemComm = ((item.lineTotal * 0.10) * 100.0).roundToInt() / 100.0
                val itemEarn = ((item.lineTotal - itemComm) * 100.0).roundToInt() / 100.0
                orderDao.updateOrderItem(
                    item.copy(
                        commissionAmount = itemComm,
                        sellerEarningAmount = itemEarn,
                        payoutStatus = PayoutStatus.PENDING_PAYOUT
                    )
                )
            }
        }

        orderDao.updateOrder(updatedOrder)
        val title = "Order ${order.orderCode}: $newStatus"
        val msg = if (newStatus == OrderStatus.DELIVERED) {
            "Your order has been delivered! 10% ZM Bazaar commission (${IntentHelper.formatRs(updatedOrder.commissionAmount)}) calculated."
        } else {
            "Your ZM Bazaar order status updated to $newStatus."
        }
        notificationDao.insertNotification(
            NotificationEntity(userId = order.customerId, title = title, message = msg, type = "STATUS")
        )
        NotificationHelper.showNotification(context, title, msg)
    }

    suspend fun sendPaymentToSeller(seller: SellerEntity, amount: Double) {
        orderDao.markSellerItemsPaid(seller.id)
        val orders = orderDao.observeAllOrders().first()
        for (ord in orders) {
            if (ord.status == OrderStatus.DELIVERED && ord.payoutStatus == PayoutStatus.PENDING_PAYOUT) {
                val items = orderDao.getOrderItemsByOrderId(ord.id)
                if (items.all { it.payoutStatus == PayoutStatus.PAID || it.payoutStatus == PayoutStatus.NONE }) {
                    orderDao.updateOrder(ord.copy(payoutStatus = PayoutStatus.PAID, updatedAt = System.currentTimeMillis()))
                }
            }
        }
        val title = "Payout Sent to ${seller.shopName}"
        val msg = "${IntentHelper.formatRs(amount)} sent to ${seller.paymentMethod}: ${seller.paymentAccountNumber}."
        notificationDao.insertNotification(
            NotificationEntity(userId = seller.userId, title = title, message = msg, type = "PAYOUT")
        )
        NotificationHelper.showNotification(context, title, msg)
    }

    // --- Seller Operations ---

    suspend fun submitSellerApplication(
        user: UserEntity,
        shopName: String,
        ownerName: String,
        phone: String,
        cnic: String,
        paymentMethod: String,
        paymentNumber: String,
        shopAddress: String
    ): Result<Unit> {
        if (shopName.isBlank() || ownerName.isBlank() || phone.isBlank() || cnic.isBlank() || paymentNumber.isBlank() || shopAddress.isBlank()) {
            return Result.failure(IllegalArgumentException("All seller application fields are required."))
        }
        val existing = sellerDao.getSellerByUserId(user.id)
        val seller = SellerEntity(
            id = existing?.id ?: 0L,
            userId = user.id,
            shopName = shopName.trim(),
            ownerName = ownerName.trim(),
            phone = phone.trim(),
            cnic = cnic.trim(),
            paymentMethod = paymentMethod.trim(),
            paymentAccountNumber = paymentNumber.trim(),
            paymentAccountTitle = ownerName.trim(),
            shopAddress = shopAddress.trim(),
            status = SellerStatus.PENDING
        )
        val sellerId = sellerDao.insertSeller(seller)
        paymentAccountDao.insertAccount(
            PaymentAccountEntity(
                ownerType = "SELLER",
                ownerId = sellerId,
                providerName = paymentMethod.trim(),
                accountNumber = paymentNumber.trim(),
                accountTitle = ownerName.trim()
            )
        )
        notificationDao.insertNotification(
            NotificationEntity(
                userId = 0L,
                title = "New Seller Application",
                message = "${shopName.trim()} applied to sell on ZM Bazaar.",
                type = "SELLER"
            )
        )
        return Result.success(Unit)
    }

    suspend fun approveSeller(seller: SellerEntity) {
        sellerDao.updateSellerStatus(seller.id, SellerStatus.APPROVED)
        val user = userDao.getUserById(seller.userId)
        if (user != null && user.role != UserRole.ADMIN) {
            userDao.updateUserRole(user.id, UserRole.SELLER)
        }
        val title = "Seller Approved!"
        val msg = "Congratulations! ${seller.shopName} is approved on ZM Bazaar. Open your Seller Dashboard."
        notificationDao.insertNotification(
            NotificationEntity(userId = seller.userId, title = title, message = msg, type = "SELLER")
        )
        NotificationHelper.showNotification(context, title, msg)
    }

    suspend fun rejectSeller(seller: SellerEntity) {
        sellerDao.updateSellerStatus(seller.id, SellerStatus.REJECTED)
        notificationDao.insertNotification(
            NotificationEntity(
                userId = seller.userId,
                title = "Seller Application Update",
                message = "Your application for ${seller.shopName} was not approved. Contact ZM Bazaar support on WhatsApp.",
                type = "SELLER"
            )
        )
    }

    suspend fun changeSellerPaymentAccount(
        sellerId: Long,
        method: String,
        accountNumber: String,
        accountTitle: String
    ) {
        sellerDao.updateSellerPaymentAccount(sellerId, method.trim(), accountNumber.trim(), accountTitle.trim())
        val existingAccounts = paymentAccountDao.observeSellerAccounts(sellerId).first()
        if (existingAccounts.isNotEmpty()) {
            paymentAccountDao.updateAccount(
                existingAccounts.first().copy(
                    providerName = method.trim(),
                    accountNumber = accountNumber.trim(),
                    accountTitle = accountTitle.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            paymentAccountDao.insertAccount(
                PaymentAccountEntity(
                    ownerType = "SELLER",
                    ownerId = sellerId,
                    providerName = method.trim(),
                    accountNumber = accountNumber.trim(),
                    accountTitle = accountTitle.trim()
                )
            )
        }
    }

    // --- Products ---

    suspend fun saveProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun updateProductApproval(productId: Long, status: String) {
        productDao.updateApprovalStatus(productId, status)
    }

    // --- Categories, Banners, Admin Payment Accounts ---

    suspend fun saveCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun saveBanner(banner: BannerEntity) {
        bannerDao.insertBanner(banner)
    }

    suspend fun deleteBanner(banner: BannerEntity) {
        bannerDao.deleteBanner(banner)
    }

    suspend fun saveAdminPaymentAccount(account: PaymentAccountEntity) {
        if (account.id == 0L) {
            paymentAccountDao.insertAccount(account.copy(ownerType = "ADMIN", ownerId = 0L))
        } else {
            paymentAccountDao.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteAdminPaymentAccount(account: PaymentAccountEntity) {
        paymentAccountDao.deleteAccount(account)
    }

    suspend fun markNotificationsRead(userId: Long) {
        notificationDao.markAllRead(userId)
    }
}
