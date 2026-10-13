package com.stockmarket.model;

import java.sql.Timestamp;

public class User {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private String passwordHash;
    private String role; // 'USER' or 'ADMIN'
    private Timestamp createdAt;
    private String accountStatus; // 'ACTIVE', 'SUSPENDED'

    public User() {}

    public User(int userId, String fullName, String email, String phone, String username, String role, Timestamp createdAt, String accountStatus) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.accountStatus = accountStatus;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
