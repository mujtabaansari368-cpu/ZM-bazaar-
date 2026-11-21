package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ZmBazaarDatabase
import com.example.data.remote.FirestoreRepository
import com.example.ui.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CloudZmBazaarViewModel(
    application: Application,
    private val uid: String,
    firestore: FirebaseFirestore
) : AndroidViewModel(application), ZmStorefrontController {

    private val repo = FirestoreRepository(application, firestore)
    private val localDb = ZmBazaarDatabase.getInstance(application)

    private val _isCloudLoading = MutableStateFlow(true)
    override val isCloudLoading: StateFlow<Boolean> = _isCloudLoading.asStateFlow()

    private val _cloudError = MutableStateFlow<String?>(null)
    override val cloudError: StateFlow<String?> = _cloudError.asStateFlow()

    override val currentUser: StateFlow<ZmUser?> = repo.observeUserProfile(uid)
        .catch { e -> handleStreamError(e); emit(null) }
        .onEach { _isCloudLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    override val currentSeller: StateFlow<ZmSeller?> = repo.observeSellerProfile(uid)
        .catch { e -> handleStreamError(e); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val cloudCategories = repo.observeCategories()
        .catch { e -> handleStreamError(e); emit(emptyList()) }
    private val localFallbackCategories = localDb.categoryDao().observeCategories()
        .map { list -> list.map { it.toZmCategory() } }

    override val categories: StateFlow<List<ZmCategory>> = combine(
        cloudCategories,
        localFallbackCategories
    ) { cloud, local ->
        cloud.ifEmpty { local }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val cloudApprovedProducts = repo.observeApprovedProducts()
        .catch { e -> handleStreamError(e); emit(emptyList()) }
    private val localFallbackProducts = localDb.productDao().observeApprovedProducts()
        .map { list -> list.map { it.toZmProduct() } }

    override val approvedProducts: StateFlow<List<ZmProduct>> = combine(
        cloudApprovedProducts,
        cloudCategories,
        localFallbackProducts
    ) { cloud, cloudCats, local ->
        if (cloud.isNotEmpty() || cloudCats.isNotEmpty()) cloud else local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val cloudActiveBanners = repo.observeActiveBanners()
        .catch { e -> handleStreamError(e); emit(emptyList()) }
    private val localFallbackBanners = localDb.bannerDao().observeActiveBanners()
        .map { list -> list.map { it.toZmBanner() } }

    override val activeBanners: StateFlow<List<ZmBanner>> = combine(
        cloudActiveBanners,
        cloudCategories,
        localFallbackBanners
    ) { cloud, cloudCats, local ->
        if (cloud.isNotEmpty() || cloudCats.isNotEmpty()) cloud else local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val allBanners: StateFlow<List<ZmBanner>> = repo.observeAllBanners()
        .catch { e -> handleStreamError(e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val cloudPaymentAccounts = repo.observeAdminPaymentAccounts()
        .catch { e -> handleStreamError(e); emit(emptyList()) }
    private val localFallbackPayments = localDb.paymentAccountDao().observeAdminAccounts()
        .map { list -> list.map { it.toZmPaymentAccount() } }

    override val adminPaymentAccounts: StateFlow<List<ZmPaymentAccount>> = combine(
        cloudPaymentAccounts,
        cloudCategories,
        localFallbackPayments
    ) { cloud, cloudCats, local ->
        if (cloud.isNotEmpty() || cloudCats.isNotEmpty()) cloud else local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val cartItems: StateFlow<List<ZmCartItem>> = repo.observeCart(uid)
        .catch { e -> handleStreamError(e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val myOrders: StateFlow<List<ZmOrder>> = repo.observeCustomerOrders(uid)
        .catch { e -> handleStreamError(e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val sellerProducts: StateFlow<List<ZmProduct>> = repo.observeSellerProducts(uid)
        .catch { e -> handleStreamError(e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val sellerOrders: StateFlow<List<ZmOrder>> = repo.observeSellerOrders(uid)
        .catch { e -> handleStreamError(e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val sellerOrderItems: StateFlow<List<ZmOrderItem>> = sellerOrders
        .map { orders -> orders.flatMap { o -> o.items.filter { it.sellerId == uid } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val notifications: StateFlow<List<ZmNotification>> = repo.observeNotifications(uid)
        .catch { e -> handleStreamError(e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val isAdminFlow: Flow<Boolean> = currentUser.map { user ->
        user != null &&
            user.email.equals(ZmBazaarDatabase.ADMIN_EMAIL, ignoreCase = true) &&
            user.isEmailVerified
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allProducts: StateFlow<List<ZmProduct>> = isAdminFlow.flatMapLatest { isAdmin ->
        if (!isAdmin) approvedProducts
        else repo.observeAllProductsForAdmin().catch { e -> handleStreamError(e); emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allOrders: StateFlow<List<ZmOrder>> = isAdminFlow.flatMapLatest { isAdmin ->
        if (!isAdmin) sellerOrders
        else repo.observeAllOrdersForAdmin().catch { e -> handleStreamError(e); emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val allOrderItems: StateFlow<List<ZmOrderItem>> = combine(
        isAdminFlow,
        allOrders,
        myOrders
    ) { isAdmin, adminOrSellerOrders, customerOrders ->
        if (isAdmin) adminOrSellerOrders.flatMap { it.items }
        else (adminOrSellerOrders + customerOrders).distinctBy { it.id }.flatMap { it.items }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allSellers: StateFlow<List<ZmSeller>> = isAdminFlow.flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else repo.observeAllSellersForAdmin().catch { e -> handleStreamError(e); emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allUsers: StateFlow<List<ZmUser>> = isAdminFlow.flatMapLatest { isAdmin ->
        if (!isAdmin) flowOf(emptyList())
        else repo.observeAllUsersForAdmin().catch { e -> handleStreamError(e); emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            Firebase.auth.currentUser?.let { user ->
                repo.ensureUserProfileAndSeedIfAdmin(user).onFailure { err ->
                    _cloudError.value = err.localizedMessage ?: "Cloud sync error"
                }
            }
        }
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

    private fun handleStreamError(e: Throwable) {
        if (e is CancellationException) return
        val msg = (e as? FirebaseFirestoreException)?.localizedMessage ?: e.localizedMessage ?: "Firestore sync error"
        _cloudError.value = msg
        _isCloudLoading.value = false
    }

    override fun emitMessage(msg: String) {
        viewModelScope.launch { _snackbarEvents.emit(msg) }
    }

    override fun clearCloudError() {
        _cloudError.value = null
    }

    override fun setSearchQuery(q: String) { _searchQuery.value = q }
    override fun setSelectedCategory(cat: String) { _selectedCategory.value = cat }
    override fun setPriceFilter(filter: PriceFilter) { _selectedPriceFilter.value = filter }
    override fun setSortOption(sort: SortOption) { _selectedSort.value = sort }

    override fun observeProduct(productId: String): Flow<ZmProduct?> {
        return combine(
            approvedProducts,
            sellerProducts,
            repo.observeProductById(productId).catch { emit(null) }
        ) { approved, sellerProds, direct ->
            direct ?: approved.firstOrNull { it.id == productId } ?: sellerProds.firstOrNull { it.id == productId }
        }
    }

    override fun updateUserProfile(fullName: String, phone: String, city: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.updateUserProfile(fullName, phone, city)
                .onSuccess {
                    _snackbarEvents.emit("Profile updated!")
                    onSuccess()
                }
                .onFailure { _snackbarEvents.emit(it.message ?: "Unable to update profile") }
        }
    }

    override fun addToCart(product: ZmProduct, qty: Int, customName: String, customPhotoUri: String) {
        viewModelScope.launch {
            repo.addToCart(product, qty, customName, customPhotoUri)
                .onSuccess { _snackbarEvents.emit("Added ${product.name} to cart!") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Unable to add to cart") }
        }
    }

    override fun updateCartQuantity(item: ZmCartItem, newQty: Int) {
        viewModelScope.launch {
            repo.updateCartQuantity(item, newQty)
                .onFailure { _snackbarEvents.emit(it.message ?: "Failed to update quantity") }
        }
    }

    override fun removeCartItem(item: ZmCartItem) {
        viewModelScope.launch {
            repo.removeCartItem(item)
                .onSuccess { _snackbarEvents.emit("Removed from cart") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Failed to remove item") }
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
        viewModelScope.launch {
            repo.placeOrder(
                fullName = fullName,
                phone = phone,
                address = address,
                city = city,
                note = note,
                paymentMethod = paymentMethod,
                paymentAccountInfo = paymentAccountInfo,
                paymentScreenshotUri = paymentScreenshotUri,
                cartSnapshot = cartItems.value
            )
                .onSuccess { onSuccess(it) }
                .onFailure { onError(it.message ?: "Order placement failed") }
        }
    }

    override fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            repo.updateOrderStatus(orderId, status)
                .onSuccess { _snackbarEvents.emit("Order status updated to $status") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Failed to update status") }
        }
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
        viewModelScope.launch {
            repo.submitSellerApplication(shopName, ownerName, phone, cnic, paymentMethod, paymentNumber, shopAddress)
                .onSuccess {
                    _snackbarEvents.emit("Seller application submitted! Waiting for Admin approval.")
                    onSuccess()
                }
                .onFailure { onError(it.message ?: "Submission failed") }
        }
    }

    override fun approveSeller(seller: ZmSeller) {
        viewModelScope.launch {
            repo.approveSeller(seller)
                .onSuccess { _snackbarEvents.emit("${seller.shopName} approved!") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Approval failed") }
        }
    }

    override fun rejectSeller(seller: ZmSeller) {
        viewModelScope.launch {
            repo.rejectSeller(seller)
                .onSuccess { _snackbarEvents.emit("${seller.shopName} rejected.") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Rejection failed") }
        }
    }

    override fun sendPaymentToSeller(seller: ZmSeller, amount: Double) {
        viewModelScope.launch {
            repo.sendPaymentToSeller(seller, amount)
                .onSuccess { _snackbarEvents.emit("Payout sent to ${seller.shopName}") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Payout update failed") }
        }
    }

    override fun changeSellerPaymentAccount(sellerId: String, method: String, number: String, title: String) {
        viewModelScope.launch {
            repo.changeSellerPaymentAccount(sellerId, method, number, title)
                .onSuccess { _snackbarEvents.emit("Seller payment account updated!") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Update failed") }
        }
    }

    override fun saveProduct(product: ZmProduct) {
        viewModelScope.launch {
            repo.saveProduct(product)
                .onSuccess { _snackbarEvents.emit("Product saved to cloud!") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Save product failed") }
        }
    }

    override fun deleteProduct(product: ZmProduct) {
        viewModelScope.launch {
            repo.deleteProduct(product.id)
                .onSuccess { _snackbarEvents.emit("Product deleted") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Delete product failed") }
        }
    }

    override fun updateProductApproval(productId: String, status: String) {
        viewModelScope.launch {
            repo.updateProductApproval(productId, status)
                .onSuccess { _snackbarEvents.emit("Product marked $status") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Approval update failed") }
        }
    }

    override fun saveCategory(category: ZmCategory) {
        viewModelScope.launch {
            repo.saveCategory(category)
                .onSuccess { _snackbarEvents.emit("Category saved") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Save category failed") }
        }
    }

    override fun deleteCategory(category: ZmCategory) {
        viewModelScope.launch {
            repo.deleteCategory(category.id)
                .onSuccess { _snackbarEvents.emit("Category deleted") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Delete category failed") }
        }
    }

    override fun saveBanner(banner: ZmBanner) {
        viewModelScope.launch {
            repo.saveBanner(banner)
                .onSuccess { _snackbarEvents.emit("Banner updated") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Save banner failed") }
        }
    }

    override fun deleteBanner(banner: ZmBanner) {
        viewModelScope.launch {
            repo.deleteBanner(banner.id)
                .onSuccess { _snackbarEvents.emit("Banner removed") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Delete banner failed") }
        }
    }

    override fun saveAdminPaymentAccount(account: ZmPaymentAccount) {
        viewModelScope.launch {
            repo.saveAdminPaymentAccount(account)
                .onSuccess { _snackbarEvents.emit("Payment account saved") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Save account failed") }
        }
    }

    override fun deleteAdminPaymentAccount(account: ZmPaymentAccount) {
        viewModelScope.launch {
            repo.deleteAdminPaymentAccount(account.id)
                .onSuccess { _snackbarEvents.emit("Payment account deleted") }
                .onFailure { _snackbarEvents.emit(it.message ?: "Delete account failed") }
        }
    }

    override fun markNotificationsRead() {
        viewModelScope.launch {
            repo.markNotificationsRead(uid)
        }
    }

    class Factory(
        private val application: Application,
        private val uid: String,
        private val firestore: FirebaseFirestore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CloudZmBazaarViewModel(application, uid, firestore) as T
        }
    }
}
