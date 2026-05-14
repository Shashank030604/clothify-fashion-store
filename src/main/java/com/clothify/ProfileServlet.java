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

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
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

            String sql = "SELECT * FROM users WHERE user_id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String address = rs.getString("address");
                String city = rs.getString("city");
                String state = rs.getString("state");
                String pincode = rs.getString("pincode");

                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = "User";
                }

                if (email == null) email = "";
                if (phone == null) phone = "";
                if (address == null) address = "";
                if (city == null) city = "";
                if (state == null) state = "";
                if (pincode == null) pincode = "";

                String firstLetter = fullName.substring(0, 1).toUpperCase();

                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head>");
                out.println("<meta charset='UTF-8'>");
                out.println("<title>Profile - Clothify</title>");
                out.println("<link rel='stylesheet' href='css/style.css'>");

                out.println("<style>");
                out.println(".profile-page-custom {");
                out.println("    padding: 50px 20px;");
                out.println("    display: flex;");
                out.println("    justify-content: center;");
                out.println("}");

                out.println(".profile-card-custom {");
                out.println("    width: 520px;");
                out.println("    background: linear-gradient(135deg, #ffffff, #eef2ff);");
                out.println("    padding: 35px;");
                out.println("    border-radius: 22px;");
                out.println("    box-shadow: 0 10px 25px rgba(0,0,0,0.12);");
                out.println("}");

                out.println(".profile-photo-box-custom {");
                out.println("    text-align: center;");
                out.println("    margin-bottom: 25px;");
                out.println("}");

                out.println(".profile-icon-custom {");
                out.println("    width: 130px;");
                out.println("    height: 130px;");
                out.println("    border-radius: 50%;");
                out.println("    background: linear-gradient(135deg, #ff3f6c, #7c3aed);");
                out.println("    color: white;");
                out.println("    display: flex;");
                out.println("    align-items: center;");
                out.println("    justify-content: center;");
                out.println("    font-size: 58px;");
                out.println("    font-weight: bold;");
                out.println("    margin: 0 auto;");
                out.println("    border: 5px solid white;");
                out.println("    box-shadow: 0 6px 15px rgba(0,0,0,0.18);");
                out.println("}");

                out.println(".profile-name-custom {");
                out.println("    text-align: center;");
                out.println("    font-size: 28px;");
                out.println("    color: #111827;");
                out.println("    margin-top: 15px;");
                out.println("}");

                out.println(".profile-email-custom {");
                out.println("    text-align: center;");
                out.println("    color: #555;");
                out.println("    margin-top: 6px;");
                out.println("    margin-bottom: 25px;");
                out.println("}");

                out.println(".profile-field-custom {");
                out.println("    margin-bottom: 15px;");
                out.println("}");

                out.println(".profile-field-custom label {");
                out.println("    display: block;");
                out.println("    margin-bottom: 6px;");
                out.println("    font-weight: bold;");
                out.println("    color: #333;");
                out.println("}");

                out.println(".profile-field-custom input {");
                out.println("    width: 100%;");
                out.println("    padding: 12px;");
                out.println("    border: 1px solid #ccc;");
                out.println("    border-radius: 10px;");
                out.println("    font-size: 15px;");
                out.println("}");

                out.println(".profile-card-custom button {");
                out.println("    width: 100%;");
                out.println("    margin-top: 20px;");
                out.println("    padding: 13px;");
                out.println("    border: none;");
                out.println("    border-radius: 25px;");
                out.println("    background: linear-gradient(90deg, #ff3f6c, #fb7185);");
                out.println("    color: white;");
                out.println("    font-size: 16px;");
                out.println("    font-weight: bold;");
                out.println("    cursor: pointer;");
                out.println("}");

                out.println(".profile-card-custom button:hover {");
                out.println("    background: linear-gradient(90deg, #111827, #374151);");
                out.println("}");
                out.println("</style>");

                out.println("</head>");
                out.println("<body>");

                out.println(NavigationUtil.getNavbar(session));

                out.println("<section class='page-title'>");
                out.println("<h1>My Profile</h1>");
                out.println("<p>View and update your account details</p>");
                out.println("</section>");

                out.println("<section class='profile-page-custom'>");

                out.println("<form class='profile-card-custom' action='updateprofile' method='post'>");

                out.println("<div class='profile-photo-box-custom'>");
                out.println("<div class='profile-icon-custom'>" + firstLetter + "</div>");
                out.println("<h2 class='profile-name-custom'>" + fullName + "</h2>");
                out.println("<p class='profile-email-custom'>" + email + "</p>");
                out.println("</div>");

                out.println("<div class='profile-field-custom'>");
                out.println("<label>Full Name</label>");
                out.println("<input type='text' name='full_name' value='" + fullName + "' required>");
                out.println("</div>");

                out.println("<div class='profile-field-custom'>");
                out.println("<label>Email</label>");
                out.println("<input type='email' name='email' value='" + email + "' readonly>");
                out.println("</div>");

                out.println("<div class='profile-field-custom'>");
                out.println("<label>Phone</label>");
                out.println("<input type='text' name='phone' value='" + phone + "' required>");
                out.println("</div>");

                out.println("<div class='profile-field-custom'>");
                out.println("<label>Address</label>");
                out.println("<input type='text' name='address' value='" + address + "' required>");
                out.println("</div>");

                out.println("<div class='profile-field-custom'>");
                out.println("<label>City</label>");
                out.println("<input type='text' name='city' value='" + city + "' required>");
                out.println("</div>");

                out.println("<div class='profile-field-custom'>");
                out.println("<label>State</label>");
                out.println("<input type='text' name='state' value='" + state + "' required>");
                out.println("</div>");

                out.println("<div class='profile-field-custom'>");
                out.println("<label>Pincode</label>");
                out.println("<input type='text' name='pincode' value='" + pincode + "' required>");
                out.println("</div>");

                out.println("<button type='submit'>Update Profile</button>");

                out.println("</form>");

                out.println("</section>");

                out.println("</body>");
                out.println("</html>");

            } else {
                out.println("<h1>User not found</h1>");
                out.println("<a href='home'>Back to Home</a>");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("<h1>Error: " + e.getMessage() + "</h1>");
            out.println("<a href='home'>Back to Home</a>");
        }
    }
}