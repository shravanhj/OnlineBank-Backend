package com.bankapp.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * BaseRestServlet provides common functionality for RESTful servlets.
 * It includes methods for sending JSON responses, handling CORS, and error handling.
 * It is intended to be extended by other servlets in the application.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

public abstract class BaseRestServlet extends HttpServlet {
    
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        setCorsHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    protected void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        response.setHeader("Access-Control-Max-Age", "3600");
    }
    
    protected void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        setCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try (PrintWriter out = response.getWriter()) {
            out.print(objectMapper.writeValueAsString(data));
            out.flush();
        }
    }
    
    protected void sendSuccessResponse(HttpServletResponse response, Object data) throws IOException {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("data", data);
        sendJsonResponse(response, responseMap);
    }
    
    protected void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", false);
        responseMap.put("error", message);
        sendJsonResponse(response, responseMap);
    }
    
    protected void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        sendErrorResponse(response, message, HttpServletResponse.SC_BAD_REQUEST);
    }
} 