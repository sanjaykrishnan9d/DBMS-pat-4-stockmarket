package com.stockmarket.model;

import java.sql.Timestamp;

public class PortfolioItem {
    private int portfolioId;
    private int userId;
    private int stockId;
    private String symbol;
    private String company;
    private String sector;
    private int quantity;
    private double averageBuyPrice;
    private double currentPrice;
    private double totalCost;
    private double currentValue;
    private double unrealizedProfitLoss;
    private double profitLossPercent;
    private Timestamp updatedAt;

    public PortfolioItem() {}

    public int getPortfolioId() { return portfolioId; }
    public void setPortfolioId(int portfolioId) { this.portfolioId = portfolioId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getStockId() { return stockId; }
    public void setStockId(int stockId) { this.stockId = stockId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        calculateMetrics();
    }

    public double getAverageBuyPrice() { return averageBuyPrice; }
    public void setAverageBuyPrice(double averageBuyPrice) {
        this.averageBuyPrice = averageBuyPrice;
        calculateMetrics();
    }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
        calculateMetrics();
    }

    public double getTotalCost() { return totalCost; }
    public double getCurrentValue() { return currentValue; }
    public double getUnrealizedProfitLoss() { return unrealizedProfitLoss; }
    public double getProfitLossPercent() { return profitLossPercent; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public void calculateMetrics() {
        this.totalCost = Math.round(this.quantity * this.averageBuyPrice * 100.0) / 100.0;
        this.currentValue = Math.round(this.quantity * this.currentPrice * 100.0) / 100.0;
        this.unrealizedProfitLoss = Math.round((this.currentValue - this.totalCost) * 100.0) / 100.0;
        if (this.totalCost > 0) {
            this.profitLossPercent = Math.round(((this.unrealizedProfitLoss / this.totalCost) * 100.0) * 100.0) / 100.0;
        } else {
            this.profitLossPercent = 0.0;
        }
    }
}
