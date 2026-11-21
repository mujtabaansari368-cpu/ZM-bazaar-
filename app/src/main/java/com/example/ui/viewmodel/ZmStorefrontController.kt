package com.example.ui.viewmodel

import com.example.ui.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class SortOption(val label: String) {
    NEWEST("Newest"),
    LOW_TO_HIGH("Price: Low to High"),
    HIGH_TO_LOW("Price: High to Low")
}

enum class PriceFilter(val label: String, val min: Double, val max: Double) {
    ALL("All Prices", 0.0, Double.MAX_VALUE),
    UNDER_1500("Under Rs. 1,500", 0.0, 1500.0),
    RANGE_1500_3500("Rs. 1,500 - 3,500", 1500.0, 3500.0),
    ABOVE_3500("Above Rs. 3,500", 3500.0, Double.MAX_VALUE)
}

interface ZmStorefrontController {
    val currentUser: StateFlow<ZmUser?>
    val currentSeller: StateFlow<ZmSeller?>
    val categories: StateFlow<List<ZmCategory>>
    val approvedProducts: StateFlow<List<ZmProduct>>
    val allProducts: StateFlow<List<ZmProduct>>
    val filteredProducts: StateFlow<List<ZmProduct>>
    val activeBanners: StateFlow<List<ZmBanner>>
    val allBanners: StateFlow<List<ZmBanner>>
    val cartItems: StateFlow<List<ZmCartItem>>
    val adminPaymentAccounts: StateFlow<List<ZmPaymentAccount>>
    val myOrders: StateFlow<List<ZmOrder>>
    val allOrders: StateFlow<List<ZmOrder>>
    val allOrderItems: StateFlow<List<ZmOrderItem>>
    val allSellers: StateFlow<List<ZmSeller>>
    val allUsers: StateFlow<List<ZmUser>>
    val sellerProducts: StateFlow<List<ZmProduct>>
    val sellerOrderItems: StateFlow<List<ZmOrderItem>>
    val sellerOrders: StateFlow<List<ZmOrder>>
    val notifications: StateFlow<List<ZmNotification>>
    val flashSaleSecondsLeft: StateFlow<Int>
    val searchQuery: StateFlow<String>
    val selectedCategory: StateFlow<String>
    val selectedPriceFilter: StateFlow<PriceFilter>
    val selectedSort: StateFlow<SortOption>
    val snackbarEvents: SharedFlow<String>
    val isCloudLoading: StateFlow<Boolean>
    val cloudError: StateFlow<String?>

    fun emitMessage(msg: String)
    fun clearCloudError()
    fun setSearchQuery(q: String)
    fun setSelectedCategory(cat: String)
    fun setPriceFilter(filter: PriceFilter)
    fun setSortOption(sort: SortOption)
    fun observeProduct(productId: String): Flow<ZmProduct?>

    fun updateUserProfile(fullName: String, phone: String, city: String, onSuccess: () -> Unit = {})
    fun addToCart(product: ZmProduct, qty: Int = 1, customName: String = "", customPhotoUri: String = "")
    fun updateCartQuantity(item: ZmCartItem, newQty: Int)
    fun removeCartItem(item: ZmCartItem)

    fun placeOrder(
        fullName: String,
        phone: String,
        address: String,
        city: String,
        note: String,
        paymentMethod: String,
        paymentAccountInfo: String,
        paymentScreenshotUri: String,
        onSuccess: (ZmOrder) -> Unit,
        onError: (String) -> Unit
    )

    fun updateOrderStatus(orderId: String, status: String)
    fun submitSellerApplication(
        shopName: String,
        ownerName: String,
        phone: String,
        cnic: String,
        paymentMethod: String,
        paymentNumber: String,
        shopAddress: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )

    fun approveSeller(seller: ZmSeller)
    fun rejectSeller(seller: ZmSeller)
    fun sendPaymentToSeller(seller: ZmSeller, amount: Double)
    fun changeSellerPaymentAccount(sellerId: String, method: String, number: String, title: String)
    fun saveProduct(product: ZmProduct)
    fun deleteProduct(product: ZmProduct)
    fun updateProductApproval(productId: String, status: String)
    fun saveCategory(category: ZmCategory)
    fun deleteCategory(category: ZmCategory)
    fun saveBanner(banner: ZmBanner)
    fun deleteBanner(banner: ZmBanner)
    fun saveAdminPaymentAccount(account: ZmPaymentAccount)
    fun deleteAdminPaymentAccount(account: ZmPaymentAccount)
    fun markNotificationsRead()
}
