package com.stockmarket.dao;

import com.stockmarket.config.DBConnection;
import com.stockmarket.model.Stock;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for STOCK table.
 * Supports active market queries, search, sector filters, and admin stock management.
 */
public class StockDAO {
    private static final Logger LOGGER = Logger.getLogger(StockDAO.class.getName());

    public List<Stock> getAllActiveStocks() {
        List<Stock> list = new ArrayList<>();
        String sql = "SELECT STOCK_ID, SYMBOL, STOCK_NAME, COMPANY, SECTOR, CURRENT_PRICE, ACTIVE_STATUS, UPDATED_AT " +
                     "FROM STOCK WHERE ACTIVE_STATUS = 'ACTIVE' ORDER BY SYMBOL ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToStock(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching active stocks", e);
        }
        return list;
    }

    public List<Stock> getAllStocksAdmin() {
        List<Stock> list = new ArrayList<>();
        String sql = "SELECT STOCK_ID, SYMBOL, STOCK_NAME, COMPANY, SECTOR, CURRENT_PRICE, ACTIVE_STATUS, UPDATED_AT " +
                     "FROM STOCK ORDER BY STOCK_ID ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToStock(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching admin stocks list", e);
        }
        return list;
    }

    public Stock getStockById(int stockId) {
        String sql = "SELECT STOCK_ID, SYMBOL, STOCK_NAME, COMPANY, SECTOR, CURRENT_PRICE, ACTIVE_STATUS, UPDATED_AT " +
                     "FROM STOCK WHERE STOCK_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stockId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStock(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching stock by id: " + stockId, e);
        }
        return null;
    }

    public Stock getStockBySymbol(String symbol) {
        String sql = "SELECT STOCK_ID, SYMBOL, STOCK_NAME, COMPANY, SECTOR, CURRENT_PRICE, ACTIVE_STATUS, UPDATED_AT " +
                     "FROM STOCK WHERE UPPER(SYMBOL) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToStock(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching stock by symbol: " + symbol, e);
        }
        return null;
    }

    public List<Stock> searchStocks(String query, String sector) {
        List<Stock> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT STOCK_ID, SYMBOL, STOCK_NAME, COMPANY, SECTOR, CURRENT_PRICE, ACTIVE_STATUS, UPDATED_AT " +
            "FROM STOCK WHERE ACTIVE_STATUS = 'ACTIVE' "
        );

        List<Object> params = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (UPPER(SYMBOL) LIKE ? OR UPPER(COMPANY) LIKE ? OR UPPER(STOCK_NAME) LIKE ?) ");
            String q = "%" + query.trim().toUpperCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (sector != null && !sector.trim().isEmpty() && !"ALL".equalsIgnoreCase(sector)) {
            sql.append("AND UPPER(SECTOR) = UPPER(?) ");
            params.add(sector.trim());
        }

        sql.append("ORDER BY SYMBOL ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStock(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching stocks", e);
        }
        return list;
    }

    public boolean addStock(Stock stock) throws SQLException {
        String sql = "INSERT INTO STOCK (SYMBOL, STOCK_NAME, COMPANY, SECTOR, CURRENT_PRICE, ACTIVE_STATUS) " +
                     "VALUES (UPPER(?), ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stock.getSymbol().trim());
            ps.setString(2, stock.getStockName().trim());
            ps.setString(3, stock.getCompany().trim());
            ps.setString(4, stock.getSector().trim());
            ps.setDouble(5, stock.getCurrentPrice());
            ps.setString(6, stock.getActiveStatus() != null ? stock.getActiveStatus() : "ACTIVE");
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStock(Stock stock) throws SQLException {
        String sql = "UPDATE STOCK SET STOCK_NAME = ?, COMPANY = ?, SECTOR = ?, CURRENT_PRICE = ?, " +
                     "ACTIVE_STATUS = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE STOCK_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stock.getStockName().trim());
            ps.setString(2, stock.getCompany().trim());
            ps.setString(3, stock.getSector().trim());
            ps.setDouble(4, stock.getCurrentPrice());
            ps.setString(5, stock.getActiveStatus());
            ps.setInt(6, stock.getStockId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setStockStatus(int stockId, String status) throws SQLException {
        String sql = "UPDATE STOCK SET ACTIVE_STATUS = ?, UPDATED_AT = CURRENT_TIMESTAMP WHERE STOCK_ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, stockId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countTotalStocks() {
        String sql = "SELECT COUNT(*) FROM STOCK WHERE ACTIVE_STATUS = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting stocks", e);
        }
        return 0;
    }

    public List<String> getDistinctSectors() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT SECTOR FROM STOCK WHERE ACTIVE_STATUS = 'ACTIVE' ORDER BY SECTOR ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("SECTOR"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching distinct sectors", e);
        }
        return list;
    }

    private Stock mapResultSetToStock(ResultSet rs) throws SQLException {
        Stock s = new Stock();
        s.setStockId(rs.getInt("STOCK_ID"));
        s.setSymbol(rs.getString("SYMBOL"));
        s.setStockName(rs.getString("STOCK_NAME"));
        s.setCompany(rs.getString("COMPANY"));
        s.setSector(rs.getString("SECTOR"));
        s.setCurrentPrice(rs.getDouble("CURRENT_PRICE"));
        s.setActiveStatus(rs.getString("ACTIVE_STATUS"));
        s.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
        return s;
    }
}
