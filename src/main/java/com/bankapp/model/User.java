package com.bankapp.model;

/**
 * User model class representing a user in the banking application.
 * It contains fields for user ID, username, password, name, and phone number.
 * It also includes constructors, getters, and setters for these fields.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

public class User {
    private int userId;
    private String username;
    private String password;
    private String name;
    private String phoneNumber;

    public User() {
    }

    public User(int userId, String username, String password, String name, String phoneNumber) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    // Getters and Setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
