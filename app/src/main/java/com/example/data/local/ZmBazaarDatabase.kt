package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Database(
    entities = [
        UserEntity::class,
        SellerEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        BannerEntity::class,
        CartItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentAccountEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZmBazaarDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sellerDao(): SellerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun bannerDao(): BannerDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentAccountDao(): PaymentAccountDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val ADMIN_EMAIL = "ansariproducts05@gmail.com"

        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        @Volatile
        private var INSTANCE: ZmBazaarDatabase? = null

        fun getInstance(context: Context): ZmBazaarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ZmBazaarDatabase::class.java,
                    "zm_bazaar_db"
                )
                    .addCallback(SeedCallback())
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedInitialData(database)
                }
            }
        }

        private suspend fun seedInitialData(db: ZmBazaarDatabase) {
            val userDao = db.userDao()
            val sellerDao = db.sellerDao()
            val categoryDao = db.categoryDao()
            val productDao = db.productDao()
            val bannerDao = db.bannerDao()
            val paymentAccountDao = db.paymentAccountDao()
            val notificationDao = db.notificationDao()

            // 1. Seed Admin, Sample Seller, and Sample Customer
            userDao.insertUser(
                UserEntity(
                    fullName = "ZM Bazaar Admin",
                    phone = "03558279470",
                    email = ADMIN_EMAIL,
                    passwordHash = sha256("admin123"),
                    city = "Barnala, Azad Kashmir",
                    role = UserRole.ADMIN
                )
            )
            val sellerUserId = userDao.insertUser(
                UserEntity(
                    fullName = "Kashmir Crafts Seller",
                    phone = "03451234567",
                    email = "seller@zmbazaar.pk",
                    passwordHash = sha256("seller123"),
                    city = "Bhimber, Azad Kashmir",
                    role = UserRole.SELLER
                )
            )
            userDao.insertUser(
                UserEntity(
                    fullName = "Bilal Ahmed",
                    phone = "03001122334",
                    email = "customer@zmbazaar.pk",
                    passwordHash = sha256("customer123"),
                    city = "Mirpur, Azad Kashmir",
                    role = UserRole.CUSTOMER
                )
            )

            // 2. Seed Approved Seller Store
            val sellerId = sellerDao.insertSeller(
                SellerEntity(
                    userId = sellerUserId,
                    shopName = "Kashmir Craft Studio",
                    ownerName = "Hamza Ansari",
                    phone = "03451234567",
                    cnic = "81101-1234567-1",
                    paymentMethod = "Easypaisa",
                    paymentAccountNumber = "03451234567",
                    paymentAccountTitle = "Hamza Ansari",
                    shopAddress = "Main Bazaar Road, Barnala, Azad Kashmir",
                    status = SellerStatus.APPROVED
                )
            )
            val secondSellerId = sellerDao.insertSeller(
                SellerEntity(
                    userId = 1L,
                    shopName = "ZM Official Store",
                    ownerName = "ZM Bazaar Barnala",
                    phone = "03558279470",
                    cnic = "81101-9876543-1",
                    paymentMethod = "Easypaisa",
                    paymentAccountNumber = "03455368275",
                    paymentAccountTitle = "ZM Bazaar",
                    shopAddress = "Barnala, Azad Kashmir, Pakistan",
                    status = SellerStatus.APPROVED
                )
            )

            // 3. Seed Seller Payment Account
            paymentAccountDao.insertAccount(
                PaymentAccountEntity(
                    ownerType = "SELLER",
                    ownerId = sellerId,
                    providerName = "Easypaisa",
                    accountNumber = "03451234567",
                    accountTitle = "Hamza Ansari"
                )
            )

            // 4. Seed Default Admin Payment Accounts
            paymentAccountDao.insertAccount(
                PaymentAccountEntity(
                    ownerType = "ADMIN",
                    ownerId = 0L,
                    providerName = "Easypaisa",
                    accountNumber = "03455368275",
                    accountTitle = "ZM Bazaar"
                )
            )
            paymentAccountDao.insertAccount(
                PaymentAccountEntity(
                    ownerType = "ADMIN",
                    ownerId = 0L,
                    providerName = "JazzCash",
                    accountNumber = "03285690902",
                    accountTitle = "ZM Bazaar"
                )
            )
            paymentAccountDao.insertAccount(
                PaymentAccountEntity(
                    ownerType = "ADMIN",
                    ownerId = 0L,
                    providerName = "Meezan Bank",
                    accountNumber = "12930112996287",
                    accountTitle = "ZM Bazaar"
                )
            )

            // 5. Seed 9 Required Categories
            val categories = listOf(
                CategoryEntity(name = "Clothes", iconKey = "clothes", accentHex = 0xFF00A651, sortOrder = 1),
                CategoryEntity(name = "Shoes", iconKey = "shoes", accentHex = 0xFFE31E24, sortOrder = 2),
                CategoryEntity(name = "Cosmetics", iconKey = "cosmetics", accentHex = 0xFFD81B60, sortOrder = 3),
                CategoryEntity(name = "Mobile Accessories", iconKey = "mobile", accentHex = 0xFF1565C0, sortOrder = 4),
                CategoryEntity(name = "Electronics", iconKey = "electronics", accentHex = 0xFF000000, sortOrder = 5),
                CategoryEntity(name = "Home Items", iconKey = "home", accentHex = 0xFFEF6C00, sortOrder = 6),
                CategoryEntity(name = "Kids", iconKey = "kids", accentHex = 0xFF8E24AA, sortOrder = 7),
                CategoryEntity(name = "Grocery", iconKey = "grocery", accentHex = 0xFF2E7D32, sortOrder = 8),
                CategoryEntity(name = "Custom Name Products", iconKey = "custom", accentHex = 0xFFC62828, sortOrder = 9)
            )
            categories.forEach { categoryDao.insertCategory(it) }

            // 6. Seed 3 Home Slider Banners
            bannerDao.insertBanner(
                BannerEntity(
                    title = "Mega Azadi Flash Sale",
                    subtitle = "Up to 55% OFF across Barnala & All Pakistan",
                    badgeText = "FLASH SALE",
                    imageUri = "drawable:img_banner_flash_sale",
                    targetCategory = "Electronics",
                    sortOrder = 1
                )
            )
            bannerDao.insertBanner(
                BannerEntity(
                    title = "Custom Name Gifts Studio",
                    subtitle = "Print Your Name & Photo on Mugs, Shirts & Wallets",
                    badgeText = "PERSONALIZED",
                    imageUri = "drawable:img_banner_custom_gifts",
                    targetCategory = "Custom Name Products",
                    sortOrder = 2
                )
            )
            bannerDao.insertBanner(
                BannerEntity(
                    title = "ZM Bazaar Kashmir Collection",
                    subtitle = "Fashion, Footwear & Cosmetics Delivered Nationwide",
                    badgeText = "NEW ARRIVALS",
                    imageUri = "drawable:img_banner_kashmir_bazaar",
                    targetCategory = "Clothes",
                    sortOrder = 3
                )
            )

            // 7. Seed Rich Approved Starter Products across all 9 Categories
            val now = System.currentTimeMillis()
            val seedProducts = listOf(
                ProductEntity(
                    sellerId = secondSellerId,
                    shopName = "ZM Official Store",
                    name = "Pro Wireless ANC Earbuds TWS",
                    description = "Crystal-clear stereo sound with Active Noise Cancellation, 30-hour charging case, and fast Type-C charging. Ideal for calls and music.",
                    category = "Mobile Accessories",
                    price = 2499.0,
                    oldPrice = 4500.0,
                    stock = 35,
                    rating = 4.8,
                    reviewCount = 142,
                    imageUrls = "preset:earbuds|drawable:img_banner_flash_sale",
                    isFlashSale = true,
                    isFeatured = true,
                    createdAt = now - 10000
                ),
                ProductEntity(
                    sellerId = sellerId,
                    shopName = "Kashmir Craft Studio",
                    name = "Custom Name Printed Ceramic Mug",
                    description = "High-gloss ceramic mug with permanent sublimation printing of your custom name and optional photo. Perfect gift for birthdays and anniversaries.",
                    category = "Custom Name Products",
                    price = 850.0,
                    oldPrice = 1200.0,
                    stock = 50,
                    rating = 4.9,
                    reviewCount = 89,
                    imageUrls = "preset:custom_mug|drawable:img_banner_custom_gifts",
                    isFlashSale = true,
                    isFeatured = true,
                    allowsCustomName = true,
                    createdAt = now - 20000
                ),
                ProductEntity(
                    sellerId = secondSellerId,
                    shopName = "ZM Official Store",
                    name = "Men's Premium Cotton Kurta Shalwar",
                    description = "Breathable Pakistani festive cotton-linen blend kurta shalwar with fine collar stitching. Comfortable all-season wear.",
                    category = "Clothes",
                    price = 3200.0,
                    oldPrice = 4800.0,
                    stock = 22,
                    rating = 4.7,
                    reviewCount = 64,
                    imageUrls = "preset:kurta|drawable:img_banner_kashmir_bazaar",
                    isFlashSale = false,
                    isFeatured = true,
                    createdAt = now - 30000
                ),
                ProductEntity(
                    sellerId = secondSellerId,
                    shopName = "ZM Official Store",
                    name = "Urban Runner Sport Sneakers",
                    description = "Lightweight breathable mesh running shoes with cushioned memory foam sole for daily walks and gym workouts.",
                    category = "Shoes",
                    price = 2999.0,
                    oldPrice = 4499.0,
                    stock = 18,
                    rating = 4.6,
                    reviewCount = 51,
                    imageUrls = "preset:sneakers|drawable:img_banner_flash_sale",
                    isFlashSale = true,
                    isFeatured = true,
                    createdAt = now - 40000
                ),
                ProductEntity(
                    sellerId = sellerId,
                    shopName = "Kashmir Craft Studio",
                    name = "Custom Name Engraved Leather Wallet",
                    description = "Genuine soft leather bi-fold wallet laser-engraved with your custom name. Includes luxury gift box packaging.",
                    category = "Custom Name Products",
                    price = 1499.0,
                    oldPrice = 2200.0,
                    stock = 40,
                    rating = 4.9,
                    reviewCount = 112,
                    imageUrls = "preset:custom_wallet|drawable:img_banner_custom_gifts",
                    isFlashSale = false,
                    isFeatured = true,
                    allowsCustomName = true,
                    createdAt = now - 50000
                ),
                ProductEntity(
                    sellerId = secondSellerId,
                    shopName = "ZM Official Store",
                    name = "24K Vitamin C Glow Serum & Cleanser",
                    description = "Dermatologist-tested brightening facial serum with hyaluronic acid and rosewater cleanser for radiant skin.",
                    category = "Cosmetics",
                    price = 1350.0,
                    oldPrice = 1950.0,
                    stock = 28,
                    rating = 4.8,
                    reviewCount = 73,
                    imageUrls = "preset:cosmetics|drawable:img_banner_kashmir_bazaar",
                    isFlashSale = true,
                    isFeatured = true,
                    createdAt = now - 60000
                ),
                ProductEntity(
                    sellerId = secondSellerId,
                    shopName = "ZM Official Store",
                    name = "AMOLED Smart Watch Series Ultra",
                    description = "1.96-inch HD AMOLED display, Bluetooth calling, heart rate & step tracker, 7-day battery backup.",
                    category = "Electronics",
                    price = 4999.0,
                    oldPrice = 7500.0,
                    stock = 15,
                    rating = 4.7,
                    reviewCount = 96,
                    imageUrls = "preset:smartwatch|drawable:img_banner_flash_sale",
                    isFlashSale = true,
                    isFeatured = true,
                    createdAt = now - 70000
                ),
                ProductEntity(
                    sellerId = sellerId,
                    shopName = "Kashmir Craft Studio",
                    name = "Stainless Steel Electric Kettle 1.8L",
                    description = "Rapid-boil 1500W cordless stainless steel kettle with auto shut-off and cool-touch handle for kitchen use.",
                    category = "Home Items",
                    price = 2150.0,
                    oldPrice = 3000.0,
                    stock = 20,
                    rating = 4.5,
                    reviewCount = 38,
                    imageUrls = "preset:kettle|drawable:img_banner_kashmir_bazaar",
                    isFlashSale = false,
                    isFeatured = true,
                    createdAt = now - 80000
                ),
                ProductEntity(
                    sellerId = sellerId,
                    shopName = "Kashmir Craft Studio",
                    name = "Kids Cotton Dino Hoodie & Trouser Set",
                    description = "Soft fleece-lined 2-piece kids outfit, machine washable and gentle on skin for ages 2-8 years.",
                    category = "Kids",
                    price = 1650.0,
                    oldPrice = 2400.0,
                    stock = 26,
                    rating = 4.8,
                    reviewCount = 44,
                    imageUrls = "preset:kids_set|drawable:img_banner_kashmir_bazaar",
                    isFlashSale = false,
                    isFeatured = true,
                    createdAt = now - 90000
                ),
                ProductEntity(
                    sellerId = sellerId,
                    shopName = "Kashmir Craft Studio",
                    name = "Pure Kashmir Wild Sidr Honey 500g",
                    description = "100% raw, unfiltered mountain Sidr honey harvested from Azad Kashmir valleys. Lab tested purity.",
                    category = "Grocery",
                    price = 1900.0,
                    oldPrice = 2500.0,
                    stock = 30,
                    rating = 4.9,
                    reviewCount = 128,
                    imageUrls = "preset:honey|drawable:img_banner_kashmir_bazaar",
                    isFlashSale = false,
                    isFeatured = true,
                    createdAt = now - 100000
                ),
                ProductEntity(
                    sellerId = secondSellerId,
                    shopName = "ZM Official Store",
                    name = "20000mAh 22.5W Fast Power Bank",
                    description = "Dual USB + Type-C PD fast charging power bank with LED battery percentage display.",
                    category = "Mobile Accessories",
                    price = 3450.0,
                    oldPrice = 4900.0,
                    stock = 25,
                    rating = 4.7,
                    reviewCount = 59,
                    imageUrls = "preset:powerbank|drawable:img_banner_flash_sale",
                    isFlashSale = false,
                    isFeatured = true,
                    createdAt = now - 110000
                ),
                ProductEntity(
                    sellerId = sellerId,
                    shopName = "Kashmir Craft Studio",
                    name = "Custom Name & Photo Cotton T-Shirt",
                    description = "100% combed cotton round-neck T-shirt printed with your custom name and uploaded photo in vivid fade-proof colors.",
                    category = "Custom Name Products",
                    price = 1299.0,
                    oldPrice = 1800.0,
                    stock = 45,
                    rating = 4.8,
                    reviewCount = 67,
                    imageUrls = "preset:custom_shirt|drawable:img_banner_custom_gifts",
                    isFlashSale = true,
                    isFeatured = true,
                    allowsCustomName = true,
                    createdAt = now - 120000
                )
            )
            seedProducts.forEach { productDao.insertProduct(it) }

            notificationDao.insertNotification(
                NotificationEntity(
                    userId = 0L,
                    title = "Welcome to ZM Bazaar!",
                    message = "Pakistan Ka No.1 Online Bazaar — Enjoy Flash Deals & Custom Name Products across Azad Kashmir & Pakistan.",
                    type = "ORDER"
                )
            )
        }
    }
}
