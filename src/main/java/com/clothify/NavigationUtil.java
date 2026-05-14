package com.clothify;

import jakarta.servlet.http.HttpSession;

public class NavigationUtil {

    public static String getNavbar(HttpSession session) {

        boolean isLoggedIn = session != null && session.getAttribute("user_id") != null;

        String fullName = "User";
        String firstLetter = "U";

        if (isLoggedIn) {
            Object nameObj = session.getAttribute("full_name");

            if (nameObj != null && !nameObj.toString().trim().isEmpty()) {
                fullName = nameObj.toString();
                firstLetter = fullName.substring(0, 1).toUpperCase();
            }
        }

        StringBuilder nav = new StringBuilder();

        nav.append("<header class='main-header'>");

        nav.append("<div class='brand'>");
        nav.append("<span class='brand-symbol'>🛍️</span>");
        nav.append("<span class='brand-text'>Clothify</span>");
        nav.append("</div>");

        /*
         * Search bar:
         * When user types men / women / kids / unisex / tshirt / jeans / watch etc.
         * It sends the search keyword to ProductsServlet using q parameter.
         *
         * Example:
         * /products?q=men
         */
        nav.append("<form action='products' method='get' class='search-box'>");
        nav.append("<span class='search-symbol'>🔍</span>");
        nav.append("<input type='text' name='q' placeholder='Search for Products, Brands and More'>");
        nav.append("</form>");

        nav.append("<nav class='main-nav'>");

        nav.append("<a href='home'><span class='nav-symbol'>🏠</span> Home</a>");
        nav.append("<a href='products'><span class='nav-symbol'>🛍️</span> Products</a>");

        if (isLoggedIn) {
            nav.append("<a href='viewcart'><span class='nav-symbol'>🛒</span> Cart</a>");
            nav.append("<a href='checkout-page'><span class='nav-symbol'>💳</span> Checkout</a>");
            nav.append("<a href='orders'><span class='nav-symbol'>📦</span> My Orders</a>");

            nav.append("<a href='profile' class='profile-name-link' title='Profile'>");
            nav.append("<span class='profile-small-icon'>").append(firstLetter).append("</span>");
            nav.append("<span class='profile-user-name'>").append(fullName).append("</span>");
            nav.append("<span class='profile-arrow'>▼</span>");
            nav.append("</a>");

            nav.append("<a href='logout'><span class='nav-symbol'>↪</span> Logout</a>");
        } else {
            nav.append("<a href='login.html'><span class='nav-symbol'>🔐</span> Login</a>");
            nav.append("<a href='register.html'><span class='nav-symbol'>📝</span> Register</a>");
        }

        nav.append("</nav>");
        nav.append("</header>");

        return nav.toString();
    }
}