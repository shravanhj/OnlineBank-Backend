package com.bankapp.servlet;

import com.bankapp.dao.Database;
import com.bankapp.model.Transaction;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TransactionApiServlet handles fetching transactions for a user.
 * Accepts startDate and endDate as request parameters.
 */
@WebServlet("/api/transactions")
public class TransactionApiServlet extends BaseRestServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // Check if user is logged in
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("userId");

        if (userId == null) {
            sendErrorResponse(response, "User not authenticated", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Get date range parameters
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");

        if (startDate == null || endDate == null) {
            sendErrorResponse(response, "startDate and endDate parameters are required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = Database.getConnection()) {
            List<Transaction> transactions = new ArrayList<>();

            // Query for transaction history within given range
            String transactionSql = "SELECT t.transaction_id, t.amount, t.transaction_date, t.transaction_type, t.status, " +
                                    "t.from_account_id, t.to_account_id, t.otp, t.transfer_mode " +
                                    "FROM transactions t " +
                                    "WHERE t.user_id = ? AND DATE(t.transaction_date) BETWEEN ? AND ? " +
                                    "ORDER BY t.transaction_date DESC";

            try (PreparedStatement transactionStmt = conn.prepareStatement(transactionSql)) {
                transactionStmt.setInt(1, userId);
                transactionStmt.setString(2, startDate);
                transactionStmt.setString(3, endDate);

                try (ResultSet transactionRs = transactionStmt.executeQuery()) {
                    while (transactionRs.next()) {
                        int transactionId = transactionRs.getInt("transaction_id");
                        double amount = transactionRs.getDouble("amount");
                        java.sql.Timestamp date = transactionRs.getTimestamp("transaction_date");
                        String type = transactionRs.getString("transaction_type");
                        String status = transactionRs.getString("status");
                        int fromAccountId = transactionRs.getInt("from_account_id");
                        int toAccountId = transactionRs.getInt("to_account_id");
                        String otp = transactionRs.getString("otp");
                        String transferMode = transactionRs.getString("transfer_mode");

                        Transaction transaction = new Transaction(transactionId, amount, date, type);
                        transaction.setStatus(status);
                        transaction.setOtp(otp);
                        transaction.setFromAccountId(fromAccountId);
                        transaction.setToAccountId(toAccountId);
                        transaction.setTransferMode(transferMode);

                        transactions.add(transaction);
                    }
                }
            }

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("transactions", transactions);
            responseData.put("userId", userId);

            sendSuccessResponse(response, responseData);

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, "Database error occurred", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
