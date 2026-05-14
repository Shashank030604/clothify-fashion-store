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

@WebServlet("/viewcart")
public class ViewCartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.html");
            return;
        }

        int userId = (int) session.getAttribute("user_id");

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT c.cart_id, c.quantity, p.product_name, p.price, v.size, v.color " +
                         "FROM cart c " +
                         "JOIN products p ON c.product_id = p.product_id " +
                         "JOIN product_variants v ON c.variant_id = v.variant_id " +
                         "WHERE c.user_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>My Cart - Clothify</title>");
            out.println("<link rel='stylesheet' href='css/style.css'>");

            /* Inline CSS for empty cart */
            out.println("<style>");
            out.println(".empty-cart-center-box {");
            out.println("    width: 80%;");
            out.println("    max-width: 900px;");
            out.println("    min-height: 380px;");
            out.println("    margin: 40px auto;");
            out.println("    background: linear-gradient(135deg, #ffffff, #f8fafc);");
            out.println("    border-radius: 25px;");
            out.println("    box-shadow: 0 10px 30px rgba(0,0,0,0.15);");
            out.println("    display: flex;");
            out.println("    flex-direction: column;");
            out.println("    justify-content: center;");
            out.println("    align-items: center;");
            out.println("    text-align: center;");
            out.println("    padding: 50px 20px;");
            out.println("}");

            out.println(".empty-cart-center-icon {");
            out.println("    font-size: 120px;");
            out.println("    margin-bottom: 25px;");
            out.println("    line-height: 1;");
            out.println("}");

            out.println(".empty-cart-center-box h2 {");
            out.println("    font-size: 40px;");
            out.println("    color: #111827;");
            out.println("    margin-bottom: 15px;");
            out.println("}");

            out.println(".empty-cart-center-box p {");
            out.println("    font-size: 20px;");
            out.println("    color: #555;");
            out.println("    margin-bottom: 30px;");
            out.println("}");

            out.println(".empty-cart-center-btn {");
            out.println("    text-decoration: none;");
            out.println("    background: linear-gradient(90deg, #ff3f6c, #fb7185);");
            out.println("    color: white;");
            out.println("    padding: 15px 45px;");
            out.println("    border-radius: 30px;");
            out.println("    font-size: 18px;");
            out.println("    font-weight: bold;");
            out.println("    display: inline-block;");
            out.println("}");

            out.println(".empty-cart-center-btn:hover {");
            out.println("    background: linear-gradient(90deg, #111827, #374151);");
            out.println("}");

            out.println(".cart-container-empty {");
            out.println("    padding: 50px 20px;");
            out.println("    min-height: 520px;");
            out.println("    display: flex;");
            out.println("    justify-content: center;");
            out.println("    align-items: center;");
            out.println("}");
            out.println("</style>");

            out.println("</head>");
            out.println("<body>");

            out.println(NavigationUtil.getNavbar(session));

            out.println("<section class='page-title'>");
            out.println("<h1>My Cart</h1>");
            out.println("<p>Items added to your shopping cart</p>");
            out.println("</section>");

            boolean hasItems = false;
            double grandTotal = 0;

            StringBuilder cartItems = new StringBuilder();

            while (rs.next()) {
                hasItems = true;

                double price = rs.getDouble("price");
                int quantity = rs.getInt("quantity");
                double total = price * quantity;
                grandTotal += total;

                cartItems.append("<div class='cart-item'>");

                cartItems.append("<div class='cart-details'>");
                cartItems.append("<h3>").append(rs.getString("product_name")).append("</h3>");
                cartItems.append("<p><b>Size:</b> ").append(rs.getString("size")).append("</p>");
                cartItems.append("<p><b>Color:</b> ").append(rs.getString("color")).append("</p>");
                cartItems.append("<p><b>Price:</b> ₹").append(price).append("</p>");
                cartItems.append("<p><b>Quantity:</b> ").append(quantity).append("</p>");
                cartItems.append("</div>");

                cartItems.append("<div class='cart-total'>");
                cartItems.append("<h3>₹").append(total).append("</h3>");
                cartItems.append("<form action='removecart' method='post'>");
                cartItems.append("<input type='hidden' name='cart_id' value='").append(rs.getInt("cart_id")).append("'>");
                cartItems.append("<button type='submit'>Remove</button>");
                cartItems.append("</form>");
                cartItems.append("</div>");

                cartItems.append("</div>");
            }

            if (hasItems) {
                out.println("<section class='cart-container'>");

                out.println(cartItems.toString());

                out.println("<div class='cart-summary'>");
                out.println("<h2>Cart Summary</h2>");
                out.println("<p><b>Total Amount:</b> ₹" + grandTotal + "</p>");
                out.println("<a href='checkout-page'><button>Proceed to Checkout</button></a>");
                out.println("</div>");

                out.println("</section>");
            } else {
                out.println("<section class='cart-container-empty'>");

                out.println("<div class='empty-cart-center-box'>");
                out.println("<div class='empty-cart-center-icon'>🛒</div>");
                out.println("<h2>Your cart is empty!</h2>");
                out.println("<p>Looks like you have not added anything to your cart yet.</p>");
                out.println("<a href='products' class='empty-cart-center-btn'>Shop Now</a>");
                out.println("</div>");

                out.println("</section>");
            }

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {
            e.printStackTrace();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Error</title>");
            out.println("<link rel='stylesheet' href='css/style.css'>");
            out.println("</head>");
            out.println("<body>");
            out.println("<section class='page-title'>");
            out.println("<h1>Error</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</section>");
            out.println("<div style='text-align:center;'>");
            out.println("<a href='home'>Back to Home</a>");
            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}