package com.clothify;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/checkout-page")
public class CheckoutPageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private String safe(String value) {
        return value == null ? "" : value;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String contextPath = request.getContextPath();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect(contextPath + "/login.html");
            return;
        }

        int userId = (int) session.getAttribute("user_id");

        try {
            Connection con = DBConnection.getConnection();

            String userSql = "SELECT full_name, email, phone, address, city, state, pincode FROM users WHERE user_id = ?";
            PreparedStatement userPs = con.prepareStatement(userSql);
            userPs.setInt(1, userId);
            ResultSet userRs = userPs.executeQuery();

            String fullName = "";
            String email = "";
            String phone = "";
            String address = "";
            String city = "";
            String state = "";
            String pincode = "";

            if (userRs.next()) {
                fullName = safe(userRs.getString("full_name"));
                email = safe(userRs.getString("email"));
                phone = safe(userRs.getString("phone"));
                address = safe(userRs.getString("address"));
                city = safe(userRs.getString("city"));
                state = safe(userRs.getString("state"));
                pincode = safe(userRs.getString("pincode"));
            }

            String cartSql =
                    "SELECT c.cart_id, c.product_id, c.variant_id, c.quantity, " +
                    "p.product_name, p.price, p.image_url, " +
                    "v.size, v.color " +
                    "FROM cart c " +
                    "JOIN products p ON c.product_id = p.product_id " +
                    "JOIN product_variants v ON c.variant_id = v.variant_id " +
                    "WHERE c.user_id = ?";

            PreparedStatement cartPs = con.prepareStatement(cartSql);
            cartPs.setInt(1, userId);
            ResultSet cartRs = cartPs.executeQuery();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Checkout - Clothify</title>");
            out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=42000'>");
            out.println("</head>");
            out.println("<body>");

            out.println(NavigationUtil.getNavbar(session));

            out.println("<section class='page-title'>");
            out.println("<h1>Checkout</h1>");
            out.println("<p>Complete your order details</p>");
            out.println("</section>");

            boolean hasCartItems = false;
            double totalAmount = 0.0;

            StringBuilder orderSummary = new StringBuilder();

            while (cartRs.next()) {
                hasCartItems = true;

                String productName = safe(cartRs.getString("product_name"));
                String size = safe(cartRs.getString("size"));
                String color = safe(cartRs.getString("color"));
                int quantity = cartRs.getInt("quantity");
                double price = cartRs.getDouble("price");
                double itemTotal = price * quantity;

                totalAmount += itemTotal;

                String imageUrl = cartRs.getString("image_url");

                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    imageUrl = contextPath + "/images/cat-tshirt.jpg";
                } else if (!imageUrl.startsWith("http") && !imageUrl.startsWith(contextPath)) {
                    imageUrl = contextPath + "/" + imageUrl;
                }

                orderSummary.append("<div class='order-summary-item order-summary-with-img'>");
                orderSummary.append("<img class='checkout-product-img' src='").append(imageUrl).append("' alt='").append(productName).append("'>");
                orderSummary.append("<div>");
                orderSummary.append("<h3>").append(productName).append("</h3>");
                orderSummary.append("<p><b>Size:</b> ").append(size).append("</p>");
                orderSummary.append("<p><b>Color:</b> ").append(color).append("</p>");
                orderSummary.append("<p><b>Quantity:</b> ").append(quantity).append("</p>");
                orderSummary.append("<p><b>Price:</b> ₹").append(price).append("</p>");
                orderSummary.append("<p><b>Item Total:</b> ₹").append(itemTotal).append("</p>");
                orderSummary.append("</div>");
                orderSummary.append("</div>");
            }

            if (!hasCartItems) {
                out.println("<div class='checkout-empty-wrapper'>");
                out.println("<div class='checkout-empty-card'>");
                out.println("<div class='checkout-empty-icon'>🛒</div>");
                out.println("<h2>Your checkout page is empty</h2>");
                out.println("<p>Add some products to your cart before checkout.</p>");
                out.println("<a href='products' class='checkout-empty-btn'>Shop Now</a>");
                out.println("</div>");
                out.println("</div>");
                out.println("</body>");
                out.println("</html>");

                cartRs.close();
                cartPs.close();
                userRs.close();
                userPs.close();
                con.close();
                return;
            }

            out.println("<form action='checkout' method='post'>");

            out.println("<section class='checkout-main-wrapper'>");

            out.println("<div class='checkout-main-card'>");
            out.println("<h2>Shipping Details</h2>");

            out.println("<label>Full Name</label>");
            out.println("<input type='text' name='full_name' value='" + fullName + "' required>");

            out.println("<label>Email</label>");
            out.println("<input type='email' name='email' value='" + email + "' required>");

            out.println("<label>Phone</label>");
            out.println("<input type='text' name='phone' value='" + phone + "' required>");

            out.println("<label>Address</label>");
            out.println("<textarea name='address' required>" + address + "</textarea>");

            out.println("<label>City</label>");
            out.println("<input type='text' name='city' value='" + city + "' required>");

            out.println("<label>State</label>");
            out.println("<input type='text' name='state' value='" + state + "' required>");

            out.println("<label>Pincode</label>");
            out.println("<input type='text' name='pincode' value='" + pincode + "' required>");

            out.println("</div>");

            out.println("<div class='checkout-main-card'>");
            out.println("<h2>Order Summary</h2>");
            out.println(orderSummary.toString());
            out.println("<div class='checkout-total'>Total: ₹" + totalAmount + "</div>");

            out.println("<div class='payment-method-box'>");
            out.println("<h3>Payment Method</h3>");

            out.println("<label class='payment-option'>");
            out.println("<input type='radio' name='payment_method' value='Cash on Delivery' checked>");
            out.println("<span>💵 Cash on Delivery</span>");
            out.println("</label>");

            out.println("<div style='margin-top:15px; color:#555; line-height:1.8; font-size:17px;'>");
            out.println("<p>📱 UPI payment available in Cashfree</p>");
            out.println("<p>💳 Debit Card payment available in Cashfree</p>");
            out.println("<p>💳 Credit Card payment available in Cashfree</p>");
            out.println("<p>🏦 Net Banking available in Cashfree</p>");
            out.println("<p>👛 Wallet payment available in Cashfree</p>");
            out.println("</div>");

            out.println("</div>");

            out.println("<button type='submit' class='place-order-btn'>Place Order - Cash on Delivery</button>");

            out.println("<button type='button' class='place-order-btn' style='background:#111827; margin-top:15px;' onclick='submitCashfreePayment()'>Pay Online with Cashfree</button>");

            out.println("</div>");

            out.println("</section>");

            out.println("</form>");

            out.println("<script>");

            out.println("var cashfreeAmount = '" + totalAmount + "';");

            out.println("function submitCashfreePayment() {");
            out.println("  var form = document.createElement('form');");
            out.println("  form.method = 'post';");
            out.println("  form.action = 'create-cashfree-order';");

            out.println("  var amountInput = document.createElement('input');");
            out.println("  amountInput.type = 'hidden';");
            out.println("  amountInput.name = 'amount';");
            out.println("  amountInput.value = cashfreeAmount;");
            out.println("  form.appendChild(amountInput);");

            out.println("  document.body.appendChild(form);");
            out.println("  form.submit();");
            out.println("}");

            out.println("</script>");

            out.println("</body>");
            out.println("</html>");

            cartRs.close();
            cartPs.close();
            userRs.close();
            userPs.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();

            out.println("<h1>Error loading checkout page</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("<a href='viewcart'>Back to Cart</a>");
        }
    }
}