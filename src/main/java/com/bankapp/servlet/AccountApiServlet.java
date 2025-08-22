package com.bankapp.servlet;

import com.bankapp.dao.Database;
import com.bankapp.model.Account;
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
 * It allows users to view their accounts and other beneficiary accounts.
 * Transactions are moved to TransactionApiServlet.
 */
@WebServlet("/api/accounts")
public class AccountApiServlet extends BaseRestServlet {

    private static final int DEFAULT_PAGE_SIZE = 2000; // number of accounts per page

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("userId");
        String phoneNumber = (String) session.getAttribute("phoneNumber");

        if (userId == null) {
            sendErrorResponse(response, "User not authenticated", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Read pagination parameters
        int pageSize = DEFAULT_PAGE_SIZE;
        int page = 0;
        String pageParam = request.getParameter("page");
        String sizeParam = request.getParameter("size");
        try {
            if (pageParam != null) page = Integer.parseInt(pageParam);
            if (sizeParam != null) pageSize = Integer.parseInt(sizeParam);
        } catch (NumberFormatException ignored) {}

        int offset = page * pageSize;

        try (Connection conn = Database.getConnection()) {
            List<Account> userAccounts = new ArrayList<>();
            List<Account> allAccounts = new ArrayList<>();

            // User's accounts with pagination
            String userAccountsSql = "SELECT a.* FROM accounts a JOIN users u ON a.user_id = u.user_id " +
                                     "WHERE u.phone_number = ? LIMIT ? OFFSET ?";
            try (PreparedStatement stmt = conn.prepareStatement(userAccountsSql)) {
                stmt.setString(1, phoneNumber);
                stmt.setInt(2, pageSize);
                stmt.setInt(3, offset);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int accountId = rs.getInt("account_id");
                        String accountNumber = rs.getString("account_number");
                        double balance = rs.getDouble("balance");
                        userAccounts.add(new Account(accountId, accountNumber, balance));
                    }
                }
            }

            // All other accounts with pagination
            String allAccountsSql = "SELECT a.*, u.name AS beneficiary_name FROM accounts a " +
                                    "JOIN users u ON a.user_id = u.user_id " +
                                    "WHERE a.user_id != (SELECT user_id FROM users WHERE phone_number = ?) " +
                                    "LIMIT ? OFFSET ?";
            try (PreparedStatement stmt = conn.prepareStatement(allAccountsSql)) {
                stmt.setString(1, phoneNumber);
                stmt.setInt(2, pageSize);
                stmt.setInt(3, offset);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int accountId = rs.getInt("account_id");
                        String accountNumber = rs.getString("account_number");
                        double balance = rs.getDouble("balance");
                        String beneficiaryName = rs.getString("beneficiary_name");
                        Account account = new Account(accountId, accountNumber, balance);
                        account.setBeneficiaryName(beneficiaryName);
                        allAccounts.add(account);
                    }
                }
            }

            // Determine if there is a next page
            boolean hasNextPage = (userAccounts.size() == pageSize || allAccounts.size() == pageSize);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accounts", userAccounts);
            responseData.put("allAccounts", allAccounts);
            responseData.put("userId", userId);
            responseData.put("nextPage", hasNextPage ? page + 1 : null);

            sendSuccessResponse(response, responseData);

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, "Database error occurred", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

