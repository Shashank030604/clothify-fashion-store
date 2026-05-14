package com.clothify;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/checkout")
public class CheckoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        int userId = (int) session.getAttribute("user_id");

        String paymentMethod = request.getParameter("payment_method");

        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            paymentMethod = "Cash on Delivery";
        }

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            double totalAmount = 0.0;

            String totalSql =
                    "SELECT SUM(p.price * c.quantity) AS total_amount " +
                    "FROM cart c " +
                    "JOIN products p ON c.product_id = p.product_id " +
                    "WHERE c.user_id = ?";

            PreparedStatement totalPs = con.prepareStatement(totalSql);
            totalPs.setInt(1, userId);

            ResultSet totalRs = totalPs.executeQuery();

            if (totalRs.next()) {
                totalAmount = totalRs.getDouble("total_amount");
            }

            if (totalAmount <= 0) {
                con.rollback();
                response.sendRedirect(request.getContextPath() + "/checkout-page");
                return;
            }

            String orderSql =
                    "INSERT INTO orders (user_id, total_amount, order_status, order_date) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement orderPs = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
            orderPs.setInt(1, userId);
            orderPs.setDouble(2, totalAmount);
            orderPs.setString(3, "Pending");
            orderPs.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            orderPs.executeUpdate();

            ResultSet orderKeys = orderPs.getGeneratedKeys();

            int orderId = 0;

            if (orderKeys.next()) {
                orderId = orderKeys.getInt(1);
            }

            if (orderId == 0) {
                throw new Exception("Order ID not generated");
            }

            String cartSql =
                    "SELECT c.product_id, c.variant_id, c.quantity, p.price " +
                    "FROM cart c " +
                    "JOIN products p ON c.product_id = p.product_id " +
                    "WHERE c.user_id = ?";

            PreparedStatement cartPs = con.prepareStatement(cartSql);
            cartPs.setInt(1, userId);
            ResultSet cartRs = cartPs.executeQuery();

            String itemSql =
                    "INSERT INTO order_items (order_id, product_id, variant_id, quantity, price) " +
                    "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement itemPs = con.prepareStatement(itemSql);

            while (cartRs.next()) {
                int productId = cartRs.getInt("product_id");
                int variantId = cartRs.getInt("variant_id");
                int quantity = cartRs.getInt("quantity");
                double price = cartRs.getDouble("price");

                itemPs.setInt(1, orderId);
                itemPs.setInt(2, productId);
                itemPs.setInt(3, variantId);
                itemPs.setInt(4, quantity);
                itemPs.setDouble(5, price);

                itemPs.addBatch();
            }

            itemPs.executeBatch();

            String paymentSql =
                    "INSERT INTO payments (order_id, payment_method, payment_status, payment_date) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement paymentPs = con.prepareStatement(paymentSql);
            paymentPs.setInt(1, orderId);
            paymentPs.setString(2, paymentMethod);
            paymentPs.setString(3, "Pending");
            paymentPs.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            paymentPs.executeUpdate();

            String clearCartSql = "DELETE FROM cart WHERE user_id = ?";
            PreparedStatement clearCartPs = con.prepareStatement(clearCartSql);
            clearCartPs.setInt(1, userId);
            clearCartPs.executeUpdate();

            con.commit();

            response.sendRedirect(request.getContextPath() + "/orders?success=1&orderId=" + orderId);

        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (Exception rollbackException) {
                rollbackException.printStackTrace();
            }

            response.setContentType("text/html");
            response.getWriter().println("<h1>Error placing order</h1>");
            response.getWriter().println("<p>" + e.getMessage() + "</p>");
            response.getWriter().println("<a href='checkout-page'>Back to Checkout</a>");

        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect(request.getContextPath() + "/checkout-page");
    }
}