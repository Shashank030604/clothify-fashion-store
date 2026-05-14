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

@WebServlet("/orders")
public class OrdersServlet extends HttpServlet {
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

        int userId = Integer.parseInt(session.getAttribute("user_id").toString());

        try {
            Connection con = DBConnection.getConnection();

            String orderSql =
                    "SELECT order_id, total_amount, order_status, order_date " +
                    "FROM orders " +
                    "WHERE user_id = ? " +
                    "ORDER BY order_id DESC";

            PreparedStatement orderPs = con.prepareStatement(orderSql);
            orderPs.setInt(1, userId);

            ResultSet orderRs = orderPs.executeQuery();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>My Orders - Clothify</title>");
            out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=37000'>");
            out.println("</head>");
            out.println("<body>");

            out.println(NavigationUtil.getNavbar(session));

            out.println("<section class='page-title'>");
            out.println("<h1>My Orders</h1>");
            out.println("<p>Same products are combined with increased quantity</p>");
            out.println("</section>");

            String success = request.getParameter("success");
            String orderIdParam = request.getParameter("orderId");

            if ("1".equals(success) && orderIdParam != null) {
                out.println("<div class='order-success-box'>");
                out.println("<h2>✅ Order Placed Successfully</h2>");
                out.println("<p><b>Order ID:</b> #" + orderIdParam + "</p>");
                out.println("</div>");
            }

            boolean hasOrders = false;

            out.println("<section class='orders-wrapper'>");

            while (orderRs.next()) {
                hasOrders = true;

                int orderId = orderRs.getInt("order_id");
                double totalAmount = orderRs.getDouble("total_amount");
                String orderStatus = safe(orderRs.getString("order_status"));
                String orderDate = String.valueOf(orderRs.getTimestamp("order_date"));

                String statusClass = "status-pending";

                if ("Paid".equalsIgnoreCase(orderStatus) || "PAID".equalsIgnoreCase(orderStatus)) {
                    statusClass = "status-paid";
                } else if ("Delivered".equalsIgnoreCase(orderStatus)) {
                    statusClass = "status-delivered";
                } else if ("Cancelled".equalsIgnoreCase(orderStatus)
                        || orderStatus.toLowerCase().contains("failed")) {
                    statusClass = "status-cancelled";
                } else if (orderStatus.toLowerCase().contains("pending")) {
                    statusClass = "status-pending";
                }

                out.println("<div class='order-card'>");

                out.println("<div class='order-header-row'>");

                out.println("<div>");
                out.println("<h2>Order ID: #" + orderId + "</h2>");
                out.println("<p><b>Order Date:</b> " + orderDate + "</p>");
                out.println("<p><b>Total Amount:</b> ₹" + totalAmount + "</p>");
                out.println("</div>");

                out.println("<span class='order-status " + statusClass + "'>" + orderStatus + "</span>");

                out.println("</div>");

                String itemSql =
                        "SELECT oi.product_id, oi.variant_id, SUM(oi.quantity) AS total_quantity, oi.price, " +
                        "p.product_name, p.image_url, v.size, v.color " +
                        "FROM order_items oi " +
                        "JOIN products p ON oi.product_id = p.product_id " +
                        "JOIN product_variants v ON oi.variant_id = v.variant_id " +
                        "WHERE oi.order_id = ? " +
                        "GROUP BY oi.product_id, oi.variant_id, oi.price, p.product_name, p.image_url, v.size, v.color";

                PreparedStatement itemPs = con.prepareStatement(itemSql);
                itemPs.setInt(1, orderId);

                ResultSet itemRs = itemPs.executeQuery();

                out.println("<div class='order-items-list'>");

                while (itemRs.next()) {
                    String productName = safe(itemRs.getString("product_name"));
                    String size = safe(itemRs.getString("size"));
                    String color = safe(itemRs.getString("color"));
                    int quantity = itemRs.getInt("total_quantity");
                    double price = itemRs.getDouble("price");
                    double itemTotal = price * quantity;

                    String imageUrl = itemRs.getString("image_url");

                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        imageUrl = contextPath + "/images/cat-tshirt.jpg";
                    } else if (!imageUrl.startsWith("http") && !imageUrl.startsWith(contextPath)) {
                        imageUrl = contextPath + "/" + imageUrl;
                    }

                    out.println("<div class='order-product-row'>");

                    out.println("<img class='order-product-img' src='" + imageUrl + "' alt='" + productName + "'>");

                    out.println("<div class='order-product-info'>");
                    out.println("<p><b>Product:</b> " + productName + "</p>");
                    out.println("<p><b>Size:</b> " + size + "</p>");
                    out.println("<p><b>Color:</b> " + color + "</p>");
                    out.println("<p><b>Quantity:</b> " + quantity + "</p>");
                    out.println("<p><b>Price:</b> ₹" + price + "</p>");
                    out.println("<p><b>Item Total:</b> ₹" + itemTotal + "</p>");
                    out.println("</div>");

                    out.println("</div>");
                }

                out.println("</div>");

                itemRs.close();
                itemPs.close();

                out.println("</div>");
            }

            out.println("</section>");

            if (!hasOrders) {
                out.println("<div class='checkout-empty-wrapper'>");
                out.println("<div class='checkout-empty-card'>");
                out.println("<div class='checkout-empty-icon'>📦</div>");
                out.println("<h2>No orders found</h2>");
                out.println("<p>You have not placed any orders yet.</p>");
                out.println("<a href='products' class='checkout-empty-btn'>Shop Now</a>");
                out.println("</div>");
                out.println("</div>");
            }

            out.println("</body>");
            out.println("</html>");

            orderRs.close();
            orderPs.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();

            out.println("<h1>Error loading orders</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("<a href='home'>Back to Home</a>");
        }
    }
}