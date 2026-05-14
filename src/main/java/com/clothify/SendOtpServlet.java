package com.clothify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SendOtpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Paste your NEW 2Factor API key here
    private static final String API_KEY = "a66760a7-4e1c-11f1-9800-0200cd936042";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/mobile-login.html");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String contextPath = request.getContextPath();
        String phone = request.getParameter("phone");

        if (phone == null || phone.trim().isEmpty()) {
            showError(out, contextPath, "Please enter mobile number.");
            return;
        }

        phone = phone.trim();

        if (!phone.matches("[0-9]{10}")) {
            showError(out, contextPath, "Please enter valid 10 digit mobile number.");
            return;
        }

        Random random = new Random();
        String otp = String.valueOf(100000 + random.nextInt(900000));

        System.out.println("CURRENT CODE RUNNING: SMS ONLY");
        System.out.println("Generated OTP: " + otp);

        boolean smsSent = sendSmsOtp(phone, otp);

        if (smsSent) {
            HttpSession session = request.getSession();
            session.setAttribute("otp", otp);
            session.setAttribute("phone", phone);

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Verify OTP - Clothify</title>");
            out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=23000'>");
            out.println("</head>");
            out.println("<body>");

            out.println("<header class='main-header'>");

            out.println("<div class='brand'>");
            out.println("<span class='brand-symbol'>🛍️</span>");
            out.println("<span class='brand-text'>Clothify</span>");
            out.println("</div>");

            out.println("<form action='products' method='get' class='search-box'>");
            out.println("<span class='search-symbol'>🔍</span>");
            out.println("<input type='text' name='q' placeholder='Search for Products, Brands and More'>");
            out.println("</form>");

            out.println("<nav class='main-nav'>");
            out.println("<a href='home'><span class='nav-symbol'>🏠</span> Home</a>");
            out.println("<a href='products'><span class='nav-symbol'>🛍️</span> Products</a>");
            out.println("<a href='login.html'><span class='nav-symbol'>🔐</span> Login</a>");
            out.println("<a href='register.html'><span class='nav-symbol'>📝</span> Register</a>");
            out.println("</nav>");

            out.println("</header>");

            out.println("<section class='page-title'>");
            out.println("<h1>Verify OTP</h1>");
            out.println("<p>OTP sent by SMS message</p>");
            out.println("</section>");

            out.println("<section class='form-container'>");
            out.println("<div class='form-box'>");

            out.println("<h2>Enter OTP</h2>");
            out.println("<p style='text-align:center; margin-bottom:15px;'>OTP sent to: <b>" + phone + "</b></p>");
            out.println("<p class='demo-otp-box'>OTP sent by SMS message.</p>");

            out.println("<form action='verify-otp' method='post'>");
            out.println("<label>Enter OTP</label>");
            out.println("<input type='text' name='entered_otp' placeholder='Enter OTP' maxlength='6' required>");
            out.println("<button type='submit'>Verify OTP</button>");
            out.println("</form>");

            out.println("<p style='margin-top:15px; text-align:center;'>");
            out.println("<a href='mobile-login.html'>Change Number</a>");
            out.println("</p>");

            out.println("</div>");
            out.println("</section>");

            out.println("</body>");
            out.println("</html>");

        } else {
            showError(out, contextPath, "SMS OTP not sent. Check 2Factor SMS OTP balance, API key, or SMS OTP service activation.");
        }
    }

    private boolean sendSmsOtp(String phone, String otp) {
        String apiUrl = "https://2factor.in/API/V1/"
                + API_KEY
                + "/SMS/"
                + phone
                + "/"
                + otp;

        return call2FactorApi(apiUrl);
    }

    private boolean call2FactorApi(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");

            int responseCode = con.getResponseCode();

            BufferedReader br;

            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }

            String line;
            StringBuilder apiResponse = new StringBuilder();

            while ((line = br.readLine()) != null) {
                apiResponse.append(line);
            }

            br.close();

            String responseText = apiResponse.toString();

            System.out.println("2Factor SMS Response Code: " + responseCode);
            System.out.println("2Factor SMS Response: " + responseText);

            return responseCode == 200 && responseText.contains("\"Status\":\"Success\"");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showError(PrintWriter out, String contextPath, String message) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>OTP Error - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=23000'>");
        out.println("</head>");
        out.println("<body>");

        out.println("<div class='login-error-wrapper'>");
        out.println("<div class='login-error-card'>");
        out.println("<div class='login-error-icon'>⚠️</div>");
        out.println("<h1>OTP Not Sent</h1>");
        out.println("<p>" + message + "</p>");
        out.println("<a href='mobile-login.html'>Try Again</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("</body>");
        out.println("</html>");
    }
}