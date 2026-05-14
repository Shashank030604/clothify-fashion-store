package com.clothify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CashfreeSuccessServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Paste your Cashfree TEST credentials here
    private static final String CLIENT_ID = System.getenv("CASHFREE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("CASHFREE_CLIENT_SECRET");

    private static final String CASHFREE_ORDER_STATUS_URL = "https://sandbox.cashfree.com/pg/orders/";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String contextPath = request.getContextPath();

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect(contextPath + "/login.html");
            return;
        }

        int userId = Integer.parseInt(session.getAttribute("user_id").toString());

        String cashfreeOrderId = request.getParameter("order_id");
        String clothifyOrderIdParam = request.getParameter("clothify_order_id");

        if (cashfreeOrderId == null || cashfreeOrderId.trim().isEmpty()) {
            showError(out, contextPath, "Cashfree order ID not found.");
            return;
        }

        if (clothifyOrderIdParam == null || clothifyOrderIdParam.trim().isEmpty()) {
            showError(out, contextPath, "Clothify order ID not found.");
            return;
        }

        cashfreeOrderId = cashfreeOrderId.trim();
        int clothifyOrderId = Integer.parseInt(clothifyOrderIdParam.trim());

        try {
            String orderStatus = checkCashfreeOrderStatus(cashfreeOrderId);

            if ("PAID".equalsIgnoreCase(orderStatus)) {
                updatePaidOrder(userId, clothifyOrderId, cashfreeOrderId);
                showSuccess(out, contextPath, clothifyOrderId, cashfreeOrderId);
            } else {
                updateFailedOrder(clothifyOrderId, cashfreeOrderId, orderStatus);
                showPending(out, contextPath, clothifyOrderId, cashfreeOrderId, orderStatus);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError(out, contextPath, "Error checking payment status: " + e.getMessage());
        }
    }

    private String checkCashfreeOrderStatus(String cashfreeOrderId) throws Exception {
        URL url = new URL(CASHFREE_ORDER_STATUS_URL + cashfreeOrderId);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("x-client-id", CLIENT_ID);
        con.setRequestProperty("x-client-secret", CLIENT_SECRET);
        con.setRequestProperty("x-api-version", "2023-08-01");

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

        System.out.println("Cashfree Status Response Code: " + responseCode);
        System.out.println("Cashfree Status Response: " + responseText);

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Cashfree status check failed: " + responseText);
        }

        String status = extractValue(responseText, "order_status");

        if (status == null || status.isEmpty()) {
            status = "UNKNOWN";
        }

        return status;
    }

    private void updatePaidOrder(int userId, int clothifyOrderId, String cashfreeOrderId) {
        Connection con = null;

        try {
            con = DBConnection.getConnection();

            String orderSql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
            PreparedStatement orderPs = con.prepareStatement(orderSql);
            orderPs.setString(1, "Paid");
            orderPs.setInt(2, clothifyOrderId);
            orderPs.executeUpdate();
            orderPs.close();

            String paymentSql =
                    "UPDATE payments SET payment_status = ? " +
                    "WHERE order_id = ? AND cashfree_order_id = ?";

            PreparedStatement paymentPs = con.prepareStatement(paymentSql);
            paymentPs.setString(1, "PAID");
            paymentPs.setInt(2, clothifyOrderId);
            paymentPs.setString(3, cashfreeOrderId);
            paymentPs.executeUpdate();
            paymentPs.close();

            String clearCartSql = "DELETE FROM cart WHERE user_id = ?";
            PreparedStatement clearCartPs = con.prepareStatement(clearCartSql);
            clearCartPs.setInt(1, userId);
            clearCartPs.executeUpdate();
            clearCartPs.close();

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateFailedOrder(int clothifyOrderId, String cashfreeOrderId, String status) {
        Connection con = null;

        try {
            con = DBConnection.getConnection();

            String orderSql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
            PreparedStatement orderPs = con.prepareStatement(orderSql);
            orderPs.setString(1, "Payment " + status);
            orderPs.setInt(2, clothifyOrderId);
            orderPs.executeUpdate();
            orderPs.close();

            String paymentSql =
                    "UPDATE payments SET payment_status = ? " +
                    "WHERE order_id = ? AND cashfree_order_id = ?";

            PreparedStatement paymentPs = con.prepareStatement(paymentSql);
            paymentPs.setString(1, status);
            paymentPs.setInt(2, clothifyOrderId);
            paymentPs.setString(3, cashfreeOrderId);
            paymentPs.executeUpdate();
            paymentPs.close();

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);

        if (start == -1) {
            return "";
        }

        start = start + searchKey.length();
        int end = json.indexOf("\"", start);

        if (end == -1) {
            return "";
        }

        return json.substring(start, end);
    }

    private void showSuccess(PrintWriter out, String contextPath, int clothifyOrderId, String cashfreeOrderId) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Payment Success - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=36000'>");
        out.println("</head>");
        out.println("<body>");

        out.println("<div class='login-error-wrapper'>");
        out.println("<div class='login-error-card'>");
        out.println("<div class='login-error-icon'>✅</div>");
        out.println("<h1 style='color:#166534;'>Payment Successful</h1>");
        out.println("<p>Your Cashfree payment is completed successfully.</p>");
        out.println("<p><b>Clothify Order ID:</b> #" + clothifyOrderId + "</p>");
        out.println("<p><b>Cashfree Order ID:</b></p>");
        out.println("<p style='word-break:break-all;'>" + cashfreeOrderId + "</p>");
        out.println("<a href='orders?success=1&orderId=" + clothifyOrderId + "'>View My Orders</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("</body>");
        out.println("</html>");
    }

    private void showPending(PrintWriter out, String contextPath, int clothifyOrderId,
                             String cashfreeOrderId, String status) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Payment Status - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=36000'>");
        out.println("</head>");
        out.println("<body>");

        out.println("<div class='login-error-wrapper'>");
        out.println("<div class='login-error-card'>");
        out.println("<div class='login-error-icon'>⚠️</div>");
        out.println("<h1>Payment Status</h1>");
        out.println("<p>Your payment status is: <b>" + status + "</b></p>");
        out.println("<p><b>Clothify Order ID:</b> #" + clothifyOrderId + "</p>");
        out.println("<p><b>Cashfree Order ID:</b></p>");
        out.println("<p style='word-break:break-all;'>" + cashfreeOrderId + "</p>");
        out.println("<a href='orders'>View My Orders</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("</body>");
        out.println("</html>");
    }

    private void showError(PrintWriter out, String contextPath, String message) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Payment Error - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=36000'>");
        out.println("</head>");
        out.println("<body>");

        out.println("<div class='login-error-wrapper'>");
        out.println("<div class='login-error-card'>");
        out.println("<div class='login-error-icon'>⚠️</div>");
        out.println("<h1>Payment Error</h1>");
        out.println("<p>" + message + "</p>");
        out.println("<a href='checkout-page'>Back to Checkout</a>");
        out.println("</div>");
        out.println("</div>");

        out.println("</body>");
        out.println("</html>");
    }
}