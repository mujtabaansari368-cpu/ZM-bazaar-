package com.example.ui.navigation

object Routes {
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val CATALOG_WITH_CATEGORY = "catalog?category={category}&query={query}"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val ORDER_SUCCESS = "order_success/{orderCode}"
    const val MY_ORDERS = "my_orders"
    const val LOGIN = "login?redirect={redirect}"
    const val SIGNUP = "signup"
    const val FORGOT_PASSWORD = "forgot_password"
    const val ACCOUNT = "account"
    const val BECOME_SELLER = "become_seller"
    const val SELLER_DASHBOARD = "seller_dashboard"
    const val ADMIN_PANEL = "admin_panel"

    fun catalog(category: String = "All", query: String = ""): String {
        return "catalog?category=${android.net.Uri.encode(category)}&query=${android.net.Uri.encode(query)}"
    }

    fun productDetail(productId: String): String = "product/${android.net.Uri.encode(productId)}"
    fun orderSuccess(orderCode: String): String = "order_success/${android.net.Uri.encode(orderCode)}"
    fun login(redirect: String = ""): String = "login?redirect=${android.net.Uri.encode(redirect)}"
}
