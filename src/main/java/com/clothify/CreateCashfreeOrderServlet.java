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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CreateCashfreeOrderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String CLIENT_ID =
            System.getenv("CASHFREE_CLIENT_ID") == null ? "" : System.getenv("CASHFREE_CLIENT_ID").trim();

    private static final String CLIENT_SECRET =
            System.getenv("CASHFREE_CLIENT_SECRET") == null ? "" : System.getenv("CASHFREE_CLIENT_SECRET").trim();

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

        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect(request.getContextPath() + "/login.html");
            return;
        }

        int userId = (int) session.getAttribute("user_id");

        String amountValue = request.getParameter("amount");

        if (amountValue == null || amountValue.trim().isEmpty()) {
            showError(out, "Amount not found.");
            return;
        }

        amountValue = amountValue.trim();

        double amount;

        try {
            amount = Double.parseDouble(amountValue);
        } catch (Exception e) {
            showError(out, "Invalid amount.");
            return;
        }

        if (amount <= 0) {
            showError(out, "Invalid amount.");
            return;
        }

        if (CLIENT_ID.isEmpty() || CLIENT_SECRET.isEmpty()) {
            showError(out, "Cashfree API keys are missing in Render Environment Variables.");
            return;
        }

        String cashfreeOrderId = "CF_ORDER_" + System.currentTimeMillis();

        String customerName = "Clothify User";
        String customerEmail = "clothifyuser@gmail.com";
        String customerPhone = "9999999999";

        if (session.getAttribute("full_name") != null) {
            customerName = session.getAttribute("full_name").toString();
        }

        if (session.getAttribute("email") != null) {
            customerEmail = session.getAttribute("email").toString();
        }

        /*
         * This return URL works both locally and on Render.
         * On Render it becomes:
         * https://clothify-fashion-store.onrender.com/Clothify/cashfree-success?order_id={order_id}
         */
        String baseUrl = getBaseUrl(request);

        String returnUrl = baseUrl
                + request.getContextPath()
                + "/cashfree-success?order_id={order_id}";

        /*
         * IMPORTANT:
         * notify_url is removed because Cashfree rejected it.
         * Only return_url is used.
         */
        String jsonBody = "{"
                + "\"order_id\":\"" + escape(cashfreeOrderId) + "\","
                + "\"order_amount\":" + amount + ","
                + "\"order_currency\":\"INR\","
                + "\"customer_details\":{"
                + "\"customer_id\":\"USER_" + userId + "\","
                + "\"customer_name\":\"" + escape(customerName) + "\","
                + "\"customer_email\":\"" + escape(customerEmail) + "\","
                + "\"customer_phone\":\"" + escape(customerPhone) + "\""
                + "},"
                + "\"order_meta\":{"
                + "\"return_url\":\"" + escape(returnUrl) + "\""
                + "}"
                + "}";

        try {
            URL url = new URL(CASHFREE_ORDER_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
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

            if (responseCode >= 200 && responseCode < 300) {

                String paymentSessionId = extractValue(responseText, "payment_session_id");

                if (paymentSessionId == null || paymentSessionId.trim().isEmpty()) {
                    showError(out, "Payment session ID not received from Cashfree.");
                    return;
                }

                savePayment(userId, cashfreeOrderId, paymentSessionId, amount);

                showCashfreeCheckoutPage(out, cashfreeOrderId, paymentSessionId, amount);

            } else {
                showError(out, "Cashfree order creation failed: " + responseText);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError(out, e.getMessage());
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        String host = request.getHeader("X-Forwarded-Host");

        if (proto != null && host != null) {
            return proto + "://" + host;
        }

        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();

        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            return scheme + "://" + serverName;
        }

        return scheme + "://" + serverName + ":" + port;
    }

    private void savePayment(int userId, String cashfreeOrderId, String paymentSessionId, double amount) {
        try {
            Connection con = DBConnection.getConnection();

            String sql = "INSERT INTO payments "
                    + "(user_id, cashfree_order_id, payment_session_id, amount, payment_status) "
                    + "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, cashfreeOrderId);
            ps.setString(3, paymentSessionId);
            ps.setDouble(4, amount);
            ps.setString(5, "PENDING");

            ps.executeUpdate();

            ps.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractValue(String json, String key) {
        try {
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

        } catch (Exception e) {
            return "";
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "")
                .replace("\r", "")
                .trim();
    }

    private void showCashfreeCheckoutPage(PrintWriter out, String cashfreeOrderId, String paymentSessionId, double amount) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Cashfree Payment - Clothify</title>");
        out.println("<script src='https://sdk.cashfree.com/js/v3/cashfree.js'></script>");

        out.println("<style>");
        out.println("body{margin:0;font-family:Arial;background:linear-gradient(135deg,#f8f7ff,#e8f8ff);min-height:100vh;display:flex;align-items:center;justify-content:center;}");
        out.println(".card{width:90%;max-width:560px;background:white;border-radius:24px;padding:45px 30px;text-align:center;box-shadow:0 15px 35px rgba(0,0,0,0.15);}");
        out.println(".icon{font-size:70px;margin-bottom:15px;}");
        out.println("h1{color:#991b1b;font-size:32px;margin-bottom:12px;}");
        out.println("p{font-size:18px;color:#444;}");
        out.println(".btn{border:none;background:#111827;color:white;padding:14px 42px;border-radius:30px;font-size:18px;font-weight:bold;cursor:pointer;margin:10px;}");
        out.println(".back{display:inline-block;text-decoration:none;background:#ff3f6c;color:white;padding:14px 42px;border-radius:30px;font-size:18px;font-weight:bold;margin-top:10px;}");
        out.println("</style>");

        out.println("</head>");
        out.println("<body>");

        out.println("<div class='card'>");
        out.println("<div class='icon'>💳</div>");
        out.println("<h1>Cashfree Order Created</h1>");
        out.println("<p><b>Order ID:</b> " + cashfreeOrderId + "</p>");
        out.println("<p><b>Amount:</b> ₹" + amount + "</p>");
        out.println("<p>Click below to continue payment.</p>");
        out.println("<button class='btn' onclick='payNow()'>Pay Now</button>");
        out.println("<br>");
        out.println("<a class='back' href='checkout-page'>Back to Checkout</a>");
        out.println("</div>");

        out.println("<script>");
        out.println("const cashfree = Cashfree({ mode: 'sandbox' });");
        out.println("function payNow(){");
        out.println("cashfree.checkout({");
        out.println("paymentSessionId: '" + paymentSessionId + "',");
        out.println("redirectTarget: '_self'");
        out.println("});");
        out.println("}");
        out.println("</script>");

        out.println("</body>");
        out.println("</html>");
    }

    private void showError(PrintWriter out, String message) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Cashfree Error - Clothify</title>");

        out.println("<style>");
        out.println("body{margin:0;font-family:Arial;background:linear-gradient(135deg,#f8f7ff,#e8f8ff);min-height:100vh;display:flex;align-items:center;justify-content:center;}");
        out.println(".card{width:90%;max-width:560px;background:white;border-radius:24px;padding:45px 30px;text-align:center;box-shadow:0 15px 35px rgba(0,0,0,0.15);}");
        out.println(".icon{font-size:70px;margin-bottom:15px;}");
        out.println("h1{color:#991b1b;font-size:34px;margin-bottom:12px;}");
        out.println("p{font-size:18px;color:#444;word-break:break-word;}");
        out.println("a{display:inline-block;text-decoration:none;background:#ff3f6c;color:white;padding:14px 42px;border-radius:30px;font-size:18px;font-weight:bold;margin-top:20px;}");
        out.println("</style>");

        out.println("</head>");
        out.println("<body>");
        out.println("<div class='card'>");
        out.println("<div class='icon'>⚠️</div>");
        out.println("<h1>Payment Error</h1>");
        out.println("<p>Error: " + escape(message) + "</p>");
        out.println("<a href='checkout-page'>Back to Checkout</a>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
}