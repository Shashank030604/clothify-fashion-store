package com.clothify;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class OrdersServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        int userId = (int) session.getAttribute("user_id");

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>My Orders - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + request.getContextPath() + "/css/style.css?v=92000'>");
        out.println("</head>");
        out.println("<body>");

        out.println(NavigationUtil.getNavbar(session));

        out.println("<section class='page-title'>");
        out.println("<h1>My Orders</h1>");
        out.println("<p>Same products are combined with increased quantity</p>");
        out.println("</section>");

        out.println("<section class='orders-section'>");

        try {
            Connection con = DBConnection.getConnection();

            String orderSql = "SELECT order_id, total_amount, order_status, order_date, payment_method "
                    + "FROM orders WHERE user_id = ? ORDER BY order_id DESC";

            PreparedStatement orderPs = con.prepareStatement(orderSql);
            orderPs.setInt(1, userId);

            ResultSet orderRs = orderPs.executeQuery();

            boolean hasOrders = false;

            while (orderRs.next()) {
                hasOrders = true;

                int orderId = orderRs.getInt("order_id");
                double totalAmount = orderRs.getDouble("total_amount");
                String orderStatus = orderRs.getString("order_status");
                String orderDate = String.valueOf(orderRs.getTimestamp("order_date"));
                String paymentMethod = orderRs.getString("payment_method");

                if (orderStatus == null || orderStatus.trim().isEmpty()) {
                    orderStatus = "Payment Pending";
                }

                String statusClass = "status-pending";

                if ("Paid".equalsIgnoreCase(orderStatus)) {
                    statusClass = "status-paid";
                } else if ("Payment Pending".equalsIgnoreCase(orderStatus) || "Pending".equalsIgnoreCase(orderStatus)) {
                    statusClass = "status-pending";
                }

                out.println("<div class='order-card'>");

                out.println("<div class='order-header'>");
                out.println("<div>");
                out.println("<h2>Order ID: #" + orderId + "</h2>");
                out.println("<p><b>Order Date:</b> " + orderDate + "</p>");
                out.println("<p><b>Total Amount:</b> ₹" + totalAmount + "</p>");

                if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
                    out.println("<p><b>Payment Method:</b> " + paymentMethod + "</p>");
                }

                out.println("</div>");
                out.println("<span class='" + statusClass + "'>" + orderStatus + "</span>");
                out.println("</div>");

                showOrderItems(out, con, orderId);

                out.println("</div>");
            }

            if (!hasOrders) {
                out.println("<div class='empty-state'>");
                out.println("<h2>No orders found</h2>");
                out.println("<p>You have not placed any orders yet.</p>");
                out.println("<a href='products'>Shop Now</a>");
                out.println("</div>");
            }

            orderRs.close();
            orderPs.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();

            out.println("<div class='empty-state'>");
            out.println("<h2>Error loading orders</h2>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("</div>");
        }

        out.println("</section>");

        out.println("</body>");
        out.println("</html>");
    }

    private void showOrderItems(PrintWriter out, Connection con, int orderId) {
        try {
            String itemSql = "SELECT oi.*, p.product_name, p.image_url "
                    + "FROM order_item oi "
                    + "JOIN products p ON oi.product_id = p.product_id "
                    + "WHERE oi.order_id = ?";

            PreparedStatement itemPs = con.prepareStatement(itemSql);
            itemPs.setInt(1, orderId);

            ResultSet itemRs = itemPs.executeQuery();

            while (itemRs.next()) {
                String productName = itemRs.getString("product_name");
                String imageUrl = itemRs.getString("image_url");
                String size = itemRs.getString("size");
                String color = itemRs.getString("color");
                int quantity = itemRs.getInt("quantity");
                double price = itemRs.getDouble("price");

                double itemTotal = price * quantity;

                out.println("<div class='order-item'>");

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    out.println("<img src='" + imageUrl + "' alt='" + productName + "'>");
                }

                out.println("<div class='order-item-details'>");
                out.println("<p><b>Product:</b> " + productName + "</p>");
                out.println("<p><b>Size:</b> " + size + "</p>");
                out.println("<p><b>Color:</b> " + color + "</p>");
                out.println("<p><b>Quantity:</b> " + quantity + "</p>");
                out.println("<p><b>Price:</b> ₹" + price + "</p>");
                out.println("<p><b>Item Total:</b> ₹" + itemTotal + "</p>");
                out.println("</div>");

                out.println("</div>");
            }

            itemRs.close();
            itemPs.close();

        } catch (Exception e) {
            e.printStackTrace();
            out.println("<p style='color:red;'>Error loading order items: " + e.getMessage() + "</p>");
        }
    }
}