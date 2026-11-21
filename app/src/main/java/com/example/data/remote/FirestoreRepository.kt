package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.OrderStatus
import com.example.data.local.PayoutStatus
import com.example.data.local.ProductApproval
import com.example.data.local.SellerStatus
import com.example.data.local.UserRole
import com.example.data.local.ZmBazaarDatabase
import com.example.ui.model.*
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class FirestoreRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val tsBehavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication required")

    private fun isCurrentUserAdmin(): Boolean {
        val u = auth.currentUser ?: return false
        return u.email.equals(ZmBazaarDatabase.ADMIN_EMAIL, ignoreCase = true) && u.isEmailVerified
    }

    // --- Observers (Strictly Gated Behind Sign-In) ---

    fun observeUserProfile(uid: String): Flow<ZmUser?> {
        if (auth.currentUser == null) return flowOf(null)
        val path = "users/$uid"
        val isVerified = auth.currentUser?.isEmailVerified == true
        return db.document(path)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap -> snap.toObject(FsUserProfile::class.java, tsBehavior)?.toZmUser(isVerified) }
            .catch { e ->
                handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observeSellerProfile(uid: String): Flow<ZmSeller?> {
        if (auth.currentUser == null) return flowOf(null)
        val path = "sellers/$uid"
        return db.document(path)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap -> snap.toObject(FsSellerProfile::class.java, tsBehavior)?.toZmSeller() }
            .catch { e ->
                handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observeCategories(): Flow<List<ZmCategory>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("categories")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsCategory::class.java, tsBehavior)?.toZmCategory() }
                    .sortedBy { it.sortOrder }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "categories")
                throw e
            }
    }

    fun observeActiveBanners(): Flow<List<ZmBanner>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("banners")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsBanner::class.java, tsBehavior)?.toZmBanner() }
                    .filter { it.isActive }
                    .sortedBy { it.sortOrder }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "banners")
                throw e
            }
    }

    fun observeAllBanners(): Flow<List<ZmBanner>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("banners")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsBanner::class.java, tsBehavior)?.toZmBanner() }
                    .sortedBy { it.sortOrder }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "banners")
                throw e
            }
    }

    fun observeAdminPaymentAccounts(): Flow<List<ZmPaymentAccount>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("payment_accounts")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsPaymentAccount::class.java, tsBehavior)?.toZmPaymentAccount() }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "payment_accounts")
                throw e
            }
    }

    fun observeApprovedProducts(): Flow<List<ZmProduct>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("products")
            .whereEqualTo("approvalStatus", ProductApproval.APPROVED)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsProduct::class.java, tsBehavior)?.toZmProduct() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "products")
                throw e
            }
    }

    fun observeSellerProducts(sellerUid: String): Flow<List<ZmProduct>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("products")
            .whereEqualTo("sellerUid", sellerUid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsProduct::class.java, tsBehavior)?.toZmProduct() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "products")
                throw e
            }
    }

    fun observeProductById(productId: String): Flow<ZmProduct?> {
        if (auth.currentUser == null || productId.isBlank()) return flowOf(null)
        val path = "products/$productId"
        return db.document(path)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap -> snap.toObject(FsProduct::class.java, tsBehavior)?.toZmProduct() }
            .catch { e ->
                handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    fun observeCart(uid: String): Flow<List<ZmCartItem>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        val path = "users/$uid/cart"
        return db.collection(path)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsCartItem::class.java, tsBehavior)?.toZmCartItem() }
                    .sortedByDescending { it.addedAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    fun observeCustomerOrders(uid: String): Flow<List<ZmOrder>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("orders")
            .whereEqualTo("customerId", uid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsOrder::class.java, tsBehavior)?.toZmOrder() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "orders")
                throw e
            }
    }

    fun observeSellerOrders(sellerUid: String): Flow<List<ZmOrder>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        return db.collection("orders")
            .whereArrayContains("sellerUids", sellerUid)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsOrder::class.java, tsBehavior)?.toZmOrder() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "orders")
                throw e
            }
    }

    fun observeNotifications(uid: String): Flow<List<ZmNotification>> {
        if (auth.currentUser == null) return flowOf(emptyList())
        val path = "users/$uid/notifications"
        return db.collection(path)
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsNotification::class.java, tsBehavior)?.toZmNotification(uid) }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    // --- Admin-Only Collection Observers ---

    fun observeAllProductsForAdmin(): Flow<List<ZmProduct>> {
        if (!isCurrentUserAdmin()) return flowOf(emptyList())
        return db.collection("products")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsProduct::class.java, tsBehavior)?.toZmProduct() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "products")
                throw e
            }
    }

    fun observeAllOrdersForAdmin(): Flow<List<ZmOrder>> {
        if (!isCurrentUserAdmin()) return flowOf(emptyList())
        return db.collection("orders")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsOrder::class.java, tsBehavior)?.toZmOrder() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "orders")
                throw e
            }
    }

    fun observeAllSellersForAdmin(): Flow<List<ZmSeller>> {
        if (!isCurrentUserAdmin()) return flowOf(emptyList())
        return db.collection("sellers")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsSellerProfile::class.java, tsBehavior)?.toZmSeller() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "sellers")
                throw e
            }
    }

    fun observeAllUsersForAdmin(): Flow<List<ZmUser>> {
        if (!isCurrentUserAdmin()) return flowOf(emptyList())
        return db.collection("users")
            .snapshots(MetadataChanges.EXCLUDE)
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(FsUserProfile::class.java, tsBehavior)?.toZmUser() }
                    .sortedByDescending { it.createdAt }
            }
            .catch { e ->
                handleFirestoreError(e, OperationType.LIST, "users")
                throw e
            }
    }

    // --- User Initialization & Admin Marketplace Seeding ---

    suspend fun ensureUserProfileAndSeedIfAdmin(user: FirebaseUser): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            val docRef = db.document(path)
            val snap = docRef.get().await()
            val email = (user.email ?: "").trim().ifBlank { "${user.uid}@zmbazaar.pk" }
            val isAdminAccount = email.equals(ZmBazaarDatabase.ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
            val displayName = (user.displayName ?: "").trim().ifBlank { email.substringBefore("@").ifBlank { "ZM Bazaar User" } }.take(100)

            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to user.uid,
                    "fullName" to displayName,
                    "phone" to (user.phoneNumber ?: "").take(30),
                    "email" to email.take(160),
                    "city" to "Barnala, Azad Kashmir",
                    "role" to if (isAdminAccount) UserRole.ADMIN else UserRole.CUSTOMER,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                createNotification(
                    recipientUid = user.uid,
                    title = "Welcome to ZM Bazaar, $displayName!",
                    message = "Your cloud account is synced across devices. Enjoy Pakistan Ka No.1 Online Bazaar!",
                    type = "ORDER"
                )
            } else if (isAdminAccount && snap.getString("role") != UserRole.ADMIN) {
                docRef.update(
                    mapOf(
                        "role" to UserRole.ADMIN,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (isAdminAccount) {
                seedDefaultMarketplaceIfEmpty(user.uid)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun seedDefaultMarketplaceIfEmpty(adminUid: String) {
        try {
            // 1. Ensure Admin's official store seller doc exists
            val adminSellerRef = db.document("sellers/$adminUid")
            if (!adminSellerRef.get().await().exists()) {
                adminSellerRef.set(
                    mapOf(
                        "userId" to adminUid,
                        "shopName" to "ZM Official Store",
                        "ownerName" to "ZM Bazaar HQ",
                        "phone" to "03558279470",
                        "cnic" to "81101-9876543-1",
                        "paymentMethod" to "Easypaisa",
                        "paymentAccountNumber" to "03455368275",
                        "paymentAccountTitle" to "ZM Bazaar",
                        "shopAddress" to "Main Bazaar, Barnala, Azad Kashmir, Pakistan",
                        "status" to SellerStatus.APPROVED,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // 2. Seed 9 default categories if empty
            if (db.collection("categories").limit(1).get().await().isEmpty) {
                val categories = listOf(
                    Triple("cat_clothes", "Clothes", Triple("clothes", 0xFF00A651L, 1)),
                    Triple("cat_shoes", "Shoes", Triple("shoes", 0xFFE31E24L, 2)),
                    Triple("cat_cosmetics", "Cosmetics", Triple("cosmetics", 0xFFD81B60L, 3)),
                    Triple("cat_mobile", "Mobile Accessories", Triple("mobile", 0xFF1565C0L, 4)),
                    Triple("cat_electronics", "Electronics", Triple("electronics", 0xFF000000L, 5)),
                    Triple("cat_home", "Home Items", Triple("home", 0xFFEF6C00L, 6)),
                    Triple("cat_kids", "Kids", Triple("kids", 0xFF8E24AAL, 7)),
                    Triple("cat_grocery", "Grocery", Triple("grocery", 0xFF2E7D32L, 8)),
                    Triple("cat_custom", "Custom Name Products", Triple("custom", 0xFFC62828L, 9))
                )
                for ((docId, name, meta) in categories) {
                    db.document("categories/$docId").set(
                        mapOf(
                            "name" to name,
                            "iconKey" to meta.first,
                            "accentHex" to meta.second,
                            "sortOrder" to meta.third,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // 3. Seed 3 home banners if empty
            if (db.collection("banners").limit(1).get().await().isEmpty) {
                val banners = listOf(
                    mapOf(
                        "id" to "ban_flash_sale",
                        "title" to "Mega Azadi Flash Sale",
                        "subtitle" to "Up to 55% OFF across Barnala & All Pakistan",
                        "badgeText" to "FLASH SALE",
                        "imageUri" to "drawable:img_banner_flash_sale",
                        "targetCategory" to "Electronics",
                        "sortOrder" to 1
                    ),
                    mapOf(
                        "id" to "ban_custom_gifts",
                        "title" to "Custom Name Gifts Studio",
                        "subtitle" to "Print Your Name & Photo on Mugs, Shirts & Wallets",
                        "badgeText" to "PERSONALIZED",
                        "imageUri" to "drawable:img_banner_custom_gifts",
                        "targetCategory" to "Custom Name Products",
                        "sortOrder" to 2
                    ),
                    mapOf(
                        "id" to "ban_kashmir",
                        "title" to "ZM Bazaar Kashmir Collection",
                        "subtitle" to "Fashion, Footwear & Cosmetics Delivered Nationwide",
                        "badgeText" to "NEW ARRIVALS",
                        "imageUri" to "drawable:img_banner_kashmir_bazaar",
                        "targetCategory" to "Clothes",
                        "sortOrder" to 3
                    )
                )
                for (b in banners) {
                    val docId = b["id"] as String
                    db.document("banners/$docId").set(
                        mapOf(
                            "title" to b["title"],
                            "subtitle" to b["subtitle"],
                            "badgeText" to b["badgeText"],
                            "imageUri" to b["imageUri"],
                            "targetCategory" to b["targetCategory"],
                            "active" to true,
                            "sortOrder" to b["sortOrder"],
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // 4. Seed Default Admin Payment Accounts if empty
            if (db.collection("payment_accounts").limit(1).get().await().isEmpty) {
                val accounts = listOf(
                    Triple("acc_easypaisa", "Easypaisa", "03455368275"),
                    Triple("acc_jazzcash", "JazzCash", "03285690902"),
                    Triple("acc_meezan", "Meezan Bank", "12930112996287")
                )
                for ((docId, provider, number) in accounts) {
                    db.document("payment_accounts/$docId").set(
                        mapOf(
                            "providerName" to provider,
                            "accountNumber" to number,
                            "accountTitle" to "ZM Bazaar",
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            // 5. Seed 12 starter products if empty
            if (db.collection("products").limit(1).get().await().isEmpty) {
                val starterProducts = listOf(
                    ProductSeed("prod_earbuds", "ZM Official Store", "Pro Wireless ANC Earbuds TWS", "Crystal-clear stereo sound with Active Noise Cancellation, 30-hour charging case, and fast Type-C charging.", "Mobile Accessories", 2499.0, 4500.0, 35, 4.8, 142, "preset:earbuds|drawable:img_banner_flash_sale", true, true, false),
                    ProductSeed("prod_mug", "ZM Official Store", "Custom Name Printed Ceramic Mug", "High-gloss ceramic mug with permanent sublimation printing of your custom name and optional photo.", "Custom Name Products", 850.0, 1200.0, 50, 4.9, 89, "preset:custom_mug|drawable:img_banner_custom_gifts", true, true, true),
                    ProductSeed("prod_kurta", "ZM Official Store", "Men's Premium Cotton Kurta Shalwar", "Breathable Pakistani festive cotton-linen blend kurta shalwar with fine collar stitching.", "Clothes", 3200.0, 4800.0, 22, 4.7, 64, "preset:kurta|drawable:img_banner_kashmir_bazaar", false, true, false),
                    ProductSeed("prod_sneakers", "ZM Official Store", "Urban Runner Sport Sneakers", "Lightweight breathable mesh running shoes with cushioned memory foam sole.", "Shoes", 2999.0, 4499.0, 18, 4.6, 51, "preset:sneakers|drawable:img_banner_flash_sale", true, true, false),
                    ProductSeed("prod_wallet", "ZM Official Store", "Custom Name Engraved Leather Wallet", "Genuine soft leather bi-fold wallet laser-engraved with your custom name in gift box.", "Custom Name Products", 1499.0, 2200.0, 40, 4.9, 112, "preset:custom_wallet|drawable:img_banner_custom_gifts", false, true, true),
                    ProductSeed("prod_serum", "ZM Official Store", "24K Vitamin C Glow Serum & Cleanser", "Dermatologist-tested brightening facial serum with hyaluronic acid and rosewater cleanser.", "Cosmetics", 1350.0, 1950.0, 28, 4.8, 73, "preset:cosmetics|drawable:img_banner_kashmir_bazaar", true, true, false),
                    ProductSeed("prod_watch", "ZM Official Store", "AMOLED Smart Watch Series Ultra", "1.96-inch HD AMOLED display, Bluetooth calling, heart rate & step tracker, 7-day battery backup.", "Electronics", 4999.0, 7500.0, 15, 4.7, 96, "preset:smartwatch|drawable:img_banner_flash_sale", true, true, false),
                    ProductSeed("prod_kettle", "ZM Official Store", "Stainless Steel Electric Kettle 1.8L", "Rapid-boil 1500W cordless stainless steel kettle with auto shut-off and cool-touch handle.", "Home Items", 2150.0, 3000.0, 20, 4.5, 38, "preset:kettle|drawable:img_banner_kashmir_bazaar", false, true, false),
                    ProductSeed("prod_kids", "ZM Official Store", "Kids Cotton Dino Hoodie & Trouser Set", "Soft fleece-lined 2-piece kids outfit, machine washable and gentle on skin for ages 2-8 years.", "Kids", 1650.0, 2400.0, 26, 4.8, 44, "preset:kids_set|drawable:img_banner_kashmir_bazaar", false, true, false),
                    ProductSeed("prod_honey", "ZM Official Store", "Pure Kashmir Wild Sidr Honey 500g", "100% raw, unfiltered mountain Sidr honey harvested from Azad Kashmir valleys.", "Grocery", 1900.0, 2500.0, 30, 4.9, 128, "preset:honey|drawable:img_banner_kashmir_bazaar", false, true, false),
                    ProductSeed("prod_powerbank", "ZM Official Store", "20000mAh 22.5W Fast Power Bank", "Dual USB + Type-C PD fast charging power bank with LED battery percentage display.", "Mobile Accessories", 3450.0, 4900.0, 25, 4.7, 59, "preset:powerbank|drawable:img_banner_flash_sale", false, true, false),
                    ProductSeed("prod_tshirt", "ZM Official Store", "Custom Name & Photo Cotton T-Shirt", "100% combed cotton round-neck T-shirt printed with your custom name and uploaded photo.", "Custom Name Products", 1299.0, 1800.0, 45, 4.8, 67, "preset:custom_shirt|drawable:img_banner_custom_gifts", true, true, true)
                )
                for (p in starterProducts) {
                    db.document("products/${p.id}").set(
                        mapOf(
                            "sellerUid" to adminUid,
                            "shopName" to p.shopName,
                            "name" to p.name,
                            "description" to p.description,
                            "category" to p.category,
                            "price" to p.price,
                            "oldPrice" to p.oldPrice,
                            "stock" to p.stock,
                            "rating" to p.rating,
                            "reviewCount" to p.reviewCount,
                            "imageUrls" to p.imageUrls,
                            "flashSale" to p.flashSale,
                            "featured" to p.featured,
                            "allowsCustomName" to p.allowsCustomName,
                            "approvalStatus" to ProductApproval.APPROVED,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirestoreSeed", "Marketplace seeding failed: ${e.message}", e)
        }
    }

    private data class ProductSeed(
        val id: String,
        val shopName: String,
        val name: String,
        val description: String,
        val category: String,
        val price: Double,
        val oldPrice: Double,
        val stock: Int,
        val rating: Double,
        val reviewCount: Int,
        val imageUrls: String,
        val flashSale: Boolean,
        val featured: Boolean,
        val allowsCustomName: Boolean
    )

    // --- Profile Update ---

    suspend fun updateUserProfile(fullName: String, phone: String, city: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanName = fullName.trim()
        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Full name is required."))
        val path = "users/$uid"
        return try {
            db.document(path).update(
                mapOf(
                    "fullName" to cleanName.take(100),
                    "phone" to phone.trim().take(30),
                    "city" to city.trim().take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Cart Operations ---

    suspend fun addToCart(
        product: ZmProduct,
        quantity: Int,
        customName: String = "",
        customPhotoUri: String = ""
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (product.stock <= 0) {
            return Result.failure(IllegalStateException("This product is currently out of stock."))
        }
        val path = "users/$uid/cart"
        return try {
            val cleanCustomName = customName.trim().take(120)
            val cleanCustomPhoto = customPhotoUri.trim().take(1000)
            val existingSnap = db.collection(path)
                .whereEqualTo("productId", product.id)
                .get()
                .await()
            val matchingDoc = existingSnap.documents.firstOrNull { doc ->
                (doc.getString("customName") ?: "") == cleanCustomName &&
                    (doc.getString("customPhotoUri") ?: "") == cleanCustomPhoto
            }
            if (matchingDoc != null) {
                val curQty = matchingDoc.getLong("quantity")?.toInt() ?: 1
                val newQty = (curQty + quantity).coerceIn(1, product.stock.coerceAtMost(999))
                matchingDoc.reference.update(
                    mapOf(
                        "quantity" to newQty,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val docRef = db.collection(path).document()
                val payload = mapOf(
                    "userId" to uid,
                    "productId" to product.id.take(128),
                    "sellerUid" to product.sellerId.ifBlank { uid }.take(128),
                    "productName" to product.name.take(160),
                    "shopName" to product.shopName.ifBlank { "ZM Bazaar Store" }.take(120),
                    "unitPrice" to product.price,
                    "oldPrice" to product.oldPrice.coerceAtLeast(0.0),
                    "imageUri" to product.primaryImage.take(1000),
                    "quantity" to quantity.coerceIn(1, product.stock.coerceAtMost(999)),
                    "customName" to cleanCustomName,
                    "customPhotoUri" to cleanCustomPhoto,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQuantity(item: ZmCartItem, newQty: Int): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docPath = "users/$uid/cart/${item.id}"
        return try {
            if (newQty <= 0) {
                db.document(docPath).delete().await()
            } else {
                db.document(docPath).update(
                    mapOf(
                        "quantity" to newQty.coerceIn(1, 999),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeCartItem(item: ZmCartItem): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val docPath = "users/$uid/cart/${item.id}"
        return try {
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Orders & Automatic 10% Commission ---

    suspend fun placeOrder(
        fullName: String,
        phone: String,
        address: String,
        city: String,
        note: String,
        paymentMethod: String,
        paymentAccountInfo: String,
        paymentScreenshotUri: String,
        cartSnapshot: List<ZmCartItem>
    ): Result<ZmOrder> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanName = fullName.trim()
        val cleanPhone = phone.trim()
        val cleanAddress = address.trim()
        val cleanCity = city.trim()
        if (cleanName.isBlank() || cleanPhone.length < 4 || cleanAddress.length < 3 || cleanCity.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter valid name, phone, address, and city."))
        }
        if (cartSnapshot.isEmpty()) {
            return Result.failure(IllegalStateException("Your cart is empty."))
        }

        val orderRef = db.collection("orders").document()
        val path = "orders/${orderRef.id}"
        return try {
            val subtotal = cartSnapshot.sumOf { it.unitPrice * it.quantity }
            val deliveryCharges = if (subtotal >= 3000.0) 0.0 else 150.0
            val grandTotal = subtotal + deliveryCharges
            val orderCode = "ZM-${(100000..999999).random()}"

            val sellerUids = cartSnapshot.map { it.sellerId.ifBlank { uid } }.distinct().take(20)
            val itemMaps = cartSnapshot.take(50).map { c ->
                val lineTotal = c.unitPrice * c.quantity
                mapOf(
                    "productId" to c.productId,
                    "sellerUid" to c.sellerId.ifBlank { uid },
                    "shopName" to c.shopName.ifBlank { "ZM Bazaar" },
                    "productName" to c.productName,
                    "imageUri" to c.imageUri.take(1000),
                    "unitPrice" to c.unitPrice,
                    "quantity" to c.quantity,
                    "lineTotal" to lineTotal,
                    "commissionAmount" to 0.0,
                    "sellerEarningAmount" to 0.0,
                    "payoutStatus" to PayoutStatus.NONE,
                    "customName" to c.customName.take(120),
                    "customPhotoUri" to c.customPhotoUri.take(1000)
                )
            }

            val payload = hashMapOf<String, Any>(
                "orderCode" to orderCode,
                "customerId" to uid,
                "customerName" to cleanName.take(120),
                "phone" to cleanPhone.take(30),
                "address" to cleanAddress.take(300),
                "city" to cleanCity.take(80),
                "paymentMethod" to paymentMethod.take(120),
                "subtotal" to subtotal,
                "deliveryCharges" to deliveryCharges,
                "grandTotal" to grandTotal,
                "commissionAmount" to 0.0,
                "sellerEarningAmount" to 0.0,
                "payoutStatus" to PayoutStatus.NONE,
                "status" to OrderStatus.PENDING,
                "sellerUids" to sellerUids,
                "items" to itemMaps,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (note.isNotBlank()) payload["note"] = note.trim().take(400)
            if (paymentAccountInfo.isNotBlank()) payload["paymentAccountInfo"] = paymentAccountInfo.take(200)
            if (paymentScreenshotUri.isNotBlank()) payload["paymentScreenshotUri"] = paymentScreenshotUri.take(1500)

            orderRef.set(payload).await()

            // Decrement stock cleanly per rule: affectedKeys().hasOnly(['stock', 'updatedAt'])
            cartSnapshot.forEach { cart ->
                try {
                    val prodRef = db.document("products/${cart.productId}")
                    val prodSnap = prodRef.get().await()
                    val currentStock = prodSnap.getLong("stock")?.toInt()
                    if (prodSnap.exists() && currentStock != null && currentStock > 0) {
                        val newStock = (currentStock - cart.quantity).coerceAtLeast(0)
                        prodRef.update(
                            mapOf(
                                "stock" to newStock,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                } catch (_: Exception) {
                    // Non-fatal if product was edited/removed concurrently
                }
            }

            // Clear user's cart
            val cartDocs = db.collection("users/$uid/cart").get().await()
            cartDocs.documents.forEach { it.reference.delete().await() }

            // Notifications for customer and sellers
            createNotification(
                recipientUid = uid,
                title = "Order Placed: $orderCode",
                message = "Thank you for ordering from ZM Bazaar! Total Rs. ${grandTotal.roundToInt()} ($paymentMethod).",
                type = "ORDER"
            )
            sellerUids.filter { it != uid }.forEach { sUid ->
                createNotification(
                    recipientUid = sUid,
                    title = "New Order Received: $orderCode",
                    message = "$cleanName ($cleanCity) ordered from your store.",
                    type = "ORDER"
                )
            }
            NotificationHelper.showNotification(
                context,
                "ZM Bazaar Order Confirmed!",
                "Order $orderCode placed successfully. Total Rs. ${grandTotal.roundToInt()}"
            )

            val createdOrder = ZmOrder(
                id = orderRef.id,
                orderCode = orderCode,
                customerId = uid,
                customerName = cleanName,
                phone = cleanPhone,
                address = cleanAddress,
                city = cleanCity,
                note = note.trim(),
                paymentMethod = paymentMethod,
                paymentAccountInfo = paymentAccountInfo,
                paymentScreenshotUri = paymentScreenshotUri,
                subtotal = subtotal,
                deliveryCharges = deliveryCharges,
                grandTotal = grandTotal,
                status = OrderStatus.PENDING
            )
            Result.success(createdOrder)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            val orderRef = db.document(path)
            val snap = orderRef.get().await()
            val fsOrder = snap.toObject(FsOrder::class.java, tsBehavior)
                ?: return Result.failure(IllegalStateException("Order not found"))

            var commission = fsOrder.commissionAmount
            var sellerEarning = fsOrder.sellerEarningAmount
            var payoutStatus = fsOrder.payoutStatus
            var updatedItems = fsOrder.items

            if (newStatus == OrderStatus.DELIVERED) {
                commission = ((fsOrder.subtotal * 0.10) * 100.0).roundToInt() / 100.0
                sellerEarning = ((fsOrder.subtotal - commission) * 100.0).roundToInt() / 100.0
                if (payoutStatus == PayoutStatus.NONE) {
                    payoutStatus = PayoutStatus.PENDING_PAYOUT
                }
                updatedItems = fsOrder.items.map { itemMap ->
                    val lineTotal = (itemMap["lineTotal"] as? Number)?.toDouble() ?: 0.0
                    val itemComm = ((lineTotal * 0.10) * 100.0).roundToInt() / 100.0
                    val itemEarn = ((lineTotal - itemComm) * 100.0).roundToInt() / 100.0
                    val existingItemPayout = itemMap["payoutStatus"] as? String ?: PayoutStatus.NONE
                    itemMap.toMutableMap().apply {
                        put("commissionAmount", itemComm)
                        put("sellerEarningAmount", itemEarn)
                        if (existingItemPayout == PayoutStatus.NONE) {
                            put("payoutStatus", PayoutStatus.PENDING_PAYOUT)
                        }
                    }
                }
            }

            orderRef.update(
                mapOf(
                    "status" to newStatus,
                    "commissionAmount" to commission,
                    "sellerEarningAmount" to sellerEarning,
                    "payoutStatus" to payoutStatus,
                    "items" to updatedItems,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            createNotification(
                recipientUid = fsOrder.customerId,
                title = "Order ${fsOrder.orderCode}: $newStatus",
                message = "Your ZM Bazaar order status is now $newStatus.",
                type = "ORDER"
            )
            NotificationHelper.showNotification(
                context,
                "Order ${fsOrder.orderCode} Updated",
                "Status changed to $newStatus"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendPaymentToSeller(seller: ZmSeller, amount: Double): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        return try {
            val ordersSnap = db.collection("orders")
                .whereArrayContains("sellerUids", seller.id)
                .get()
                .await()
            for (doc in ordersSnap.documents) {
                val fsOrder = doc.toObject(FsOrder::class.java, tsBehavior) ?: continue
                var modified = false
                val updatedItems = fsOrder.items.map { itemMap ->
                    val itemSellerUid = itemMap["sellerUid"] as? String ?: ""
                    val itemPayout = itemMap["payoutStatus"] as? String ?: PayoutStatus.NONE
                    if (itemSellerUid == seller.id && itemPayout == PayoutStatus.PENDING_PAYOUT) {
                        modified = true
                        itemMap.toMutableMap().apply { put("payoutStatus", PayoutStatus.PAID) }
                    } else {
                        itemMap
                    }
                }
                if (modified || fsOrder.payoutStatus == PayoutStatus.PENDING_PAYOUT) {
                    val allItemsPaid = updatedItems.all {
                        (it["payoutStatus"] as? String ?: PayoutStatus.NONE) != PayoutStatus.PENDING_PAYOUT
                    }
                    doc.reference.update(
                        mapOf(
                            "items" to updatedItems,
                            "payoutStatus" to if (allItemsPaid) PayoutStatus.PAID else fsOrder.payoutStatus,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            createNotification(
                recipientUid = seller.id,
                title = "Payout Sent: Rs. ${amount.roundToInt()}",
                message = "ZM Bazaar transferred Rs. ${amount.roundToInt()} to ${seller.paymentMethod} (${seller.paymentAccountNumber}).",
                type = "PAYOUT"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders")
            Result.failure(e)
        }
    }

    // --- Seller Operations ---

    suspend fun submitSellerApplication(
        shopName: String,
        ownerName: String,
        phone: String,
        cnic: String,
        paymentMethod: String,
        paymentNumber: String,
        shopAddress: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val sShop = shopName.trim()
        val sOwner = ownerName.trim()
        val sPhone = phone.trim()
        val sCnic = cnic.trim()
        val sNum = paymentNumber.trim()
        val sAddr = shopAddress.trim()

        if (sShop.length < 2 || sOwner.length < 2 || sPhone.length < 5 || sCnic.length < 5 || sNum.length < 4 || sAddr.length < 3) {
            return Result.failure(IllegalArgumentException("Please enter complete valid shop, phone, CNIC, payment number, and address."))
        }

        val path = "sellers/$uid"
        return try {
            val docRef = db.document(path)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "shopName" to sShop.take(120),
                        "phone" to sPhone.take(30),
                        "paymentMethod" to paymentMethod.take(40),
                        "paymentAccountNumber" to sNum.take(50),
                        "paymentAccountTitle" to sOwner.take(100),
                        "shopAddress" to sAddr.take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "shopName" to sShop.take(120),
                        "ownerName" to sOwner.take(100),
                        "phone" to sPhone.take(30),
                        "cnic" to sCnic.take(30),
                        "paymentMethod" to paymentMethod.take(40),
                        "paymentAccountNumber" to sNum.take(50),
                        "paymentAccountTitle" to sOwner.take(100),
                        "shopAddress" to sAddr.take(300),
                        "status" to if (isCurrentUserAdmin()) SellerStatus.APPROVED else SellerStatus.PENDING,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun approveSeller(seller: ZmSeller): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val path = "sellers/${seller.id}"
        return try {
            db.document(path).update(
                mapOf(
                    "status" to SellerStatus.APPROVED,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val userDoc = db.document("users/${seller.id}").get().await()
            if (userDoc.exists() && userDoc.getString("role") != UserRole.ADMIN) {
                userDoc.reference.update(
                    mapOf(
                        "role" to UserRole.SELLER,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            createNotification(
                recipientUid = seller.id,
                title = "Seller Account Approved!",
                message = "Congratulations! Your store '${seller.shopName}' is now approved on ZM Bazaar.",
                type = "SELLER"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectSeller(seller: ZmSeller): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val path = "sellers/${seller.id}"
        return try {
            db.document(path).update(
                mapOf(
                    "status" to SellerStatus.REJECTED,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            createNotification(
                recipientUid = seller.id,
                title = "Seller Application Update",
                message = "Your application for '${seller.shopName}' was rejected by Admin.",
                type = "SELLER"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun changeSellerPaymentAccount(
        sellerId: String,
        method: String,
        number: String,
        title: String
    ): Result<Unit> {
        val path = "sellers/$sellerId"
        return try {
            db.document(path).update(
                mapOf(
                    "paymentMethod" to method.trim().take(40),
                    "paymentAccountNumber" to number.trim().take(50),
                    "paymentAccountTitle" to title.trim().ifBlank { "Seller" }.take(100),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Product CRUD & Approval ---

    suspend fun saveProduct(product: ZmProduct): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val isNew = product.id.isBlank()
        val docRef = if (isNew) db.collection("products").document() else db.document("products/${product.id}")
        val path = "products/${docRef.id}"
        return try {
            if (isNew) {
                val approval = if (isCurrentUserAdmin()) ProductApproval.APPROVED else ProductApproval.PENDING
                docRef.set(
                    mapOf(
                        "sellerUid" to product.sellerId.ifBlank { uid },
                        "shopName" to product.shopName.ifBlank { "ZM Bazaar Store" }.take(120),
                        "name" to product.name.trim().take(160),
                        "description" to product.description.trim().take(2000),
                        "category" to product.category.trim().take(80),
                        "price" to product.price.coerceAtLeast(1.0),
                        "oldPrice" to product.oldPrice.coerceAtLeast(0.0),
                        "stock" to product.stock.coerceAtLeast(0),
                        "rating" to product.rating.coerceIn(0.0, 5.0),
                        "reviewCount" to product.reviewCount.coerceAtLeast(0),
                        "imageUrls" to product.imageUrls.ifBlank { "preset:default" }.take(2000),
                        "flashSale" to product.isFlashSale,
                        "featured" to product.isFeatured,
                        "allowsCustomName" to product.allowsCustomName,
                        "approvalStatus" to approval,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to product.name.trim().take(160),
                        "description" to product.description.trim().take(2000),
                        "category" to product.category.trim().take(80),
                        "price" to product.price.coerceAtLeast(1.0),
                        "oldPrice" to product.oldPrice.coerceAtLeast(0.0),
                        "stock" to product.stock.coerceAtLeast(0),
                        "imageUrls" to product.imageUrls.ifBlank { "preset:default" }.take(2000),
                        "flashSale" to product.isFlashSale,
                        "featured" to product.isFeatured,
                        "allowsCustomName" to product.allowsCustomName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val path = "products/$productId"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProductApproval(productId: String, status: String): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val path = "products/$productId"
        return try {
            db.document(path).update(
                mapOf(
                    "approvalStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Admin Category / Banner / Payment Account CRUD ---

    suspend fun saveCategory(category: ZmCategory): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val isNew = category.id.isBlank()
        val docRef = if (isNew) db.collection("categories").document() else db.document("categories/${category.id}")
        val path = "categories/${docRef.id}"
        return try {
            if (isNew) {
                docRef.set(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "iconKey" to category.iconKey.trim().ifBlank { "clothes" }.take(40),
                        "accentHex" to category.accentHex,
                        "sortOrder" to category.sortOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to category.name.trim().take(80),
                        "iconKey" to category.iconKey.trim().ifBlank { "clothes" }.take(40),
                        "accentHex" to category.accentHex,
                        "sortOrder" to category.sortOrder,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val path = "categories/$categoryId"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBanner(banner: ZmBanner): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val isNew = banner.id.isBlank()
        val docRef = if (isNew) db.collection("banners").document() else db.document("banners/${banner.id}")
        val path = "banners/${docRef.id}"
        return try {
            if (isNew) {
                docRef.set(
                    mapOf(
                        "title" to banner.title.trim().take(120),
                        "subtitle" to banner.subtitle.trim().take(240),
                        "badgeText" to banner.badgeText.trim().ifBlank { "OFFER" }.take(40),
                        "imageUri" to banner.imageUri.ifBlank { "drawable:img_banner_flash_sale" }.take(1000),
                        "targetCategory" to banner.targetCategory.trim().take(80),
                        "active" to banner.isActive,
                        "sortOrder" to banner.sortOrder,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to banner.title.trim().take(120),
                        "subtitle" to banner.subtitle.trim().take(240),
                        "badgeText" to banner.badgeText.trim().ifBlank { "OFFER" }.take(40),
                        "imageUri" to banner.imageUri.ifBlank { "drawable:img_banner_flash_sale" }.take(1000),
                        "targetCategory" to banner.targetCategory.trim().take(80),
                        "active" to banner.isActive,
                        "sortOrder" to banner.sortOrder,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBanner(bannerId: String): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val path = "banners/$bannerId"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAdminPaymentAccount(account: ZmPaymentAccount): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val isNew = account.id.isBlank()
        val docRef = if (isNew) db.collection("payment_accounts").document() else db.document("payment_accounts/${account.id}")
        val path = "payment_accounts/${docRef.id}"
        return try {
            if (isNew) {
                docRef.set(
                    mapOf(
                        "providerName" to account.providerName.trim().take(60),
                        "accountNumber" to account.accountNumber.trim().take(60),
                        "accountTitle" to account.accountTitle.trim().ifBlank { "ZM Bazaar" }.take(100),
                        "active" to account.isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "providerName" to account.providerName.trim().take(60),
                        "accountNumber" to account.accountNumber.trim().take(60),
                        "accountTitle" to account.accountTitle.trim().ifBlank { "ZM Bazaar" }.take(100),
                        "active" to account.isActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdminPaymentAccount(accountId: String): Result<Unit> {
        if (!isCurrentUserAdmin()) return Result.failure(IllegalAccessException("Admin access required"))
        val path = "payment_accounts/$accountId"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Notifications ---

    private suspend fun createNotification(
        recipientUid: String,
        title: String,
        message: String,
        type: String
    ) {
        val senderUid = auth.currentUser?.uid ?: return
        val path = "users/$recipientUid/notifications"
        try {
            db.collection(path).add(
                mapOf(
                    "senderUid" to senderUid,
                    "title" to title.take(140),
                    "message" to message.take(500),
                    "type" to type.take(30),
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("FirestoreNotif", "Unable to write notification to $path: ${e.message}")
        }
    }

    suspend fun markNotificationsRead(uid: String) {
        if (auth.currentUser == null) return
        val path = "users/$uid/notifications"
        try {
            val unread = db.collection(path).whereEqualTo("read", false).get().await()
            unread.documents.forEach { doc ->
                doc.reference.update(mapOf("read" to true)).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
        }
    }
}
