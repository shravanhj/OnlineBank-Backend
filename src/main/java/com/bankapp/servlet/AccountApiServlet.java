package com.bankapp.servlet;

import com.bankapp.dao.Database;
import com.bankapp.model.Account;
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
 * AccountApiServlet handles account-related operations.
 * It allows users to view their accounts, add new accounts, and retrieve account details.
 * It also checks if the user is authenticated before allowing access to account operations.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

@WebServlet("/api/accounts")
public class AccountApiServlet extends BaseRestServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        request.setCharacterEncoding("UTF-8");

                // Simulate server erroros for testing purposes
        if (Math.random() < 0.1) {
            int[] errorCodes = {500, 502, 503, 504};
            int randomCode = errorCodes[(int)(Math.random() * errorCodes.length)];
            sendErrorResponse(response, "xcepted Error... Simulated server error (" + randomCode + ") for testing", randomCode);
            return;
        }

        // Check if user is logged in
        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("userId");
        String phoneNumber = (String) session.getAttribute("phoneNumber");

        if (userId == null) {
            sendErrorResponse(response, "User not authenticated", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try (Connection conn = Database.getConnection()) {
            List<Account> userAccounts = new ArrayList<>();
            List<Account> allAccounts = new ArrayList<>();
            List<Transaction> transactions = new ArrayList<>();

            // Query for user's accounts
            String userAccountsSql = "SELECT a.* FROM accounts a JOIN users u ON a.user_id = u.user_id WHERE u.phone_number = ?";
            try (PreparedStatement accountStmt = conn.prepareStatement(userAccountsSql)) {
                accountStmt.setString(1, phoneNumber);
                try (ResultSet accountRs = accountStmt.executeQuery()) {
                    while (accountRs.next()) {
                        int accountId = accountRs.getInt("account_id");
                        String accountNumber = accountRs.getString("account_number");
                        double balance = accountRs.getDouble("balance");
                        userAccounts.add(new Account(accountId, accountNumber, balance));
                    }
                }
            }

            // Query for all other accounts (excluding user's own accounts)
            String allAccountsSql = "SELECT a.*, u.name as beneficiary_name FROM accounts a " +
                                  "JOIN users u ON a.user_id = u.user_id " +
                                  "WHERE a.user_id != (SELECT user_id FROM users WHERE phone_number = ?)";
            try (PreparedStatement allAccountsStmt = conn.prepareStatement(allAccountsSql)) {
                allAccountsStmt.setString(1, phoneNumber);
                try (ResultSet allAccountsRs = allAccountsStmt.executeQuery()) {
                    while (allAccountsRs.next()) {
                        int accountId = allAccountsRs.getInt("account_id");
                        String accountNumber = allAccountsRs.getString("account_number");
                        double balance = allAccountsRs.getDouble("balance");
                        String beneficiaryName = allAccountsRs.getString("beneficiary_name");
                        Account account = new Account(accountId, accountNumber, balance);
                        account.setBeneficiaryName(beneficiaryName);
                        allAccounts.add(account);
                    }
                }
            }

            // Query for transaction history (added transfer_mode)
            String transactionSql = "SELECT t.transaction_id, t.amount, t.transaction_date, t.transaction_type, t.status, " +
                                    "t.from_account_id, t.to_account_id, t.otp, t.transfer_mode " +
                                    "FROM transactions t " +
                                    "WHERE t.user_id = ? " +
                                    "ORDER BY t.transaction_date DESC";

            try (PreparedStatement transactionStmt = conn.prepareStatement(transactionSql)) {
                transactionStmt.setInt(1, userId);
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
                        transaction.setTransferMode(transferMode); // <-- added

                        transactions.add(transaction);
                    }
                }
            }

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accounts", userAccounts);
            responseData.put("allAccounts", allAccounts);
            responseData.put("transactions", transactions);
            responseData.put("userId", userId);

            sendSuccessResponse(response, responseData);

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, "Database error occurred", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
