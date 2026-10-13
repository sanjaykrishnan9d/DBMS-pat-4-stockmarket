package com.stockmarket.dao;

import com.stockmarket.config.DBConnection;
import com.stockmarket.model.StockTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for STOCK_TRANSACTION and atomic trade processing.
 * Implements strict database transaction boundaries, row-level concurrency locks (FOR UPDATE),
 * and rollback mechanics to guarantee ACID properties.
 */
public class TransactionDAO {
    private static final Logger LOGGER = Logger.getLogger(TransactionDAO.class.getName());

    /**
     * Executes an atomic Stock Purchase transaction.
     *
     * @param userId   ID of the buyer
     * @param stockId  ID of the stock being purchased
     * @param quantity Number of shares to buy (must be > 0)
     * @return Completed StockTransaction receipt
     * @throws Exception with user-friendly error message on validation or DB failure
     */
    public StockTransaction executeBuy(int userId, int stockId, int quantity) throws Exception {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin ACID transaction

            // 1. Lock and retrieve current stock price and active status from DB
            String stockSql = "SELECT SYMBOL, COMPANY, CURRENT_PRICE, ACTIVE_STATUS FROM STOCK WHERE STOCK_ID = ? FOR UPDATE";
            String symbol, company;
            double currentPrice;
            String activeStatus;

            try (PreparedStatement psStock = conn.prepareStatement(stockSql)) {
                psStock.setInt(1, stockId);
                try (ResultSet rs = psStock.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new IllegalArgumentException("Stock not found.");
                    }
                    symbol = rs.getString("SYMBOL");
                    company = rs.getString("COMPANY");
                    currentPrice = rs.getDouble("CURRENT_PRICE");
                    activeStatus = rs.getString("ACTIVE_STATUS");
                }
            }

            if (!"ACTIVE".equalsIgnoreCase(activeStatus)) {
                conn.rollback();
                throw new IllegalStateException("Trading for " + symbol + " is currently suspended.");
            }

            double totalCost = Math.round(quantity * currentPrice * 100.0) / 100.0;

