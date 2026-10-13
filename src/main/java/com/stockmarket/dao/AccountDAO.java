package com.stockmarket.dao;

import com.stockmarket.config.DBConnection;
import com.stockmarket.model.UserAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for USER_ACCOUNT table.
 * Manages virtual trading balances and simulated deposits.
 */
public class AccountDAO {
    private static final Logger LOGGER = Logger.getLogger(AccountDAO.class.getName());

    public UserAccount getAccountByUserId(int userId) {
        String sql = "SELECT ACCOUNT_ID, USER_ID, CASH_BALANCE, CREATED_AT FROM USER_ACCOUNT WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserAccount acc = new UserAccount();
                    acc.setAccountId(rs.getInt("ACCOUNT_ID"));
                    acc.setUserId(rs.getInt("USER_ID"));
                    acc.setCashBalance(rs.getDouble("CASH_BALANCE"));
                    acc.setCreatedAt(rs.getTimestamp("CREATED_AT"));
                    return acc;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching account for user " + userId, e);
        }
        return null;
    }

    /**
     * Executes an atomic simulated deposit to fund the user's virtual trading account.
     */
    public boolean addDeposit(int userId, double amount) throws SQLException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly positive.");
        }

        String lockSql = "SELECT CASH_BALANCE FROM USER_ACCOUNT WHERE USER_ID = ? FOR UPDATE";
        String updateBalSql = "UPDATE USER_ACCOUNT SET CASH_BALANCE = CASH_BALANCE + ? WHERE USER_ID = ?";
        String insertTxnSql = "INSERT INTO STOCK_TRANSACTION (USER_ID, STOCK_ID, TRANSACTION_TYPE, QUANTITY, PRICE_PER_SHARE, TOTAL_AMOUNT, STATUS) " +
                              "VALUES (?, NULL, 'DEPOSIT', 1, ?, ?, 'COMPLETED')";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Lock and verify user account exists
            try (PreparedStatement psLock = conn.prepareStatement(lockSql)) {
                psLock.setInt(1, userId);
                try (ResultSet rs = psLock.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("User account does not exist.");
                    }
                }
            }

            // 2. Update cash balance
            try (PreparedStatement psUpd = conn.prepareStatement(updateBalSql)) {
                psUpd.setDouble(1, amount);
                psUpd.setInt(2, userId);
                psUpd.executeUpdate();
            }

            // 3. Record audit trail in transactions log
            try (PreparedStatement psTxn = conn.prepareStatement(insertTxnSql)) {
                psTxn.setInt(1, userId);
                psTxn.setDouble(2, amount);
                psTxn.setDouble(3, amount);
                psTxn.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            LOGGER.log(Level.SEVERE, "Failed to deposit funds for user " + userId, e);
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }
}
