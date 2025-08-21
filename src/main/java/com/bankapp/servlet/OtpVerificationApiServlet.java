package com.bankapp.servlet;

import com.bankapp.dao.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * OtpVerificationApiServlet handles OTP verification for transactions.
 * It checks if the user is authenticated, retrieves the pending transaction,
 * verifies the OTP, and completes the transaction if valid.
 * If the OTP is invalid or no pending transaction is found, it returns an error.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

@WebServlet("/api/verifyOtp")
public class OtpVerificationApiServlet extends BaseRestServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Check if user is logged in
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("userId");
        String phoneNumber = (String) session.getAttribute("phoneNumber");

        if (userId == null) {
            sendErrorResponse(response, "User not authenticated", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String enteredOtp = request.getParameter("otp");

        if (enteredOtp == null || enteredOtp.trim().isEmpty()) {
            sendErrorResponse(response, "OTP is required");
            return;
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // Get the pending transaction for this user
                String getTransactionSql = "SELECT * FROM transactions WHERE user_id = ? AND status = 'PENDING' ORDER BY transaction_date DESC LIMIT 1";
                Integer fromAccountId = null;
                Integer toAccountId = null;
                Double amount = null;
                String storedOtp = null;
                String transferMode = null;

                try (PreparedStatement stmt = conn.prepareStatement(getTransactionSql)) {
                    stmt.setInt(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            sendErrorResponse(response, "No pending transaction found");
                            return;
                        }
                        fromAccountId = rs.getInt("from_account_id");
                        toAccountId = rs.getInt("to_account_id");
                        amount = rs.getDouble("amount");
                        storedOtp = rs.getString("otp");
                        transferMode = rs.getString("transfer_mode");
                    }
                }

                // Verify OTP
                if (!enteredOtp.equals(storedOtp)) {
                    sendErrorResponse(response, "Invalid OTP");
                    return;
                }

                // Get user_id for both accounts
                int fromUserId = 0;
                int toUserId = 0;
                
                String getUserIdsSql = "SELECT a.user_id FROM accounts a WHERE a.account_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(getUserIdsSql)) {
                    stmt.setInt(1, fromAccountId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            fromUserId = rs.getInt("user_id");
                        }
                    }
                    
                    stmt.setInt(1, toAccountId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            toUserId = rs.getInt("user_id");
                        }
                    }
                }

                // Update the existing transaction to COMPLETED
                String updateTransactionSql = "UPDATE transactions SET status = 'COMPLETED' WHERE user_id = ? AND status = 'PENDING'";
                try (PreparedStatement stmt = conn.prepareStatement(updateTransactionSql)) {
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();
                }

                // Create transaction record for beneficiary (credit)
                String beneficiaryTransactionSql = "INSERT INTO transactions (user_id, from_account_id, to_account_id, amount, transaction_type, transfer_mode, status) VALUES (?, ?, ?, ?, 'CREDIT', ?, 'COMPLETED')";
                try (PreparedStatement stmt = conn.prepareStatement(beneficiaryTransactionSql)) {
                    stmt.setInt(1, toUserId);
                    stmt.setInt(2, fromAccountId);
                    stmt.setInt(3, toAccountId);
                    stmt.setDouble(4, amount);
                    stmt.setString(5, transferMode);
                    stmt.executeUpdate();
                }

                // Update from account balance
                String updateFromSql = "UPDATE accounts SET balance = balance - ? WHERE account_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateFromSql)) {
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, fromAccountId);
                    stmt.executeUpdate();
                }

                // Update to account balance
                String updateToSql = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateToSql)) {
                    stmt.setDouble(1, amount);
                    stmt.setInt(2, toAccountId);
                    stmt.executeUpdate();
                }
                
                // Commit transaction
                conn.commit();

                // Get account numbers and beneficiary name
                String fromAccountNumber = null;
                String toAccountNumber = null;
                String beneficiaryName = null;
                Integer transactionId = null;

                String getAccountInfoSql = "SELECT t.transaction_id, a.account_number, u.name as full_name " + 
                                        "FROM accounts a " +
                                        "JOIN users u ON u.user_id = a.user_id " +
                                        "JOIN transactions t ON t.from_account_id = ? OR t.to_account_id = ? " +
                                        "WHERE t.user_id = ? AND t.status = 'COMPLETED' " +
                                        "ORDER BY t.transaction_date DESC LIMIT 1";
                
                try (PreparedStatement stmt = conn.prepareStatement(getAccountInfoSql)) {
                    // Get from account info
                    stmt.setInt(1, fromAccountId);
                    stmt.setInt(2, fromAccountId);
                    stmt.setInt(3, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            fromAccountNumber = rs.getString("account_number");
                            transactionId = rs.getInt("transaction_id");
                        }
                    }
                    
                    // Get to account info and beneficiary name
                    stmt.setInt(1, toAccountId);
                    stmt.setInt(2, toAccountId);
                    stmt.setInt(3, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            toAccountNumber = rs.getString("account_number");
                            beneficiaryName = rs.getString("full_name");
                        }
                    }
                }
                
                // Return success response with complete transfer details
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("message", "Transfer completed successfully");
                resultData.put("amount", amount);
                resultData.put("transferType", "DEBIT"); // For the sender
                resultData.put("transferMode", transferMode); // NEFT/RTGS/IMPS
                resultData.put("fromAccount", fromAccountNumber);
                resultData.put("toAccount", toAccountNumber);
                resultData.put("beneficiaryName", beneficiaryName);
                resultData.put("transactionId", transactionId);

                // 🔹 Add headers for Dynatrace capture
                response.setHeader("X-Transfer-Mode", transferMode);
                response.setHeader("X-Success", "true");

                sendSuccessResponse(response, resultData);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("SQL Error in OtpVerificationApiServlet: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Database error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
