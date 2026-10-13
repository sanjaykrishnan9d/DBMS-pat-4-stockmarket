package com.stockmarket.model;

import java.sql.Timestamp;

public class UserAccount {
    private int accountId;
    private int userId;
    private double cashBalance;
    private Timestamp createdAt;

    public UserAccount() {}

    public UserAccount(int accountId, int userId, double cashBalance, Timestamp createdAt) {
        this.accountId = accountId;
        this.userId = userId;
        this.cashBalance = cashBalance;
        this.createdAt = createdAt;
    }

    public int getAccountId() { return accountId; }
    public void setAccountId(int accountId) { this.accountId = accountId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getCashBalance() { return cashBalance; }
    public void setCashBalance(double cashBalance) { this.cashBalance = cashBalance; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
