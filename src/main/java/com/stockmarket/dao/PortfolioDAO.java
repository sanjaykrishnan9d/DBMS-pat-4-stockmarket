package com.stockmarket.dao;

import com.stockmarket.config.DBConnection;
import com.stockmarket.model.PortfolioItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for PORTFOLIO holdings and portfolio value calculations.
 */
public class PortfolioDAO {
    private static final Logger LOGGER = Logger.getLogger(PortfolioDAO.class.getName());

    public List<PortfolioItem> getUserPortfolio(int userId) {
        List<PortfolioItem> list = new ArrayList<>();
        String sql = "SELECT p.PORTFOLIO_ID, p.USER_ID, p.STOCK_ID, p.QUANTITY, p.AVERAGE_BUY_PRICE, p.UPDATED_AT, " +
                     "s.SYMBOL, s.COMPANY, s.SECTOR, s.CURRENT_PRICE " +
                     "FROM PORTFOLIO p " +
                     "JOIN STOCK s ON p.STOCK_ID = s.STOCK_ID " +
                     "WHERE p.USER_ID = ? AND p.QUANTITY > 0 " +
                     "ORDER BY s.SYMBOL ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PortfolioItem item = new PortfolioItem();
                    item.setPortfolioId(rs.getInt("PORTFOLIO_ID"));
                    item.setUserId(rs.getInt("USER_ID"));
                    item.setStockId(rs.getInt("STOCK_ID"));
                    item.setSymbol(rs.getString("SYMBOL"));
                    item.setCompany(rs.getString("COMPANY"));
                    item.setSector(rs.getString("SECTOR"));
                    item.setAverageBuyPrice(rs.getDouble("AVERAGE_BUY_PRICE"));
                    item.setCurrentPrice(rs.getDouble("CURRENT_PRICE"));
                    item.setQuantity(rs.getInt("QUANTITY")); // calculates metrics
                    item.setUpdatedAt(rs.getTimestamp("UPDATED_AT"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching user portfolio for user " + userId, e);
        }
        return list;
    }

    /**
     * Calculates user aggregate investment, current valuation, and unrealized profit/loss.
     */
    public Map<String, Object> getPortfolioTotals(int userId) {
        Map<String, Object> totals = new HashMap<>();
        List<PortfolioItem> items = getUserPortfolio(userId);

        double totalInvestment = 0.0;
        double currentTotalValue = 0.0;
        int totalShares = 0;

        for (PortfolioItem item : items) {
            totalInvestment += item.getTotalCost();
            currentTotalValue += item.getCurrentValue();
            totalShares += item.getQuantity();
        }

        totalInvestment = Math.round(totalInvestment * 100.0) / 100.0;
        currentTotalValue = Math.round(currentTotalValue * 100.0) / 100.0;
        double profitLoss = Math.round((currentTotalValue - totalInvestment) * 100.0) / 100.0;
        double profitLossPercent = totalInvestment > 0 
            ? Math.round(((profitLoss / totalInvestment) * 100.0) * 100.0) / 100.0 
            : 0.0;

        totals.put("totalInvestment", totalInvestment);
        totals.put("currentTotalValue", currentTotalValue);
        totals.put("unrealizedProfitLoss", profitLoss);
        totals.put("profitLossPercent", profitLossPercent);
        totals.put("totalShares", totalShares);
        totals.put("distinctHoldingsCount", items.size());

        return totals;
    }
}
