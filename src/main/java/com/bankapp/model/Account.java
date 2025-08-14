package com.bankapp.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Account model class representing a bank account in the banking application.
 * It contains fields for account ID, account number, balance, and beneficiary name.
 * It also includes constructors, getters, and setters for these fields.
 * @author  Shravan HJ
 * @email   shravanhj@gmail.com
 * @version 1.0
 * @since   2025-05-14
 */

public class Account {
    private int accountId;
    private String accountNumber;
    private double balance;
    private String beneficiaryName;

    // Default constructor for JSON serialization
    public Account() {
    }

    public Account(int accountId, String accountNumber, double balance) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }

    // Getters and Setters
    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", accountNumber='" + accountNumber + '\'' +
                ", balance=" + balance +
                '}';
    }
}
