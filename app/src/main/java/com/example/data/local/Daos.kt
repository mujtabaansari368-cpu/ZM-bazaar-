package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'CUSTOMER' ORDER BY createdAt DESC")
    fun observeCustomers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :passwordHash WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePasswordByEmail(email: String, passwordHash: String): Int

    @Query("UPDATE users SET role = :role WHERE id = :userId")
    suspend fun updateUserRole(userId: Long, role: String)
}

@Dao
interface SellerDao {
    @Query("SELECT * FROM sellers ORDER BY createdAt DESC")
    fun observeAllSellers(): Flow<List<SellerEntity>>

    @Query("SELECT * FROM sellers WHERE userId = :userId LIMIT 1")
    fun observeSellerByUserId(userId: Long): Flow<SellerEntity?>

    @Query("SELECT * FROM sellers WHERE userId = :userId LIMIT 1")
    suspend fun getSellerByUserId(userId: Long): SellerEntity?

    @Query("SELECT * FROM sellers WHERE id = :sellerId LIMIT 1")
    suspend fun getSellerById(sellerId: Long): SellerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeller(seller: SellerEntity): Long

    @Update
    suspend fun updateSeller(seller: SellerEntity)

    @Query("UPDATE sellers SET status = :status WHERE id = :sellerId")
    suspend fun updateSellerStatus(sellerId: Long, status: String)

    @Query("UPDATE sellers SET paymentMethod = :method, paymentAccountNumber = :number, paymentAccountTitle = :title WHERE id = :sellerId")
    suspend fun updateSellerPaymentAccount(sellerId: Long, method: String, number: String, title: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE approvalStatus = 'APPROVED' ORDER BY createdAt DESC")
    fun observeApprovedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE sellerId = :sellerId ORDER BY createdAt DESC")
    fun observeSellerProducts(sellerId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    fun observeProductById(productId: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET approvalStatus = :status WHERE id = :productId")
    suspend fun updateApprovalStatus(productId: Long, status: String)

    @Query("UPDATE products SET stock = MAX(0, stock - :quantity) WHERE id = :productId")
    suspend fun decrementStock(productId: Long, quantity: Int)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}

@Dao
interface BannerDao {
    @Query("SELECT * FROM banners WHERE isActive = 1 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveBanners(): Flow<List<BannerEntity>>

    @Query("SELECT * FROM banners ORDER BY sortOrder ASC, id ASC")
    fun observeAllBanners(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: BannerEntity): Long

    @Update
    suspend fun updateBanner(banner: BannerEntity)

    @Delete
    suspend fun deleteBanner(banner: BannerEntity)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY addedAt DESC")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items")
    suspend fun getCartItemsSnapshot(): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE productId = :productId AND customName = :customName AND customPhotoUri = :customPhotoUri LIMIT 1")
    suspend fun findMatchingCartItem(productId: Long, customName: String, customPhotoUri: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity): Long

    @Update
    suspend fun updateCartItem(item: CartItemEntity)

    @Delete
    suspend fun deleteCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun observeCustomerOrders(customerId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    fun observeOrderById(orderId: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM order_items")
    fun observeAllOrderItems(): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun observeOrderItems(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsByOrderId(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE sellerId = :sellerId ORDER BY id DESC")
    fun observeSellerOrderItems(sellerId: Long): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Update
    suspend fun updateOrderItem(item: OrderItemEntity)

    @Query("UPDATE order_items SET payoutStatus = 'PAID' WHERE sellerId = :sellerId AND payoutStatus = 'PENDING_PAYOUT'")
    suspend fun markSellerItemsPaid(sellerId: Long)
}

@Dao
interface PaymentAccountDao {
    @Query("SELECT * FROM payment_accounts WHERE ownerType = 'ADMIN' ORDER BY id ASC")
    fun observeAdminAccounts(): Flow<List<PaymentAccountEntity>>

    @Query("SELECT * FROM payment_accounts WHERE ownerType = 'SELLER' AND ownerId = :sellerId ORDER BY id ASC")
    fun observeSellerAccounts(sellerId: Long): Flow<List<PaymentAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: PaymentAccountEntity): Long

    @Update
    suspend fun updateAccount(account: PaymentAccountEntity)

    @Delete
    suspend fun deleteAccount(account: PaymentAccountEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 0 ORDER BY createdAt DESC")
    fun observeNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId OR userId = 0")
    suspend fun markAllRead(userId: Long)
}
