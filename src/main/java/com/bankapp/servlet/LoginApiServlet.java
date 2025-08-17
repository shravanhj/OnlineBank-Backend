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
 * LoginApiServlet handles user login.
 * It validates the input, checks user credentials in the database,
 * and creates a session for the user if authentication is successful.
 * If authentication fails, it returns an error message.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

@WebServlet("/api/login")
public class LoginApiServlet extends BaseRestServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

                // Simulate server erroros for testing purposes
        if (Math.random() < 0.1) {
            int[] errorCodes = {500, 502, 503, 504};
            int randomCode = errorCodes[(int)(Math.random() * errorCodes.length)];
            sendErrorResponse(response, "xcepted Error... Simulated server error (" + randomCode + ") for testing", randomCode);
            return;
        }

        String phoneNumber = request.getParameter("phoneNumber");
        String password = request.getParameter("password");

        // Validate input
        if (phoneNumber == null || phoneNumber.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            sendErrorResponse(response, "Phone number and password are required");
            return;
        }

        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT user_id, name, phone_number FROM users WHERE phone_number = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, phoneNumber.trim());
                stmt.setString(2, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // User found, create session
                        HttpSession session = request.getSession();
                        session.setAttribute("userId", rs.getInt("user_id"));
                        session.setAttribute("userName", rs.getString("name"));
                        session.setAttribute("phoneNumber", rs.getString("phone_number"));
                        
                        // Return user data
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", rs.getInt("user_id"));
                        userData.put("userName", rs.getString("name"));
                        userData.put("phoneNumber", rs.getString("phone_number"));
                        
                        sendSuccessResponse(response, userData);
                    } else {
                        sendErrorResponse(response, "Invalid phone number or password", HttpServletResponse.SC_UNAUTHORIZED);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, "Database error occurred", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
} 