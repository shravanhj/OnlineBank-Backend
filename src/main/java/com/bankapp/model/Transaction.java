package com.bankapp.model;

import java.sql.Timestamp;

/**
 * Transaction model class representing a financial transaction in the banking application.
 * It contains fields for transaction ID, amount, date, type, status, from and to
 * account IDs, OTP, and transfer mode.
 * It also includes constructors, getters, and setters for these fields.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

public class Transaction {
    private int transactionId;
    private double amount;
    private Timestamp transactionDate;
    private String transactionType;
    private String status;
    private int fromAccountId;
    private int toAccountId;
    private String otp;
    private String transferMode; // <-- Added field

    // Default constructor for JSON serialization
    public Transaction() {
    }

    public Transaction(int transactionId, double amount, Timestamp date, String type) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.transactionDate = date;
        this.transactionType = type;
    }

    // Getters and Setters
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Timestamp getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Timestamp transactionDate) {
        this.transactionDate = transactionDate;
    }

    // Legacy getter for backward compatibility
    public Timestamp getDate() {
        return transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    // Legacy getter for backward compatibility
    public String getType() {
        return transactionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(int fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public int getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(int toAccountId) {
        this.toAccountId = toAccountId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getTransferMode() {
        return transferMode;
    }

    public void setTransferMode(String transferMode) {
        this.transferMode = transferMode;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", amount=" + amount +
                ", transactionDate=" + transactionDate +
                ", transactionType='" + transactionType + '\'' +
                ", status='" + status + '\'' +
                ", fromAccountId=" + fromAccountId +
                ", toAccountId=" + toAccountId +
                ", otp='" + otp + '\'' +
                ", transferMode='" + transferMode + '\'' +
                '}';
    }
}
