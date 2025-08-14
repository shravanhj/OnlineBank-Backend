package com.bankapp.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.Arrays;

@WebListener
public class WelcomeFileConfig implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        
        // Set welcome files
        sc.setInitParameter("jakarta.servlet.welcome-file-list", 
            String.join(",", Arrays.asList("index.jsp", "index.html")));
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // No cleanup needed
    }
} 