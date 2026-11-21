package com.example.ui.navigation

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.authStateFlow
import com.example.ui.components.WhatsAppFloatingButton
import com.example.ui.components.ZmTopHeader
import com.example.ui.screens.*
import com.example.ui.theme.ZmGreen
import com.example.ui.theme.ZmMutedText
import com.example.ui.theme.ZmRed
import com.example.ui.theme.ZmRedLight
import com.example.ui.theme.ZmWhite
import com.example.ui.viewmodel.CloudZmBazaarViewModel
import com.example.ui.viewmodel.PriceFilter
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.ZmBazaarViewModel
import com.example.ui.viewmodel.ZmStorefrontController
import com.example.util.IntentHelper
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ZmBazaarApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val credentialManager = remember { CredentialManager.create(context) }
    val firebaseUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = this
        )
    }

    if (firebaseUser == null) {
        val guestVm: ZmBazaarViewModel = viewModel()
        ZmBazaarScaffold(vm = guestVm)
    } else {
        val uid = firebaseUser!!.uid
        key(uid) {
            val firestore = remember(uid) {
                FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
            }
            val cloudVm: CloudZmBazaarViewModel = viewModel(
                key = uid,
                factory = CloudZmBazaarViewModel.Factory(application, uid, firestore)
            )
            ZmBazaarScaffold(vm = cloudVm)
        }
    }
}

