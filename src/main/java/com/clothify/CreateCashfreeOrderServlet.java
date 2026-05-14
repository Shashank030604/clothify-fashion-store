package com.clothify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CreateCashfreeOrderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Paste your Cashfree TEST credentials here
    private static final String CLIENT_ID = System.getenv("CASHFREE_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("CASHFREE_CLIENT_SECRET");
    // Cashfree Sandbox URL
    private static final String CASHFREE_ORDER_URL = "https://sandbox.cashfree.com/pg/orders";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/checkout-page");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        String contextPath = request.getContextPath();

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect(contextPath + "/login.html");
            return;
        }

        int userId = Integer.parseInt(session.getAttribute("user_id").toString());

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            String fullName = "Clothify Customer";
            String email = "customer@example.com";
            String phone = "9999999999";

            String userSql = "SELECT full_name, email, phone FROM users WHERE user_id = ?";
            PreparedStatement userPs = con.prepareStatement(userSql);
            userPs.setInt(1, userId);
            ResultSet userRs = userPs.executeQuery();

            if (userRs.next()) {
                fullName = safe(userRs.getString("full_name"));
                email = safe(userRs.getString("email"));
                phone = safe(userRs.getString("phone"));
            }

            userRs.close();
            userPs.close();

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

            totalRs.close();
            totalPs.close();

            if (totalAmount <= 0) {
                con.rollback();
                response.sendRedirect(contextPath + "/checkout-page");
                return;
            }

            String orderSql =
                    "INSERT INTO orders (user_id, total_amount, order_status, order_date) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement orderPs = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
            orderPs.setInt(1, userId);
            orderPs.setDouble(2, totalAmount);
            orderPs.setString(3, "Payment Pending");
            orderPs.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            orderPs.executeUpdate();

            ResultSet orderKeys = orderPs.getGeneratedKeys();

            int clothifyOrderId = 0;

            if (orderKeys.next()) {
                clothifyOrderId = orderKeys.getInt(1);
            }

            orderKeys.close();
            orderPs.close();

            if (clothifyOrderId == 0) {
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

                itemPs.setInt(1, clothifyOrderId);
                itemPs.setInt(2, productId);
                itemPs.setInt(3, variantId);
                itemPs.setInt(4, quantity);
                itemPs.setDouble(5, price);

                itemPs.addBatch();
            }

            itemPs.executeBatch();

            cartRs.close();
            cartPs.close();
            itemPs.close();

            String cashfreeOrderId = "CF_ORDER_" + System.currentTimeMillis();

            String returnUrl = "http://localhost:8080" + contextPath
                    + "/cashfree-success?order_id={order_id}&clothify_order_id=" + clothifyOrderId;

            String jsonBody = "{"
                    + "\"order_id\":\"" + cashfreeOrderId + "\","
                    + "\"order_amount\":" + totalAmount + ","
                    + "\"order_currency\":\"INR\","
                    + "\"customer_details\":{"
                    + "\"customer_id\":\"USER_" + userId + "\","
                    + "\"customer_name\":\"" + escapeJson(fullName) + "\","
                    + "\"customer_email\":\"" + escapeJson(email) + "\","
                    + "\"customer_phone\":\"" + escapeJson(phone) + "\""
                    + "},"
                    + "\"order_meta\":{"
                    + "\"return_url\":\"" + returnUrl + "\""
                    + "}"
                    + "}";

            String responseText = callCashfreeCreateOrder(jsonBody);
            String paymentSessionId = extractValue(responseText, "payment_session_id");

            if (paymentSessionId == null || paymentSessionId.trim().isEmpty()) {
                throw new Exception("Payment Session ID not generated by Cashfree");
            }

            String paymentSql =
                    "INSERT INTO payments " +
                    "(user_id, order_id, payment_method, cashfree_order_id, payment_session_id, amount, payment_status, payment_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement paymentPs = con.prepareStatement(paymentSql);
            paymentPs.setInt(1, userId);
            paymentPs.setInt(2, clothifyOrderId);
            paymentPs.setString(3, "Cashfree");
            paymentPs.setString(4, cashfreeOrderId);
            paymentPs.setString(5, paymentSessionId);
            paymentPs.setDouble(6, totalAmount);
            paymentPs.setString(7, "CREATED");
            paymentPs.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            paymentPs.executeUpdate();
            paymentPs.close();

            con.commit();

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset='UTF-8'>");
            out.println("<title>Cashfree Payment - Clothify</title>");
            out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=35000'>");
            out.println("</head>");
            out.println("<body>");

            out.println("<div class='login-error-wrapper'>");
            out.println("<div class='login-error-card'>");
            out.println("<div class='login-error-icon'>💳</div>");
            out.println("<h1>Cashfree Order Created</h1>");
            out.println("<p><b>Clothify Order ID:</b> #" + clothifyOrderId + "</p>");
            out.println("<p><b>Cashfree Order ID:</b> " + cashfreeOrderId + "</p>");
            out.println("<p><b>Amount:</b> ₹" + totalAmount + "</p>");
            out.println("<p>Click below to continue payment.</p>");

            out.println("<button onclick='payNow()' style='background:#111827;color:white;padding:14px 35px;border:none;border-radius:30px;font-size:17px;font-weight:bold;cursor:pointer;'>Pay Now</button>");

            out.println("<p style='margin-top:18px;'>");
            out.println("<a href='checkout-page'>Back to Checkout</a>");
            out.println("</p>");

            out.println("</div>");
            out.println("</div>");

            out.println("<script src='https://sdk.cashfree.com/js/v3/cashfree.js'></script>");
            out.println("<script>");
            out.println("function payNow() {");
            out.println("    const cashfree = Cashfree({ mode: 'sandbox' });");
            out.println("    cashfree.checkout({");
            out.println("        paymentSessionId: '" + paymentSessionId + "',");
            out.println("        redirectTarget: '_self'");
            out.println("    });");
            out.println("}");
            out.println("</script>");

            out.println("</body>");
            out.println("</html>");

        } catch (Exception e) {
            e.printStackTrace();

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (Exception rollbackException) {
                rollbackException.printStackTrace();
            }

            showError(out, contextPath, "Error: " + e.getMessage());

        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String callCashfreeCreateOrder(String jsonBody) throws Exception {
        URL url = new URL(CASHFREE_ORDER_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("x-client-id", CLIENT_ID);
        con.setRequestProperty("x-client-secret", CLIENT_SECRET);
        con.setRequestProperty("x-api-version", "2023-08-01");
        con.setDoOutput(true);

        OutputStream os = con.getOutputStream();
        os.write(jsonBody.getBytes("UTF-8"));
        os.flush();
        os.close();

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

        System.out.println("Cashfree Response Code: " + responseCode);
        System.out.println("Cashfree Response: " + responseText);

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Cashfree order creation failed: " + responseText);
        }

        return responseText;
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void showError(PrintWriter out, String contextPath, String message) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Cashfree Error - Clothify</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css?v=35000'>");
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