package com.bankapp.servlet;

import com.bankapp.dao.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * RegisterApiServlet handles user registration.
 * It validates the input, checks if the user already exists,
 * and inserts a new user into the database if validation passes.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

@WebServlet("/api/register")
public class RegisterApiServlet extends BaseRestServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String phoneNumber = request.getParameter("phoneNumber");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // Validate input
        if (name == null || name.trim().isEmpty()) {
            sendErrorResponse(response, "Name is required");
            return;
        }
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            sendErrorResponse(response, "Phone number is required");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            sendErrorResponse(response, "Password is required");
            return;
        }
        if (!password.equals(confirmPassword)) {
            sendErrorResponse(response, "Passwords do not match");
            return;
        }

        try (Connection conn = Database.getConnection()) {
            // Check if user already exists
            String checkSql = "SELECT COUNT(*) as count FROM users WHERE phone_number = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, phoneNumber.trim());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt("count") > 0) {
                        sendErrorResponse(response, "User with this phone number already exists");
                        return;
                    }
                }
            }

            // Insert new user
            String insertSql = "INSERT INTO users (name, phone_number, password) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, name.trim());
                insertStmt.setString(2, phoneNumber.trim());
                insertStmt.setString(3, password);

                int result = insertStmt.executeUpdate();
                if (result > 0) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name.trim());
                    userData.put("phoneNumber", phoneNumber.trim());
                    userData.put("message", "User registered successfully");
                    
                    sendSuccessResponse(response, userData);
                } else {
                    sendErrorResponse(response, "Failed to register user", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, "Database error occurred", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
} 