            // 2. Lock user cash account and check sufficient balance
            String accSql = "SELECT CASH_BALANCE FROM USER_ACCOUNT WHERE USER_ID = ? FOR UPDATE";
            double currentBalance = 0;
            try (PreparedStatement psAcc = conn.prepareStatement(accSql)) {
                psAcc.setInt(1, userId);
                try (ResultSet rs = psAcc.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new IllegalStateException("User account not found.");
                    }
                    currentBalance = rs.getDouble("CASH_BALANCE");
                }
            }

            if (currentBalance < totalCost) {
                conn.rollback();
                throw new IllegalStateException(String.format(
                    "Insufficient cash balance! Required: $%.2f, Available: $%.2f", totalCost, currentBalance));
            }

            // 3. Deduct totalCost from user cash balance
            String deductSql = "UPDATE USER_ACCOUNT SET CASH_BALANCE = CASH_BALANCE - ? WHERE USER_ID = ?";
            try (PreparedStatement psDeduct = conn.prepareStatement(deductSql)) {
                psDeduct.setDouble(1, totalCost);
                psDeduct.setInt(2, userId);
                psDeduct.executeUpdate();
            }

            // 4. Update or Insert User Portfolio (calculating weighted average buy price)
            String portSql = "SELECT PORTFOLIO_ID, QUANTITY, AVERAGE_BUY_PRICE FROM PORTFOLIO WHERE USER_ID = ? AND STOCK_ID = ? FOR UPDATE";
            boolean hasHolding = false;
            int existingQty = 0;
            double existingAvgPrice = 0.0;

            try (PreparedStatement psPort = conn.prepareStatement(portSql)) {
                psPort.setInt(1, userId);
                psPort.setInt(2, stockId);
                try (ResultSet rs = psPort.executeQuery()) {
                    if (rs.next()) {
                        hasHolding = true;
                        existingQty = rs.getInt("QUANTITY");
                        existingAvgPrice = rs.getDouble("AVERAGE_BUY_PRICE");
                    }
                }
            }

            if (hasHolding) {
                int newTotalQty = existingQty + quantity;
                double newAvgPrice = Math.round(((existingQty * existingAvgPrice) + (quantity * currentPrice)) / newTotalQty * 100.0) / 100.0;

                String updPortSql = "UPDATE PORTFOLIO SET QUANTITY = ?, AVERAGE_BUY_PRICE = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE USER_ID = ? AND STOCK_ID = ?";
                try (PreparedStatement psUpdPort = conn.prepareStatement(updPortSql)) {
                    psUpdPort.setInt(1, newTotalQty);
                    psUpdPort.setDouble(2, newAvgPrice);
                    psUpdPort.setInt(3, userId);
                    psUpdPort.setInt(4, stockId);
                    psUpdPort.executeUpdate();
                }
            } else {
                String insPortSql = "INSERT INTO PORTFOLIO (USER_ID, STOCK_ID, QUANTITY, AVERAGE_BUY_PRICE) VALUES (?, ?, ?, ?)";
                try (PreparedStatement psInsPort = conn.prepareStatement(insPortSql)) {
                    psInsPort.setInt(1, userId);
                    psInsPort.setInt(2, stockId);
                    psInsPort.setInt(3, quantity);
                    psInsPort.setDouble(4, currentPrice);
                    psInsPort.executeUpdate();
                }
            }

            // 5. Record Transaction in STOCK_TRANSACTION
            String insTxnSql = "INSERT INTO STOCK_TRANSACTION (USER_ID, STOCK_ID, TRANSACTION_TYPE, QUANTITY, PRICE_PER_SHARE, TOTAL_AMOUNT, STATUS) " +
                               "VALUES (?, ?, 'BUY', ?, ?, ?, 'COMPLETED')";
            try (PreparedStatement psInsTxn = conn.prepareStatement(insTxnSql)) {
                psInsTxn.setInt(1, userId);
                psInsTxn.setInt(2, stockId);
                psInsTxn.setInt(3, quantity);
                psInsTxn.setDouble(4, currentPrice);
                psInsTxn.setDouble(5, totalCost);
                psInsTxn.executeUpdate();
            }

            // 6. Fetch the newly created transaction receipt
            String fetchTxnSql = "SELECT TRANSACTION_ID, TRANSACTION_DATE FROM STOCK_TRANSACTION " +
                                 "WHERE USER_ID = ? AND STOCK_ID = ? AND TRANSACTION_TYPE = 'BUY' " +
                                 "ORDER BY TRANSACTION_ID DESC";
            long txnId = 0;
            Timestamp txnDate = new Timestamp(System.currentTimeMillis());
            try (PreparedStatement psFetch = conn.prepareStatement(fetchTxnSql)) {
                psFetch.setInt(1, userId);
                psFetch.setInt(2, stockId);
                try (ResultSet rs = psFetch.executeQuery()) {
                    if (rs.next()) {
                        txnId = rs.getLong("TRANSACTION_ID");
                        txnDate = rs.getTimestamp("TRANSACTION_DATE");
                    }
                }
            }

            conn.commit(); // Transaction successfully committed!

            return new StockTransaction(
                txnId, userId, stockId, symbol, company, "BUY", quantity, currentPrice, totalCost, txnDate, "COMPLETED"
            );

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            LOGGER.log(Level.SEVERE, "Error in executeBuy for user " + userId + ", stock " + stockId, e);
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }

    /**
     * Executes an atomic Stock Sale transaction.
     *
     * @param userId   ID of the seller
     * @param stockId  ID of the stock being sold
     * @param quantity Number of shares to sell (must be > 0)
     * @return Completed StockTransaction receipt
     * @throws Exception with user-friendly error message on validation or DB failure
     */
    public StockTransaction executeSell(int userId, int stockId, int quantity) throws Exception {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer.");
        }

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin ACID transaction

            // 1. Lock and verify portfolio holdings
            String portSql = "SELECT QUANTITY, AVERAGE_BUY_PRICE FROM PORTFOLIO WHERE USER_ID = ? AND STOCK_ID = ? FOR UPDATE";
            int ownedQty = 0;
            double avgBuyPrice = 0.0;

            try (PreparedStatement psPort = conn.prepareStatement(portSql)) {
                psPort.setInt(1, userId);
                psPort.setInt(2, stockId);
                try (ResultSet rs = psPort.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new IllegalStateException("You do not own any shares of this stock.");
                    }
                    ownedQty = rs.getInt("QUANTITY");
                    avgBuyPrice = rs.getDouble("AVERAGE_BUY_PRICE");
                }
            }

            if (ownedQty < quantity) {
                conn.rollback();
                throw new IllegalStateException(String.format(
                    "Cannot sell %d shares! You only own %d shares.", quantity, ownedQty));
            }

            // 2. Lock stock to fetch current market price
            String stockSql = "SELECT SYMBOL, COMPANY, CURRENT_PRICE FROM STOCK WHERE STOCK_ID = ? FOR UPDATE";
            String symbol, company;
            double currentPrice;

            try (PreparedStatement psStock = conn.prepareStatement(stockSql)) {
                psStock.setInt(1, stockId);
                try (ResultSet rs = psStock.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new IllegalArgumentException("Stock not found.");
                    }
                    symbol = rs.getString("SYMBOL");
                    company = rs.getString("COMPANY");
                    currentPrice = rs.getDouble("CURRENT_PRICE");
                }
            }

            double totalRevenue = Math.round(quantity * currentPrice * 100.0) / 100.0;

            // 3. Credit proceeds to user's cash account
            String creditSql = "UPDATE USER_ACCOUNT SET CASH_BALANCE = CASH_BALANCE + ? WHERE USER_ID = ?";
            try (PreparedStatement psCredit = conn.prepareStatement(creditSql)) {
                psCredit.setDouble(1, totalRevenue);
                psCredit.setInt(2, userId);
                psCredit.executeUpdate();
            }

            // 4. Update portfolio holdings
            int remainingQty = ownedQty - quantity;
            if (remainingQty == 0) {
                String delPortSql = "DELETE FROM PORTFOLIO WHERE USER_ID = ? AND STOCK_ID = ?";
                try (PreparedStatement psDel = conn.prepareStatement(delPortSql)) {
                    psDel.setInt(1, userId);
                    psDel.setInt(2, stockId);
                    psDel.executeUpdate();
                }
            } else {
                String updPortSql = "UPDATE PORTFOLIO SET QUANTITY = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE USER_ID = ? AND STOCK_ID = ?";
                try (PreparedStatement psUpdPort = conn.prepareStatement(updPortSql)) {
                    psUpdPort.setInt(1, remainingQty);
                    psUpdPort.setInt(2, userId);
                    psUpdPort.setInt(3, stockId);
                    psUpdPort.executeUpdate();
                }
            }

            // 5. Record sale transaction in audit log
            String insTxnSql = "INSERT INTO STOCK_TRANSACTION (USER_ID, STOCK_ID, TRANSACTION_TYPE, QUANTITY, PRICE_PER_SHARE, TOTAL_AMOUNT, STATUS) " +
                               "VALUES (?, ?, 'SELL', ?, ?, ?, 'COMPLETED')";
            try (PreparedStatement psInsTxn = conn.prepareStatement(insTxnSql)) {
                psInsTxn.setInt(1, userId);
                psInsTxn.setInt(2, stockId);
                psInsTxn.setInt(3, quantity);
                psInsTxn.setDouble(4, currentPrice);
                psInsTxn.setDouble(5, totalRevenue);
                psInsTxn.executeUpdate();
            }

            // 6. Retrieve transaction receipt
            String fetchTxnSql = "SELECT TRANSACTION_ID, TRANSACTION_DATE FROM STOCK_TRANSACTION " +
                                 "WHERE USER_ID = ? AND STOCK_ID = ? AND TRANSACTION_TYPE = 'SELL' " +
                                 "ORDER BY TRANSACTION_ID DESC";
            long txnId = 0;
            Timestamp txnDate = new Timestamp(System.currentTimeMillis());
            try (PreparedStatement psFetch = conn.prepareStatement(fetchTxnSql)) {
                psFetch.setInt(1, userId);
                psFetch.setInt(2, stockId);
                try (ResultSet rs = psFetch.executeQuery()) {
                    if (rs.next()) {
                        txnId = rs.getLong("TRANSACTION_ID");
                        txnDate = rs.getTimestamp("TRANSACTION_DATE");
                    }
                }
            }

            conn.commit(); // Transaction successfully committed!

            return new StockTransaction(
                txnId, userId, stockId, symbol, company, "SELL", quantity, currentPrice, totalRevenue, txnDate, "COMPLETED"
            );

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            LOGGER.log(Level.SEVERE, "Error in executeSell for user " + userId + ", stock " + stockId, e);
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }

    /**
     * Retrieves transactions for a specific user with filtering options.
     */
    public List<StockTransaction> getUserTransactions(int userId, String symbolFilter, String typeFilter, int limit) {
        List<StockTransaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT t.TRANSACTION_ID, t.USER_ID, t.STOCK_ID, s.SYMBOL, s.COMPANY, " +
            "t.TRANSACTION_TYPE, t.QUANTITY, t.PRICE_PER_SHARE, t.TOTAL_AMOUNT, t.TRANSACTION_DATE, t.STATUS " +
            "FROM STOCK_TRANSACTION t " +
            "LEFT JOIN STOCK s ON t.STOCK_ID = s.STOCK_ID " +
            "WHERE t.USER_ID = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (symbolFilter != null && !symbolFilter.trim().isEmpty()) {
            sql.append("AND UPPER(s.SYMBOL) LIKE ? ");
            params.add("%" + symbolFilter.trim().toUpperCase() + "%");
        }

        if (typeFilter != null && !typeFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(typeFilter)) {
            sql.append("AND UPPER(t.TRANSACTION_TYPE) = UPPER(?) ");
            params.add(typeFilter.trim());
        }

        sql.append("ORDER BY t.TRANSACTION_DATE DESC, t.TRANSACTION_ID DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            if (limit > 0) {
                ps.setMaxRows(limit);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockTransaction t = new StockTransaction();
                    t.setTransactionId(rs.getLong("TRANSACTION_ID"));
                    t.setUserId(rs.getInt("USER_ID"));
                    int sid = rs.getInt("STOCK_ID");
                    t.setStockId(rs.wasNull() ? null : sid);
                    t.setSymbol(rs.getString("SYMBOL") != null ? rs.getString("SYMBOL") : "CASH");
                    t.setCompany(rs.getString("COMPANY") != null ? rs.getString("COMPANY") : "Virtual Cash Deposit");
                    t.setTransactionType(rs.getString("TRANSACTION_TYPE"));
                    t.setQuantity(rs.getInt("QUANTITY"));
                    t.setPricePerShare(rs.getDouble("PRICE_PER_SHARE"));
                    t.setTotalAmount(rs.getDouble("TOTAL_AMOUNT"));
                    t.setTransactionDate(rs.getTimestamp("TRANSACTION_DATE"));
                    t.setStatus(rs.getString("STATUS"));
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching user transactions", e);
        }
        return list;
    }

    public int countUserTransactions(int userId) {
        String sql = "SELECT COUNT(*) FROM STOCK_TRANSACTION WHERE USER_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting user transactions", e);
        }
        return 0;
    }
}
