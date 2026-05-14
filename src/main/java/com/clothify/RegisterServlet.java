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

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String fullName = request.getParameter("full_name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        String city = request.getParameter("city");
        String state = request.getParameter("state");
        String pincode = request.getParameter("pincode");

        try {
            Connection con = DBConnection.getConnection();

            // Check email already exists
            String checkSql = "SELECT email FROM users WHERE email = ?";
            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setString(1, email);

            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head>");
                out.println("<meta charset='UTF-8'>");
                out.println("<title>Email Already Exists</title>");
                out.println("<link rel='stylesheet' href='css/style.css'>");
                out.println("</head>");
                out.println("<body>");

                out.println("<section class='page-title'>");
                out.println("<h1>Email Already Registered</h1>");
                out.println("<p>This email is already used. Please login or use another email.</p>");
                out.println("</section>");

                out.println("<div style='text-align:center; margin-top:30px;'>");
                out.println("<a href='login.html'><button style='padding:12px 25px; border:none; border-radius:25px; background:#ff3f6c; color:white; font-weight:bold;'>Login</button></a>");
                out.println("&nbsp;&nbsp;");
                out.println("<a href='register.html'><button style='padding:12px 25px; border:none; border-radius:25px; background:#111827; color:white; font-weight:bold;'>Back to Register</button></a>");
                out.println("</div>");

                out.println("</body>");
                out.println("</html>");
                return;
            }

            // Insert new user
            String sql = "INSERT INTO users (full_name, email, password, phone, address, city, state, pincode) "
                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, phone);
            ps.setString(5, address);
            ps.setString(6, city);
            ps.setString(7, state);
            ps.setString(8, pincode);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                response.sendRedirect("login.html");
            } else {
                out.println("<h1>Registration Failed</h1>");
                out.println("<a href='register.html'>Back to Register</a>");
            }

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
            out.println("<h1>Something went wrong</h1>");
            out.println("<p>Please try again later.</p>");
            out.println("</section>");

            out.println("<div style='text-align:center; margin-top:30px;'>");
            out.println("<a href='register.html'><button style='padding:12px 25px; border:none; border-radius:25px; background:#ff3f6c; color:white; font-weight:bold;'>Back to Register</button></a>");
            out.println("</div>");

            out.println("</body>");
            out.println("</html>");
        }
    }
}