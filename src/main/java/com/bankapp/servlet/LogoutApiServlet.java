package com.bankapp.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * LogoutApiServlet handles user logout.
 * It invalidates the current session and returns a success message.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

@WebServlet("/api/logout")
public class LogoutApiServlet extends BaseRestServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        
        if (session != null) {
            session.invalidate();
        }

        Map<String, Object> logoutData = new HashMap<>();
        logoutData.put("message", "Logged out successfully");
        
        sendSuccessResponse(response, logoutData);
    }
} 