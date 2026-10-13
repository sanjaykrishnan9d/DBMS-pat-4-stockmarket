package com.stockmarket.dao;

import com.stockmarket.config.DBConnection;
import com.stockmarket.model.PortfolioItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for comprehensive financial reports and mathematical analytics.
 */
public class ReportsDAO {
    private static final Logger LOGGER = Logger.getLogger(ReportsDAO.class.getName());

    public Map<String, Object> generateUserFinancialReport(int userId) {
        Map<String, Object> report = new LinkedHashMap<>();

        PortfolioDAO portfolioDAO = new PortfolioDAO();
        List<PortfolioItem> portfolioItems = portfolioDAO.getUserPortfolio(userId);
        Map<String, Object> portfolioTotals = portfolioDAO.getPortfolioTotals(userId);

        double totalPurchases = 0.0;
        double totalSales = 0.0;
        double totalDeposits = 0.0;
        int buyCount = 0;
        int sellCount = 0;
        int depositCount = 0;

        String txnSummarySql = 
            "SELECT TRANSACTION_TYPE, COUNT(*) as CNT, NVL(SUM(TOTAL_AMOUNT), 0) as TOTAL_SUM " +
            "FROM STOCK_TRANSACTION " +
            "WHERE USER_ID = ? AND STATUS = 'COMPLETED' " +
            "GROUP BY TRANSACTION_TYPE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(txnSummarySql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("TRANSACTION_TYPE");
                    int count = rs.getInt("CNT");
                    double sum = rs.getDouble("TOTAL_SUM");

                    if ("BUY".equalsIgnoreCase(type)) {
                        totalPurchases = sum;
                        buyCount = count;
                    } else if ("SELL".equalsIgnoreCase(type)) {
                        totalSales = sum;
                        sellCount = count;
                    } else if ("DEPOSIT".equalsIgnoreCase(type)) {
                        totalDeposits = sum;
                        depositCount = count;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error aggregating transactions for reports", e);
        }

        totalPurchases = Math.round(totalPurchases * 100.0) / 100.0;
        totalSales = Math.round(totalSales * 100.0) / 100.0;
        totalDeposits = Math.round(totalDeposits * 100.0) / 100.0;

        double currentPortfolioCost = (Double) portfolioTotals.getOrDefault("totalInvestment", 0.0);
        double currentPortfolioVal = (Double) portfolioTotals.getOrDefault("currentTotalValue", 0.0);
        double unrealizedPL = (Double) portfolioTotals.getOrDefault("unrealizedProfitLoss", 0.0);

        // Mathematical formulation of Realized Profit & Loss:
        // Realized P&L = Total Sales Proceeds - Cost Basis of Sold Shares
        // Where Cost Basis of Sold Shares = (Total Historical Purchases) - (Cost Basis of Current Remaining Holdings)
        double costOfSoldShares = Math.max(0.0, totalPurchases - currentPortfolioCost);
        double realizedPL = 0.0;
        if (totalSales > 0) {
            realizedPL = Math.round((totalSales - costOfSoldShares) * 100.0) / 100.0;
        }

        // Net P&L = Realized P&L + Unrealized P&L
        double netCombinedPL = Math.round((realizedPL + unrealizedPL) * 100.0) / 100.0;

        report.put("totalPurchases", totalPurchases);
        report.put("totalSales", totalSales);
        report.put("totalDeposits", totalDeposits);
        report.put("buyCount", buyCount);
        report.put("sellCount", sellCount);
        report.put("depositCount", depositCount);
        report.put("totalTransactions", buyCount + sellCount + depositCount);
        report.put("currentPortfolioCost", currentPortfolioCost);
        report.put("currentPortfolioVal", currentPortfolioVal);
        report.put("realizedProfitLoss", realizedPL);
        report.put("unrealizedProfitLoss", unrealizedPL);
        report.put("netCombinedProfitLoss", netCombinedPL);
        report.put("holdings", portfolioItems);

        return report;
    }
}
