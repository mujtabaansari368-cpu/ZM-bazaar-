package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.ZmBazaarRepository
import com.example.ui.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Guest / Unauthenticated Storefront ViewModel backed strictly by the local Room starter catalog.
 * Zero Firestore queries are issued while the user is unauthenticated; checkout, orders, seller
 * applications, and admin features prompt Google Sign-In via Credential Manager.
 */
class ZmBazaarViewModel(application: Application) : AndroidViewModel(application), ZmStorefrontController {
    private val db = ZmBazaarDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)
    private val repo = ZmBazaarRepository(application, db, sessionManager)

    override val currentUser: StateFlow<ZmUser?> = MutableStateFlow(null)
    override val currentSeller: StateFlow<ZmSeller?> = MutableStateFlow(null)

    override val categories: StateFlow<List<ZmCategory>> = repo.categories
        .map { list -> list.map { it.toZmCategory() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val approvedProducts: StateFlow<List<ZmProduct>> = repo.approvedProducts
        .map { list -> list.map { it.toZmProduct() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val allProducts: StateFlow<List<ZmProduct>> = approvedProducts

    override val activeBanners: StateFlow<List<ZmBanner>> = repo.activeBanners
        .map { list -> list.map { it.toZmBanner() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val allBanners: StateFlow<List<ZmBanner>> = activeBanners

    override val cartItems: StateFlow<List<ZmCartItem>> = repo.cartItems
        .map { list -> list.map { it.toZmCartItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val adminPaymentAccounts: StateFlow<List<ZmPaymentAccount>> = repo.adminPaymentAccounts
        .map { list -> list.map { it.toZmPaymentAccount() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val myOrders: StateFlow<List<ZmOrder>> = MutableStateFlow(emptyList())
    override val allOrders: StateFlow<List<ZmOrder>> = MutableStateFlow(emptyList())
    override val allOrderItems: StateFlow<List<ZmOrderItem>> = MutableStateFlow(emptyList())
    override val allSellers: StateFlow<List<ZmSeller>> = MutableStateFlow(emptyList())
    override val allUsers: StateFlow<List<ZmUser>> = MutableStateFlow(emptyList())
    override val sellerProducts: StateFlow<List<ZmProduct>> = MutableStateFlow(emptyList())
    override val sellerOrderItems: StateFlow<List<ZmOrderItem>> = MutableStateFlow(emptyList())
    override val sellerOrders: StateFlow<List<ZmOrder>> = MutableStateFlow(emptyList())

    override val notifications: StateFlow<List<ZmNotification>> = repo.currentUserNotifications
        .map { list -> list.map { it.toZmNotification() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val isCloudLoading: StateFlow<Boolean> = MutableStateFlow(false)
    override val cloudError: StateFlow<String?> = MutableStateFlow(null)

    private val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    override val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedPriceFilter = MutableStateFlow(PriceFilter.ALL)
    override val selectedPriceFilter: StateFlow<PriceFilter> = _selectedPriceFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(SortOption.NEWEST)
    override val selectedSort: StateFlow<SortOption> = _selectedSort.asStateFlow()

    override val filteredProducts: StateFlow<List<ZmProduct>> = combine(
        approvedProducts,
        _searchQuery,
        _selectedCategory,
        _selectedPriceFilter,
        _selectedSort
    ) { products, query, cat, priceFilter, sort ->
        val q = query.trim().lowercase()
        val filtered = products.filter { p ->
            val matchesQuery = q.isBlank() ||
                p.name.lowercase().contains(q) ||
                p.shopName.lowercase().contains(q) ||
                p.category.lowercase().contains(q)
            val matchesCategory = cat == "All" || p.category.equals(cat, ignoreCase = true)
            val matchesPrice = p.price >= priceFilter.min && p.price <= priceFilter.max
            matchesQuery && matchesCategory && matchesPrice
        }
        when (sort) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            SortOption.LOW_TO_HIGH -> filtered.sortedBy { it.price }
            SortOption.HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _flashSaleSecondsLeft = MutableStateFlow(5 * 3600 + 42 * 60 + 18)
    override val flashSaleSecondsLeft: StateFlow<Int> = _flashSaleSecondsLeft.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>()
    override val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _flashSaleSecondsLeft.value = if (_flashSaleSecondsLeft.value > 0) {
                    _flashSaleSecondsLeft.value - 1
                } else {
                    6 * 3600
                }
            }
        }
    }

    override fun emitMessage(msg: String) {
        viewModelScope.launch { _snackbarEvents.emit(msg) }
    }

    override fun clearCloudError() {}
    override fun setSearchQuery(q: String) { _searchQuery.value = q }
    override fun setSelectedCategory(cat: String) { _selectedCategory.value = cat }
    override fun setPriceFilter(filter: PriceFilter) { _selectedPriceFilter.value = filter }
    override fun setSortOption(sort: SortOption) { _selectedSort.value = sort }

    override fun observeProduct(productId: String): Flow<ZmProduct?> {
        val longId = productId.toLongOrNull() ?: return flowOf(null)
        return repo.observeProductById(longId).map { it?.toZmProduct() }
    }

    override fun updateUserProfile(fullName: String, phone: String, city: String, onSuccess: () -> Unit) {
        emitMessage("Please sign in with Google first.")
    }

    override fun addToCart(product: ZmProduct, qty: Int, customName: String, customPhotoUri: String) {
        val longId = product.id.toLongOrNull() ?: return
        viewModelScope.launch {
            val entity = ProductEntity(
                id = longId,
                sellerId = product.sellerId.toLongOrNull() ?: 1L,
                shopName = product.shopName,
                name = product.name,
                description = product.description,
                category = product.category,
                price = product.price,
                oldPrice = product.oldPrice,
                stock = product.stock,
                rating = product.rating,
                reviewCount = product.reviewCount,
                imageUrls = product.imageUrls,
                isFlashSale = product.isFlashSale,
                isFeatured = product.isFeatured,
                allowsCustomName = product.allowsCustomName,
                approvalStatus = product.approvalStatus
            )
            repo.addToCart(entity, qty, customName, customPhotoUri)
                .onSuccess { _snackbarEvents.emit("Added ${product.name} to cart! Sign in at checkout.") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Unable to add to cart") }
        }
    }

    override fun updateCartQuantity(item: ZmCartItem, newQty: Int) {
        val longId = item.id.toLongOrNull() ?: return
        viewModelScope.launch {
            repo.updateCartQuantity(
                CartItemEntity(
                    id = longId,
                    productId = item.productId.toLongOrNull() ?: 0L,
                    sellerId = item.sellerId.toLongOrNull() ?: 0L,
                    productName = item.productName,
                    shopName = item.shopName,
                    unitPrice = item.unitPrice,
                    oldPrice = item.oldPrice,
                    imageUri = item.imageUri,
                    quantity = item.quantity,
                    customName = item.customName,
                    customPhotoUri = item.customPhotoUri,
                    addedAt = item.addedAt
                ),
                newQty
            )
        }
    }

    override fun removeCartItem(item: ZmCartItem) {
        val longId = item.id.toLongOrNull() ?: return
        viewModelScope.launch {
            repo.removeCartItem(
                CartItemEntity(
                    id = longId,
                    productId = item.productId.toLongOrNull() ?: 0L,
                    sellerId = item.sellerId.toLongOrNull() ?: 0L,
                    productName = item.productName,
                    shopName = item.shopName,
                    unitPrice = item.unitPrice,
                    oldPrice = item.oldPrice,
                    imageUri = item.imageUri,
                    quantity = item.quantity,
                    customName = item.customName,
                    customPhotoUri = item.customPhotoUri,
                    addedAt = item.addedAt
                )
            )
            _snackbarEvents.emit("Removed from cart")
        }
    }

    override fun placeOrder(
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
    ) {
        onError("Please sign in with Google before placing an order.")
    }

    override fun updateOrderStatus(orderId: String, status: String) {
        emitMessage("Sign in required.")
    }

    override fun submitSellerApplication(
        shopName: String,
        ownerName: String,
        phone: String,
        cnic: String,
        paymentMethod: String,
        paymentNumber: String,
        shopAddress: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        onError("Please sign in with Google first to apply as a seller.")
    }

    override fun approveSeller(seller: ZmSeller) {}
    override fun rejectSeller(seller: ZmSeller) {}
    override fun sendPaymentToSeller(seller: ZmSeller, amount: Double) {}
    override fun changeSellerPaymentAccount(sellerId: String, method: String, number: String, title: String) {}
    override fun saveProduct(product: ZmProduct) {}
    override fun deleteProduct(product: ZmProduct) {}
    override fun updateProductApproval(productId: String, status: String) {}
    override fun saveCategory(category: ZmCategory) {}
    override fun deleteCategory(category: ZmCategory) {}
    override fun saveBanner(banner: ZmBanner) {}
    override fun deleteBanner(banner: ZmBanner) {}
    override fun saveAdminPaymentAccount(account: ZmPaymentAccount) {}
    override fun deleteAdminPaymentAccount(account: ZmPaymentAccount) {}

    override fun markNotificationsRead() {
        viewModelScope.launch { repo.markNotificationsRead(0L) }
    }
}
