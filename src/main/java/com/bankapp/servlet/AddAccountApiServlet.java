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
 * AddAccountApiServlet handles the addition of new accounts for a user.
 * It validates the input, checks if the account number already exists,
 * and inserts a new account into the database if validation passes.
 * * It also checks if the user is authenticated before allowing account creation.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

@WebServlet("/api/addAccount")
public class AddAccountApiServlet extends BaseRestServlet {

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

        String accountNumber = request.getParameter("accountNumber");
        String initialBalance = request.getParameter("initialBalance");

        // Validate input
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            sendErrorResponse(response, "Account number is required");
            return;
        }
        if (initialBalance == null || initialBalance.trim().isEmpty()) {
            sendErrorResponse(response, "Initial balance is required");
            return;
        }

        try {
            double balance = Double.parseDouble(initialBalance);
            if (balance < 0) {
                sendErrorResponse(response, "Initial balance cannot be negative");
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid initial balance amount");
            return;
        }

        // Validate account number format (10-16 digits)
        if (!accountNumber.matches("\\d{10,16}")) {
            sendErrorResponse(response, "Account number must be 10-16 digits");
            return;
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // Check if account number already exists
                String checkAccountSql = "SELECT COUNT(*) FROM accounts WHERE account_number = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkAccountSql)) {
                    checkStmt.setString(1, accountNumber);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            sendErrorResponse(response, "Account number already exists");
                            return;
                        }
                    }
                }

                // Insert new account
                String insertAccountSql = "INSERT INTO accounts (user_id, account_number, balance) VALUES (?, ?, ?)";
                int accountId;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertAccountSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, accountNumber);
                    insertStmt.setDouble(3, Double.parseDouble(initialBalance));
                    insertStmt.executeUpdate();
                    
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            accountId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Failed to create account");
                        }
                    }
                }

                conn.commit();

                // Return success response
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("accountId", accountId);
                accountData.put("accountNumber", accountNumber);
                accountData.put("balance", Double.parseDouble(initialBalance));
                accountData.put("message", "Account created successfully");

                sendSuccessResponse(response, accountData);

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
} 