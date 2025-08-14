package com.bankapp.servlet;

import com.bankapp.dao.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;

/**
 * PaymentGatewayServlet handles payment processing.
 * It validates the payment request, generates an OTP,
 * and processes the payment through a simulated payment gateway.
 * It also handles OTP verification and transaction recording.
 * If the payment is successful, it redirects to the specified URL with the result.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

public class PaymentGatewayServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        // Get amount, token, redirectUrl, and loginToken from query params
        String amountStr = request.getParameter("amount");
        String token = request.getParameter("token");
        String redirectUrl = request.getParameter("redirectUrl");
        String loginToken = request.getParameter("loginToken");
        System.out.println("[PaymentGatewayServlet][doGet] loginToken=" + loginToken);
        request.setAttribute("amount", amountStr);
        request.setAttribute("token", token);
        request.setAttribute("redirectUrl", redirectUrl);
        request.setAttribute("loginToken", loginToken);
        // Store redirectUrl and loginToken in session for robustness
        HttpSession session = request.getSession();
        session.setAttribute("redirectUrl", redirectUrl);
        session.setAttribute("loginToken", loginToken);
        request.getRequestDispatcher("payment.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        String step = request.getParameter("step");
        String redirectUrl = request.getParameter("redirectUrl");
        String loginToken = request.getParameter("loginToken");
        if (loginToken == null || loginToken.isEmpty()) {
            Object sessionToken = session.getAttribute("loginToken");
            if (sessionToken instanceof String) {
                loginToken = (String) sessionToken;
            }
        }
        System.out.println("[PaymentGatewayServlet][doPost] step=" + step + ", loginToken=" + loginToken);
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            redirectUrl = (String) session.getAttribute("redirectUrl");
        }
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            redirectUrl = "index.jsp"; // fallback if not provided
        }
        if ("otp".equals(step)) {
            handleOtpVerification(request, response, session, redirectUrl, loginToken);
            return;
        }
        // Step 1: Validate account and balance
        String accountNumber = request.getParameter("accountNumber");
        String amountStr = request.getParameter("amount");
        String token = request.getParameter("token");
        double amount = 0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            request.setAttribute("error", "Invalid amount.");
            request.setAttribute("amount", amountStr);
            request.setAttribute("token", token);
            request.setAttribute("redirectUrl", redirectUrl);
            request.getRequestDispatcher("payment.jsp").forward(request, response);
            return;
        }
        try (Connection conn = Database.getConnection()) {
            // Check if account exists
            String sql = "SELECT account_id, balance FROM accounts WHERE account_number = ?";
            int accountId = -1;
            double balance = 0;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, accountNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        accountId = rs.getInt("account_id");
                        balance = rs.getDouble("balance");
                    } else {
                        request.setAttribute("error", "Account number does not exist.");
                        request.setAttribute("amount", amountStr);
                        request.setAttribute("token", token);
                        request.setAttribute("redirectUrl", redirectUrl);
                        request.getRequestDispatcher("payment.jsp").forward(request, response);
                        return;
                    }
                }
            }
            if (balance < amount) {
                request.setAttribute("error", "Insufficient balance.");
                request.setAttribute("amount", amountStr);
                request.setAttribute("token", token);
                request.setAttribute("redirectUrl", redirectUrl);
                request.getRequestDispatcher("payment.jsp").forward(request, response);
                return;
            }
            // Generate OTP
            String otp = generateOTP();
            session.setAttribute("paymentAccountId", accountId);
            session.setAttribute("paymentAccountNumber", accountNumber);
            session.setAttribute("paymentAmount", amount);
            session.setAttribute("paymentToken", token);
            session.setAttribute("paymentOtp", otp);
            // Forward to OTP page
            request.setAttribute("accountNumber", accountNumber);
            request.setAttribute("amount", amount);
            request.setAttribute("token", token);
            request.setAttribute("otp", otp); // For demo only
            request.setAttribute("redirectUrl", redirectUrl);
            request.setAttribute("loginToken", loginToken);
            request.getRequestDispatcher("payment-otp.jsp").forward(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
            request.setAttribute("error", "Database error: " + e.getMessage());
            request.setAttribute("amount", amountStr);
            request.setAttribute("token", token);
            request.setAttribute("redirectUrl", redirectUrl);
            request.getRequestDispatcher("payment.jsp").forward(request, response);
        }
    }

    private void handleOtpVerification(HttpServletRequest request, HttpServletResponse response, HttpSession session, String redirectUrl, String loginToken) throws ServletException, IOException {
        if (loginToken == null || loginToken.isEmpty()) {
            Object sessionToken = session.getAttribute("loginToken");
            if (sessionToken instanceof String) {
                loginToken = (String) sessionToken;
            }
        }
        System.out.println("[PaymentGatewayServlet][handleOtpVerification] loginToken=" + loginToken);
        String enteredOtp = request.getParameter("otp");
        String storedOtp = (String) session.getAttribute("paymentOtp");
        Integer accountId = (Integer) session.getAttribute("paymentAccountId");
        String accountNumber = (String) session.getAttribute("paymentAccountNumber");
        Double amount = (Double) session.getAttribute("paymentAmount");
        String token = (String) session.getAttribute("paymentToken");
        String status = "success"; // Default to success
        if (accountId == null || amount == null || storedOtp == null) {
            status = "failure";
            request.setAttribute("status", status);
            request.setAttribute("token", token);
            request.setAttribute("amount", amount);
            request.setAttribute("message", "Session expired. Please try again.");
            request.getRequestDispatcher("payment-result.jsp").forward(request, response);
            return;
        }
        if (!storedOtp.equals(enteredOtp)) {
            status = "failure";
            request.setAttribute("status", status);
            request.setAttribute("error", "Invalid OTP. Please try again.");
            request.setAttribute("accountNumber", accountNumber);
            request.setAttribute("amount", amount);
            request.setAttribute("token", token);
            request.setAttribute("otp", storedOtp);
            request.setAttribute("redirectUrl", redirectUrl);
            request.setAttribute("loginToken", loginToken);
            request.getRequestDispatcher("payment-otp.jsp").forward(request, response);
            return;
        }
        // OTP valid, record transaction and update balance
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Get user_id for account
                int userId = 0;
                String getUserIdSql = "SELECT user_id FROM accounts WHERE account_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(getUserIdSql)) {
                    stmt.setInt(1, accountId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("user_id");
                        }
                    }
                }
                // Fetch user name if needed
                String userName = null;
                String getUserNameSql = "SELECT name FROM users WHERE user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(getUserNameSql)) {
                    stmt.setInt(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            userName = rs.getString("name");
                        }
                    }
                }
                // Find ecom user(s) with name 'ecom', ignoring case and spaces
                int ecomUserId = -1;
                int ecomUserCount = 0;
                String ecomUserSql = "SELECT user_id FROM users WHERE TRIM(LOWER(name)) = 'ecom'";
                try (PreparedStatement stmt = conn.prepareStatement(ecomUserSql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ecomUserId = rs.getInt("user_id");
                            ecomUserCount++;
                        }
                    }
                }
                if (ecomUserCount == 0 || ecomUserId == -1) {
                    status = "failure";
                    request.setAttribute("status", status);
                    request.setAttribute("token", token);
                    request.setAttribute("amount", amount);
                    request.setAttribute("message", "Ecom admin hasn't added their account in onlinebank payment gateway.");
                    request.getRequestDispatcher("payment-result.jsp").forward(request, response);
                    return;
                } else if (ecomUserCount > 1) {
                    status = "failure";
                    request.setAttribute("status", status);
                    request.setAttribute("token", token);
                    request.setAttribute("amount", amount);
                    request.setAttribute("message", "Multiple ecom admin accounts found. Please contact support.");
                    request.getRequestDispatcher("payment-result.jsp").forward(request, response);
                    return;
                }
                // Find ecom account for the single ecom user
                int ecomAccountId = -1;
                String ecomAccountSql = "SELECT account_id FROM accounts WHERE user_id = ? LIMIT 1";
                try (PreparedStatement stmt = conn.prepareStatement(ecomAccountSql)) {
                    stmt.setInt(1, ecomUserId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            ecomAccountId = rs.getInt("account_id");
                        }
                    }
                }
                if (ecomAccountId == -1) {
                    status = "failure";
                    request.setAttribute("status", status);
                    request.setAttribute("token", token);
                    request.setAttribute("amount", amount);
                    request.setAttribute("message", "Ecom admin hasn't added their Bank account in onlinebank payment gateway.");
                    request.getRequestDispatcher("payment-result.jsp").forward(request, response);
                    return;
                }
                // Insert transaction (DEBIT)
                String transactionSql = "INSERT INTO transactions (user_id, from_account_id, to_account_id, amount, transaction_type, status) VALUES (?, ?, ?, ?, 'DEBIT', 'COMPLETED')";
                int transactionId = -1;
                try (PreparedStatement stmt = conn.prepareStatement(transactionSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, accountId);
                    stmt.setInt(3, ecomAccountId);
                    stmt.setDouble(4, amount);
                    stmt.executeUpdate();
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            transactionId = generatedKeys.getInt(1);
                        }
                    }
                }
                // Update account balance
                String updateSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, accountId);
                    stmt.executeUpdate();
                }
                conn.commit();
                // Clear session attributes
                session.removeAttribute("paymentAccountId");
                session.removeAttribute("paymentAccountNumber");
                session.removeAttribute("paymentAmount");
                session.removeAttribute("paymentToken");
                session.removeAttribute("paymentOtp");
                // Redirect user to ecom with payment result
                if (redirectUrl == null || redirectUrl.isEmpty()) {
                    redirectUrl = "index.jsp";
                }
                String redirectWithParams = redirectUrl
                    + "?status=" + status
                    + "&token=" + token
                    + "&amount=" + amount
                    + (loginToken != null ? "&loginToken=" + java.net.URLEncoder.encode(loginToken, "UTF-8") : "");
                session.removeAttribute("redirectUrl");
                session.removeAttribute("loginToken");
                response.sendRedirect(redirectWithParams);
                return;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                if (redirectUrl == null || redirectUrl.isEmpty()) {
                    redirectUrl = "index.jsp";
                }
                String redirectWithParams = redirectUrl
                    + "?status=failure&token=" + token
                    + "&amount=" + amount
                    + (loginToken != null ? "&loginToken=" + java.net.URLEncoder.encode(loginToken, "UTF-8") : "");
                session.removeAttribute("redirectUrl");
                session.removeAttribute("loginToken");
                response.sendRedirect(redirectWithParams);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (redirectUrl == null || redirectUrl.isEmpty()) {
                redirectUrl = "index.jsp";
            }
            String redirectWithParams = redirectUrl
                + "?status=failure&token=" + token
                + "&amount=" + amount
                + (loginToken != null ? "&loginToken=" + java.net.URLEncoder.encode(loginToken, "UTF-8") : "");
            session.removeAttribute("redirectUrl");
            session.removeAttribute("loginToken");
            response.sendRedirect(redirectWithParams);
            return;
        }
    }

    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
} 