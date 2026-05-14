package com.clothify;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/addtocart")
public class AddToCartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // If user is not logged in, go directly to login page
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        int userId = (int) session.getAttribute("user_id");

        try {
            int productId = Integer.parseInt(request.getParameter("product_id"));
            int variantId = Integer.parseInt(request.getParameter("variant_id"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));

            if (quantity <= 0) {
                quantity = 1;
            }

            Connection con = DBConnection.getConnection();

            // Check if same product variant already exists in cart
            String checkSql =
                    "SELECT cart_id, quantity FROM cart " +
                    "WHERE user_id = ? AND product_id = ? AND variant_id = ?";

            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setInt(1, userId);
            checkPs.setInt(2, productId);
            checkPs.setInt(3, variantId);

            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                // If already exists, increase quantity
                int existingQuantity = rs.getInt("quantity");
                int newQuantity = existingQuantity + quantity;
                int cartId = rs.getInt("cart_id");

                String updateSql =
                        "UPDATE cart SET quantity = ? WHERE cart_id = ?";

                PreparedStatement updatePs = con.prepareStatement(updateSql);
                updatePs.setInt(1, newQuantity);
                updatePs.setInt(2, cartId);
                updatePs.executeUpdate();

            } else {
                // If not exists, insert new item
                String insertSql =
                        "INSERT INTO cart (user_id, product_id, variant_id, quantity) " +
                        "VALUES (?, ?, ?, ?)";

                PreparedStatement insertPs = con.prepareStatement(insertSql);
                insertPs.setInt(1, userId);
                insertPs.setInt(2, productId);
                insertPs.setInt(3, variantId);
                insertPs.setInt(4, quantity);
                insertPs.executeUpdate();
            }

            response.sendRedirect(request.getContextPath() + "/viewcart");

        } catch (Exception e) {
            e.printStackTrace();

            response.setContentType("text/html");
            response.getWriter().println("<h1>Error adding product to cart</h1>");
            response.getWriter().println("<p>" + e.getMessage() + "</p>");
            response.getWriter().println("<a href='products'>Back to Products</a>");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect(request.getContextPath() + "/products");
    }
}