@Composable
private fun ZmBazaarScaffold(vm: ZmStorefrontController) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by vm.currentUser.collectAsStateWithLifecycle()
    val currentSeller by vm.currentSeller.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val approvedProducts by vm.approvedProducts.collectAsStateWithLifecycle()
    val allProducts by vm.allProducts.collectAsStateWithLifecycle()
    val filteredProducts by vm.filteredProducts.collectAsStateWithLifecycle()
    val activeBanners by vm.activeBanners.collectAsStateWithLifecycle()
    val allBanners by vm.allBanners.collectAsStateWithLifecycle()
    val cartItems by vm.cartItems.collectAsStateWithLifecycle()
    val adminPaymentAccounts by vm.adminPaymentAccounts.collectAsStateWithLifecycle()
    val myOrders by vm.myOrders.collectAsStateWithLifecycle()
    val allOrders by vm.allOrders.collectAsStateWithLifecycle()
    val allOrderItems by vm.allOrderItems.collectAsStateWithLifecycle()
    val allSellers by vm.allSellers.collectAsStateWithLifecycle()
    val allUsers by vm.allUsers.collectAsStateWithLifecycle()
    val sellerProducts by vm.sellerProducts.collectAsStateWithLifecycle()
    val sellerOrderItems by vm.sellerOrderItems.collectAsStateWithLifecycle()
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    val flashSaleSecondsLeft by vm.flashSaleSecondsLeft.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by vm.selectedCategory.collectAsStateWithLifecycle()
    val selectedPriceFilter by vm.selectedPriceFilter.collectAsStateWithLifecycle()
    val selectedSort by vm.selectedSort.collectAsStateWithLifecycle()
    val isCloudLoading by vm.isCloudLoading.collectAsStateWithLifecycle()
    val cloudError by vm.cloudError.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }

    // Request POST_NOTIFICATIONS on Android 13+ once
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        NotificationHelper.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(vm) {
        vm.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME
    val cartCount = cartItems.sumOf { it.quantity }
    val unreadNotifs = notifications.count { !it.isRead }
    val isTopTab = currentRoute in listOf(Routes.HOME, Routes.CATALOG_WITH_CATEGORY, Routes.CART, Routes.MY_ORDERS, Routes.ACCOUNT)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ZmTopHeader(
                currentUser = currentUser,
                cartCount = cartCount,
                unreadNotifications = unreadNotifs,
                searchQuery = searchQuery,
                showSearchBar = currentRoute == Routes.HOME || currentRoute.startsWith("catalog"),
                showBackButton = !isTopTab && currentRoute != Routes.HOME,
                onBackClick = { navController.popBackStack() },
                onLogoClick = {
                    vm.setSelectedCategory("All")
                    vm.setSearchQuery("")
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onSearchQueryChange = { q -> vm.setSearchQuery(q) },
                onSearchSubmit = { q ->
                    vm.setSearchQuery(q)
                    navController.navigate(Routes.catalog(category = "All", query = q)) {
                        launchSingleTop = true
                    }
                },
                onCartClick = {
                    navController.navigate(Routes.CART) { launchSingleTop = true }
                },
                onLoginOrAccountClick = {
                    if (currentUser == null) {
                        navController.navigate(Routes.login()) { launchSingleTop = true }
                    } else {
                        navController.navigate(Routes.ACCOUNT) { launchSingleTop = true }
                    }
                },
                onNotificationsClick = {
                    vm.markNotificationsRead()
                    showNotificationsSheet = true
                }
            )
        },
        floatingActionButton = {
            WhatsAppFloatingButton(
                onClick = { IntentHelper.openWhatsApp(context) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentRoute == Routes.HOME,
                    onClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ZmGreen, selectedTextColor = ZmGreen)
                )
                NavigationBarItem(
                    selected = currentRoute.startsWith("catalog"),
                    onClick = {
                        vm.setSelectedCategory("All")
                        navController.navigate(Routes.catalog()) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.Store, contentDescription = "Shop") },
                    label = { Text("Shop") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ZmGreen, selectedTextColor = ZmGreen)
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.CART,
                    onClick = { navController.navigate(Routes.CART) { launchSingleTop = true } },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (cartCount > 0) {
                                    Badge(containerColor = ZmRed) { Text(cartCount.toString(), color = ZmWhite) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                        }
                    },
                    label = { Text("Cart") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ZmGreen, selectedTextColor = ZmGreen)
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.MY_ORDERS,
                    onClick = { navController.navigate(Routes.MY_ORDERS) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.LocalShipping, contentDescription = "Orders") },
                    label = { Text("Orders") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ZmGreen, selectedTextColor = ZmGreen)
                )
                NavigationBarItem(
                    selected = currentRoute == Routes.ACCOUNT,
                    onClick = { navController.navigate(Routes.ACCOUNT) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Account") },
                    label = { Text("Account") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ZmGreen, selectedTextColor = ZmGreen)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isCloudLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ZmGreen
                )
            }
            if (!cloudError.isNullOrBlank()) {
                Surface(color = ZmRedLight, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cloudError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZmRed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.clearCloudError() }) {
                            Text("Dismiss", color = ZmRed)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            banners = activeBanners,
                            categories = categories,
                            products = approvedProducts,
                            flashSaleSecondsLeft = flashSaleSecondsLeft,
                            adminPaymentAccounts = adminPaymentAccounts,
                            currentSeller = currentSeller,
                            onCategoryClick = { cat ->
                                vm.setSelectedCategory(cat)
                                navController.navigate(Routes.catalog(category = cat))
                            },
                            onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                            onAddToCart = { prod -> vm.addToCart(prod) },
                            onViewAllShopClick = {
                                vm.setSelectedCategory("All")
                                navController.navigate(Routes.catalog())
                            },
                            onBecomeSellerClick = { navController.navigate(Routes.BECOME_SELLER) },
                            onSellerDashboardClick = { navController.navigate(Routes.SELLER_DASHBOARD) }
                        )
                    }

                    composable(
                        route = Routes.CATALOG_WITH_CATEGORY,
                        arguments = listOf(
                            navArgument("category") { type = NavType.StringType; defaultValue = "All" },
                            navArgument("query") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { entry ->
                        val argCat = entry.arguments?.getString("category") ?: "All"
                        val argQuery = entry.arguments?.getString("query") ?: ""
                        LaunchedEffect(argCat, argQuery) {
                            if (argCat.isNotBlank()) vm.setSelectedCategory(argCat)
                            if (argQuery.isNotBlank()) vm.setSearchQuery(argQuery)
                        }
                        CatalogScreen(
                            products = filteredProducts,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            selectedPriceFilter = selectedPriceFilter,
                            selectedSort = selectedSort,
                            searchQuery = searchQuery,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onSelectCategory = { vm.setSelectedCategory(it) },
                            onSelectPriceFilter = { vm.setPriceFilter(it) },
                            onSelectSort = { vm.setSortOption(it) },
                            onClearFilters = {
                                vm.setSelectedCategory("All")
                                vm.setSearchQuery("")
                                vm.setPriceFilter(PriceFilter.ALL)
                                vm.setSortOption(SortOption.NEWEST)
                            },
                            onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                            onAddToCart = { prod -> vm.addToCart(prod) }
                        )
                    }

                    composable(
                        route = Routes.PRODUCT_DETAIL,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { entry ->
                        val productId = android.net.Uri.decode(entry.arguments?.getString("productId") ?: "")
                        val product by vm.observeProduct(productId).collectAsStateWithLifecycle(initialValue = null)
                        val related = remember(approvedProducts, product) {
                            if (product == null) emptyList()
                            else approvedProducts.filter { it.id != product!!.id && it.category == product!!.category }.take(6)
                        }
                        ProductDetailScreen(
                            product = product,
                            relatedProducts = related,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onAddToCart = { p, q, customName, customPhoto ->
                                vm.addToCart(p, q, customName, customPhoto)
                            },
                            onBuyNow = { p, q, customName, customPhoto ->
                                vm.addToCart(p, q, customName, customPhoto)
                                if (currentUser == null) {
                                    navController.navigate(Routes.login(redirect = Routes.CHECKOUT))
                                } else {
                                    navController.navigate(Routes.CHECKOUT)
                                }
                            },
                            onRelatedProductClick = { relId -> navController.navigate(Routes.productDetail(relId)) }
                        )
                    }

                    composable(Routes.CART) {
                        CartScreen(
                            cartItems = cartItems,
                            currentUser = currentUser,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onUpdateQuantity = { item, q -> vm.updateCartQuantity(item, q) },
                            onRemoveItem = { item -> vm.removeCartItem(item) },
                            onContinueShopping = { navController.navigate(Routes.catalog()) },
                            onProceedToCheckout = { navController.navigate(Routes.CHECKOUT) },
                            onRequireLogin = {
                                vm.emitMessage("Please sign in with Google first to proceed to Checkout.")
                                navController.navigate(Routes.login(redirect = Routes.CHECKOUT))
                            }
                        )
                    }

                    composable(Routes.CHECKOUT) {
                        CheckoutScreen(
                            currentUser = currentUser,
                            cartItems = cartItems,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onPlaceOrder = { name, phone, addr, city, note, method, info, shot, onSuccess, onError ->
                                vm.placeOrder(name, phone, addr, city, note, method, info, shot, onSuccess, onError)
                            },
                            onOrderPlaced = { order ->
                                navController.navigate(Routes.orderSuccess(order.orderCode)) {
                                    popUpTo(Routes.CART) { inclusive = true }
                                }
                            },
                            onRequireLogin = {
                                navController.navigate(Routes.login(redirect = Routes.CHECKOUT))
                            }
                        )
                    }

                    composable(
                        route = Routes.ORDER_SUCCESS,
                        arguments = listOf(navArgument("orderCode") { type = NavType.StringType })
                    ) { entry ->
                        val code = entry.arguments?.getString("orderCode") ?: ""
                        OrderSuccessScreen(
                            orderCode = code,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onTrackOrders = { navController.navigate(Routes.MY_ORDERS) },
                            onContinueShopping = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.MY_ORDERS) {
                        MyOrdersScreen(
                            currentUser = currentUser,
                            orders = myOrders,
                            allOrderItems = allOrderItems,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onLoginClick = { navController.navigate(Routes.login(redirect = Routes.MY_ORDERS)) },
                            onShopNowClick = { navController.navigate(Routes.catalog()) }
                        )
                    }

                    composable(
                        route = Routes.LOGIN,
                        arguments = listOf(navArgument("redirect") { type = NavType.StringType; defaultValue = "" })
                    ) { entry ->
                        val redirect = entry.arguments?.getString("redirect") ?: ""
                        LoginScreen(
                            adminPaymentAccounts = adminPaymentAccounts,
                            onLoginSuccess = {
                                if (redirect.isNotBlank()) {
                                    navController.navigate(redirect) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            },
                            onBecomeSellerClick = { navController.navigate(Routes.BECOME_SELLER) }
                        )
                    }

                    composable(Routes.ACCOUNT) {
                        AccountScreen(
                            currentUser = currentUser,
                            currentSeller = currentSeller,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onLoginClick = { navController.navigate(Routes.login(redirect = Routes.ACCOUNT)) },
                            onMyOrdersClick = { navController.navigate(Routes.MY_ORDERS) },
                            onBecomeSellerClick = { navController.navigate(Routes.BECOME_SELLER) },
                            onSellerDashboardClick = { navController.navigate(Routes.SELLER_DASHBOARD) },
                            onAdminPanelClick = { navController.navigate(Routes.ADMIN_PANEL) },
                            onUpdateProfile = { name, phone, city -> vm.updateUserProfile(name, phone, city) },
                            onLogoutComplete = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.BECOME_SELLER) {
                        BecomeSellerScreen(
                            currentUser = currentUser,
                            currentSeller = currentSeller,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onSubmitApplication = { shop, owner, phone, cnic, method, num, addr, onSuccess, onError ->
                                vm.submitSellerApplication(shop, owner, phone, cnic, method, num, addr, onSuccess, onError)
                            },
                            onLoginRequired = { navController.navigate(Routes.login(redirect = Routes.BECOME_SELLER)) },
                            onOpenSellerDashboard = { navController.navigate(Routes.SELLER_DASHBOARD) }
                        )
                    }

                    composable(Routes.SELLER_DASHBOARD) {
                        SellerDashboardScreen(
                            seller = currentSeller,
                            sellerProducts = sellerProducts,
                            sellerOrderItems = sellerOrderItems,
                            allOrders = allOrders,
                            categories = categories,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onSaveProduct = { vm.saveProduct(it) },
                            onDeleteProduct = { vm.deleteProduct(it) },
                            onUpdateOrderStatus = { orderId, status -> vm.updateOrderStatus(orderId, status) },
                            onChangePaymentAccount = { sId, m, num, title -> vm.changeSellerPaymentAccount(sId, m, num, title) },
                            onNavigateToBecomeSeller = { navController.navigate(Routes.BECOME_SELLER) }
                        )
                    }

                    composable(Routes.ADMIN_PANEL) {
                        AdminPanelScreen(
                            currentUser = currentUser,
                            orders = allOrders,
                            orderItems = allOrderItems,
                            sellers = allSellers,
                            products = allProducts,
                            categories = categories,
                            users = allUsers,
                            banners = allBanners,
                            adminPaymentAccounts = adminPaymentAccounts,
                            onUpdateOrderStatus = { orderId, status -> vm.updateOrderStatus(orderId, status) },
                            onApproveSeller = { vm.approveSeller(it) },
                            onRejectSeller = { vm.rejectSeller(it) },
                            onSendPaymentToSeller = { seller, amount -> vm.sendPaymentToSeller(seller, amount) },
                            onUpdateProductApproval = { pId, status -> vm.updateProductApproval(pId, status) },
                            onDeleteProduct = { vm.deleteProduct(it) },
                            onSaveCategory = { vm.saveCategory(it) },
                            onDeleteCategory = { vm.deleteCategory(it) },
                            onSaveBanner = { vm.saveBanner(it) },
                            onDeleteBanner = { vm.deleteBanner(it) },
                            onSaveAdminPaymentAccount = { vm.saveAdminPaymentAccount(it) },
                            onDeleteAdminPaymentAccount = { vm.deleteAdminPaymentAccount(it) }
                        )
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationsSheet = false },
            title = { Text("Notifications (${notifications.size})") },
            text = {
                if (notifications.isEmpty()) {
                    Text("No notifications yet.", color = ZmMutedText)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(notif.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(notif.message, style = MaterialTheme.typography.bodySmall)
                                    Text(IntentHelper.formatDateTime(notif.createdAt), style = MaterialTheme.typography.labelSmall, color = ZmMutedText)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showNotificationsSheet = false },
                    modifier = Modifier.testTag("close_notifications_dialog")
                ) { Text("Close") }
            }
        )
    }
}
