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

public class VerifyOtpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/mobile-login.html");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String contextPath = request.getContextPath();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("otp") == null || session.getAttribute("phone") == null) {
            response.sendRedirect(contextPath + "/mobile-login.html");
            return;
        }

        String originalOtp = session.getAttribute("otp").toString();
        String phone = session.getAttribute("phone").toString();
        String enteredOtp = request.getParameter("entered_otp");

        if (enteredOtp == null || enteredOtp.trim().isEmpty()) {
            showError(response, contextPath, "Please enter OTP.");
            return;
        }

        enteredOtp = enteredOtp.trim();

        if (originalOtp.equals(enteredOtp)) {
            try {
                Connection con = DBConnection.getConnection();

                String sql = "SELECT user_id, full_name, email FROM users WHERE phone = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, phone);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    session.setAttribute("user_id", rs.getInt("user_id"));
                    session.setAttribute("full_name", rs.getString("full_name"));
                    session.setAttribute("email", rs.getString("email"));

                    session.removeAttribute("otp");
                    session.removeAttribute("phone");

                    response.sendRedirect(contextPath + "/home");
                } else {
                    showError(response, contextPath, "Mobile number not registered.");
                }

                rs.close();
                ps.close();
                con.close();

            } catch (Exception e) {
                e.printStackTrace();
                showError(response, contextPath, "Database error: " + e.getMessage());
            }

        } else {
            showError(response, contextPath, "Invalid OTP. Please try again.");
        }
    }

    private void showError(HttpServletResponse response, String contextPath, String message) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>OTP Error - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=17000'>");
        out.println("</head>");
        out.println("<body>");

        out.println("<div class='login-error-wrapper'>");
        out.println("<div class='login-error-card'>");
        out.println("<div class='login-error-icon'>⚠️</div>");
        out.println("<h1>OTP Login Failed</h1>");
        out.println("<p>" + message + "</p>");
        out.println("<a href='mobile-login.html'>Try Again</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("</body>");
        out.println("</html>");
    }
}