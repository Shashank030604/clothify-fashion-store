package com.clothify;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/removecart")
public class RemoveCartServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.html");
            return;
        }

        int cartId = Integer.parseInt(request.getParameter("cart_id"));

        try {
            Connection con = DBConnection.getConnection();

            String sql = "DELETE FROM cart WHERE cart_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, cartId);

            ps.executeUpdate();

            response.sendRedirect("viewcart");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("<h1>Error: " + e.getMessage() + "</h1>");
            response.getWriter().println("<a href='viewcart'>Back to Cart</a>");
        }
    }
}