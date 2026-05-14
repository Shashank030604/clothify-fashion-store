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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String contextPath = request.getContextPath();

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT user_id, full_name, email FROM users WHERE email = ? AND password = ?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HttpSession session = request.getSession();

                session.setAttribute("user_id", rs.getInt("user_id"));
                session.setAttribute("full_name", rs.getString("full_name"));
                session.setAttribute("email", rs.getString("email"));

                response.sendRedirect(contextPath + "/home");
            } else {
                showLoginError(out, contextPath);
            }

        } catch (Exception e) {
            e.printStackTrace();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Login Error - Clothify</title>");
            out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=5000'>");
            out.println("</head>");
            out.println("<body>");

            out.println("<div class='login-error-wrapper'>");
            out.println("<div class='login-error-card'>");
            out.println("<div class='login-error-icon'>⚠️</div>");
            out.println("<h1>Login Error</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
            out.println("<a href='login.html'>Try Again</a>");
            out.println("</div>");
            out.println("</div>");

            out.println("</body>");
            out.println("</html>");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.sendRedirect(request.getContextPath() + "/login.html");
    }

    private void showLoginError(PrintWriter out, String contextPath) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Login Failed - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=5000'>");
        out.println("</head>");
        out.println("<body>");

        out.println("<div class='login-error-wrapper'>");
        out.println("<div class='login-error-card'>");
        out.println("<div class='login-error-icon'>⚠️</div>");
        out.println("<h1>Incorrect Password</h1>");
        out.println("<p>Please try again.</p>");
        out.println("<a href='login.html'>Try Again</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("</body>");
        out.println("</html>");
    }
}