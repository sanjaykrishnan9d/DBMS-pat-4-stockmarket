package com.stockmarket.dao;

import com.stockmarket.config.DBConnection;
import com.stockmarket.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for USERS and initial account provisioning.
 * All queries utilize PreparedStatements to prevent SQL injection.
 */
public class UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    public User findByUsername(String username) {
        String sql = "SELECT USER_ID, FULL_NAME, EMAIL, PHONE, USERNAME, PASSWORD_HASH, ROLE, CREATED_AT, ACCOUNT_STATUS " +
                     "FROM USERS WHERE LOWER(USERNAME) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in findByUsername for: " + username, e);
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT USER_ID, FULL_NAME, EMAIL, PHONE, USERNAME, PASSWORD_HASH, ROLE, CREATED_AT, ACCOUNT_STATUS " +
                     "FROM USERS WHERE LOWER(EMAIL) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in findByEmail for: " + email, e);
        }
        return null;
    }

    public User findById(int userId) {
        String sql = "SELECT USER_ID, FULL_NAME, EMAIL, PHONE, USERNAME, PASSWORD_HASH, ROLE, CREATED_AT, ACCOUNT_STATUS " +
                     "FROM USERS WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in findById for: " + userId, e);
        }
        return null;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM USERS WHERE LOWER(USERNAME) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking username uniqueness", e);
        }
        return false;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM USERS WHERE LOWER(EMAIL) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking email uniqueness", e);
        }
        return false;
    }

    /**
     * Registers a new user and automatically establishes a starting USER_ACCOUNT
     * with an initial simulated balance of $10,000 for learning and trading.
     */
    public boolean createUser(User user, double initialBalance) throws SQLException {
        String userSql = "INSERT INTO USERS (FULL_NAME, EMAIL, PHONE, USERNAME, PASSWORD_HASH, ROLE, ACCOUNT_STATUS) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String selectIdSql = "SELECT USER_ID FROM USERS WHERE LOWER(USERNAME) = LOWER(?)";
        String accountSql = "INSERT INTO USER_ACCOUNT (USER_ID, CASH_BALANCE) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert user
            try (PreparedStatement psUser = conn.prepareStatement(userSql)) {
                psUser.setString(1, user.getFullName());
                psUser.setString(2, user.getEmail().toLowerCase());
                psUser.setString(3, user.getPhone());
                psUser.setString(4, user.getUsername().toLowerCase());
                psUser.setString(5, user.getPasswordHash());
                psUser.setString(6, user.getRole() != null ? user.getRole() : "USER");
                psUser.setString(7, user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");
                psUser.executeUpdate();
            }

            // 2. Retrieve generated USER_ID
            int generatedUserId = 0;
            try (PreparedStatement psId = conn.prepareStatement(selectIdSql)) {
                psId.setString(1, user.getUsername().toLowerCase());
                try (ResultSet rs = psId.executeQuery()) {
                    if (rs.next()) {
                        generatedUserId = rs.getInt(1);
                    }
                }
            }

            if (generatedUserId == 0) {
                conn.rollback();
                throw new SQLException("Could not retrieve generated user ID.");
            }

            user.setUserId(generatedUserId);

            // 3. Create initial virtual user trading account
            try (PreparedStatement psAcc = conn.prepareStatement(accountSql)) {
                psAcc.setInt(1, generatedUserId);
                psAcc.setDouble(2, initialBalance);
                psAcc.executeUpdate();
            }

            conn.commit(); // Transaction successful
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            LOGGER.log(Level.SEVERE, "Failed to create user transactionally", e);
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("USER_ID"));
        u.setFullName(rs.getString("FULL_NAME"));
        u.setEmail(rs.getString("EMAIL"));
        u.setPhone(rs.getString("PHONE"));
        u.setUsername(rs.getString("USERNAME"));
        u.setPasswordHash(rs.getString("PASSWORD_HASH"));
        u.setRole(rs.getString("ROLE"));
        u.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        u.setAccountStatus(rs.getString("ACCOUNT_STATUS"));
        return u;
    }
}
