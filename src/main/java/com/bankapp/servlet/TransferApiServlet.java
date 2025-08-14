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
import java.util.Random;

/**
 * TransferApiServlet handles fund transfers between accounts.
 * It validates the request, checks account balances, and processes the transfer.
 * If the transfer is successful, it generates an OTP and returns transfer details.
 * If any validation fails, it returns an appropriate error message.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

@WebServlet("/api/transfer")
public class TransferApiServlet extends BaseRestServlet {

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

        String fromAccountId = request.getParameter("fromAccountId");
        String toAccountId = request.getParameter("toAccountId");
        String transferMode = request.getParameter("transferMode");
        String amount = request.getParameter("amount");

        // Validate input
        if (fromAccountId == null || fromAccountId.trim().isEmpty()) {
            sendErrorResponse(response, "From account is required");
            return;
        }
        if (toAccountId == null || toAccountId.trim().isEmpty()) {
            sendErrorResponse(response, "To account is required");
            return;
        }
        if (transferMode == null || transferMode.trim().isEmpty() || 
            !transferMode.matches("^(NEFT|RTGS|IMPS)$")) {
            sendErrorResponse(response, "Valid transfer mode (NEFT/RTGS/IMPS) is required");
            return;
        }
        if (amount == null || amount.trim().isEmpty()) {
            sendErrorResponse(response, "Amount is required");
            return;
        }

        try {
            int fromAccount = Integer.parseInt(fromAccountId);
            int toAccount = Integer.parseInt(toAccountId);
            double transferAmount = Double.parseDouble(amount);
            
            if (transferAmount <= 0) {
                sendErrorResponse(response, "Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid amount or account ID");
            return;
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                int fromAccount = Integer.parseInt(fromAccountId);
                int toAccount = Integer.parseInt(toAccountId);
                double transferAmount = Double.parseDouble(amount);

                // Verify from account belongs to user and has sufficient balance
                String fromAccountSql = "SELECT a.balance, a.account_number FROM accounts a " +
                                      "JOIN users u ON a.user_id = u.user_id " +
                                      "WHERE a.account_id = ? AND u.phone_number = ?";
                String fromAccountNumber = "";
                try (PreparedStatement fromStmt = conn.prepareStatement(fromAccountSql)) {
                    fromStmt.setInt(1, fromAccount);
                    fromStmt.setString(2, phoneNumber);
                    try (ResultSet fromRs = fromStmt.executeQuery()) {
                        if (!fromRs.next()) {
                            sendErrorResponse(response, "Invalid from account");
                            return;
                        }
                        double currentBalance = fromRs.getDouble("balance");
                        fromAccountNumber = fromRs.getString("account_number");
                        if (currentBalance < transferAmount) {
                            sendErrorResponse(response, "Insufficient balance");
                            return;
                        }
                    }
                }

                // Get to account details
                String toAccountSql = "SELECT a.account_number, u.name as beneficiary_name FROM accounts a " +
                                    "JOIN users u ON a.user_id = u.user_id " +
                                    "WHERE a.account_id = ?";
                String toAccountNumber = "";
                String beneficiaryName = "";
                try (PreparedStatement toStmt = conn.prepareStatement(toAccountSql)) {
                    toStmt.setInt(1, toAccount);
                    try (ResultSet toRs = toStmt.executeQuery()) {
                        if (!toRs.next()) {
                            sendErrorResponse(response, "Invalid to account");
                            return;
                        }
                        toAccountNumber = toRs.getString("account_number");
                        beneficiaryName = toRs.getString("beneficiary_name");
                    }
                }

                // Check if transferring to same account
                if (fromAccount == toAccount) {
                    sendErrorResponse(response, "Cannot transfer to the same account");
                    return;
                }

                // Generate OTP
                String otp = generateOTP();

                // Insert transaction record
                String insertTransactionSql = "INSERT INTO transactions (user_id, from_account_id, to_account_id, amount, transaction_type, transfer_mode, status, otp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertTransactionSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, fromAccount);
                    insertStmt.setInt(3, toAccount);
                    insertStmt.setDouble(4, transferAmount);
                    insertStmt.setString(5, "DEBIT"); // Always DEBIT for outgoing transfers
                    insertStmt.setString(6, transferMode);
                    insertStmt.setString(7, "PENDING");
                    insertStmt.setString(8, otp);
                    insertStmt.executeUpdate();
                }

                conn.commit();

                // Return success response with transfer details
                Map<String, Object> transferData = new HashMap<>();
                transferData.put("message", "Transfer initiated successfully");
                transferData.put("otp", otp);
                transferData.put("amount", transferAmount);
                transferData.put("fromAccount", fromAccountNumber);
                transferData.put("toAccount", toAccountNumber);
                transferData.put("beneficiaryName", beneficiaryName);
                transferData.put("transferMode", transferMode);

                sendSuccessResponse(response, transferData);

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, "Database error occurred", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
} 