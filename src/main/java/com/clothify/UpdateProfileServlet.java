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

@WebServlet("/updateprofile")
public class UpdateProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login.html");
            return;
        }

        int userId = (int) session.getAttribute("user_id");

        String fullName = request.getParameter("full_name");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        String city = request.getParameter("city");
        String state = request.getParameter("state");
        String pincode = request.getParameter("pincode");

        try {
            Connection con = DBConnection.getConnection();

            String sql = "UPDATE users SET full_name = ?, phone = ?, address = ?, city = ?, state = ?, pincode = ? WHERE user_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, address);
            ps.setString(4, city);
            ps.setString(5, state);
            ps.setString(6, pincode);
            ps.setInt(7, userId);

            ps.executeUpdate();

            session.setAttribute("full_name", fullName);

            response.sendRedirect("profile");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("<h1>Error: " + e.getMessage() + "</h1>");
            response.getWriter().println("<a href='profile'>Back to Profile</a>");
        }
    }
}