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

public class CashfreeSuccessServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String cashfreeOrderId = request.getParameter("order_id");
        String clothifyOrderIdParam = request.getParameter("clothify_order_id");

        if (cashfreeOrderId == null || cashfreeOrderId.trim().isEmpty()) {
            showError(out, "Cashfree order ID not found.");
            return;
        }

        cashfreeOrderId = cashfreeOrderId.trim();

        try {
            Connection con = DBConnection.getConnection();

            int clothifyOrderId = 0;

            /*
             * First try to get Clothify order ID from URL.
             * Example:
             * /cashfree-success?order_id=CF_ORDER_123&clothify_order_id=62
             */
            if (clothifyOrderIdParam != null && !clothifyOrderIdParam.trim().isEmpty()) {
                try {
                    clothifyOrderId = Integer.parseInt(clothifyOrderIdParam.trim());
                } catch (Exception e) {
                    clothifyOrderId = 0;
                }
            }

            /*
             * If URL does not contain Clothify order ID,
             * get user_id from payments table using Cashfree order ID,
             * then update user's latest order.
             */
            if (clothifyOrderId == 0) {
                int userId = 0;

                String getUserSql = "SELECT user_id FROM payments WHERE cashfree_order_id = ? ORDER BY payment_id DESC LIMIT 1";
                PreparedStatement getUserPs = con.prepareStatement(getUserSql);
                getUserPs.setString(1, cashfreeOrderId);

                ResultSet userRs = getUserPs.executeQuery();

                if (userRs.next()) {
                    userId = userRs.getInt("user_id");
                }

                userRs.close();
                getUserPs.close();

                if (userId > 0) {
                    String getOrderSql = "SELECT order_id FROM orders WHERE user_id = ? ORDER BY order_id DESC LIMIT 1";
                    PreparedStatement getOrderPs = con.prepareStatement(getOrderSql);
                    getOrderPs.setInt(1, userId);

                    ResultSet orderRs = getOrderPs.executeQuery();

                    if (orderRs.next()) {
                        clothifyOrderId = orderRs.getInt("order_id");
                    }

                    orderRs.close();
                    getOrderPs.close();
                }
            }

            /*
             * Update payment status as PAID.
             */
            String updatePaymentSql = "UPDATE payments SET payment_status = ? WHERE cashfree_order_id = ?";
            PreparedStatement updatePaymentPs = con.prepareStatement(updatePaymentSql);
            updatePaymentPs.setString(1, "PAID");
            updatePaymentPs.setString(2, cashfreeOrderId);
            updatePaymentPs.executeUpdate();
            updatePaymentPs.close();

            /*
             * Your orders table column name is order_status.
             * So every successful Cashfree payment becomes Paid.
             */
            if (clothifyOrderId > 0) {
                String updateOrderSql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
                PreparedStatement updateOrderPs = con.prepareStatement(updateOrderSql);
                updateOrderPs.setString(1, "Paid");
                updateOrderPs.setInt(2, clothifyOrderId);
                updateOrderPs.executeUpdate();
                updateOrderPs.close();
            }

            con.close();

            showSuccess(out, cashfreeOrderId, clothifyOrderId);

        } catch (Exception e) {
            e.printStackTrace();
            showError(out, "Payment success update failed: " + e.getMessage());
        }
    }

    private void showSuccess(PrintWriter out, String cashfreeOrderId, int clothifyOrderId) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Payment Successful - Clothify</title>");

        out.println("<style>");
        out.println("body{margin:0;font-family:Arial;background:linear-gradient(135deg,#f8f7ff,#e8f8ff);min-height:100vh;display:flex;align-items:center;justify-content:center;}");
        out.println(".card{width:90%;max-width:560px;background:white;border-radius:24px;padding:45px 30px;text-align:center;box-shadow:0 15px 35px rgba(0,0,0,0.15);}");
        out.println(".icon{font-size:75px;margin-bottom:15px;}");
        out.println("h1{color:#166534;font-size:34px;margin-bottom:12px;}");
        out.println("p{font-size:18px;color:#444;word-break:break-word;line-height:1.6;}");
        out.println("a{display:inline-block;text-decoration:none;background:#ff3f6c;color:white;padding:14px 42px;border-radius:30px;font-size:18px;font-weight:bold;margin:10px;}");
        out.println(".orders{background:#111827;}");
        out.println("</style>");

        out.println("</head>");
        out.println("<body>");

        out.println("<div class='card'>");
        out.println("<div class='icon'>✅</div>");
        out.println("<h1>Payment Successful</h1>");
        out.println("<p>Your Cashfree payment is completed successfully.</p>");
        out.println("<p><b>Cashfree Order ID:</b><br>" + cashfreeOrderId + "</p>");

        if (clothifyOrderId > 0) {
            out.println("<p><b>Clothify Order ID:</b> #" + clothifyOrderId + "</p>");
        }

        out.println("<a class='orders' href='orders'>Go to My Orders</a>");
        out.println("<a href='home'>Go to Home</a>");
        out.println("</div>");

        out.println("</body>");
        out.println("</html>");
    }

    private void showError(PrintWriter out, String message) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Payment Error - Clothify</title>");

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
        out.println("<p>" + message + "</p>");
        out.println("<a href='checkout-page'>Back to Checkout</a>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
}