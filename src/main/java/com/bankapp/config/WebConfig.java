package com.bankapp.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class WebConfig implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        
        // Set session timeout (in minutes)
        sc.setSessionTimeout(30);
        
        // Set welcome files
        sc.setInitParameter("jakarta.servlet.jsp.jstl.fmt.localizationContext", "messages");
        
        // Set error pages
        sc.setInitParameter("jakarta.servlet.error.status-code.404", "/error.jsp");
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup code if needed
    }
} 