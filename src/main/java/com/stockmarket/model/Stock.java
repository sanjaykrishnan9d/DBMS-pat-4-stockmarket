package com.stockmarket.model;

import java.sql.Timestamp;

public class Stock {
    private int stockId;
    private String symbol;
    private String stockName;
    private String company;
    private String sector;
    private double currentPrice;
    private String activeStatus; // 'ACTIVE', 'INACTIVE'
    private Timestamp updatedAt;

    public Stock() {}

    public Stock(int stockId, String symbol, String stockName, String company, String sector, double currentPrice, String activeStatus, Timestamp updatedAt) {
        this.stockId = stockId;
        this.symbol = symbol;
        this.stockName = stockName;
        this.company = company;
        this.sector = sector;
        this.currentPrice = currentPrice;
        this.activeStatus = activeStatus;
        this.updatedAt = updatedAt;
    }

    public int getStockId() { return stockId; }
    public void setStockId(int stockId) { this.stockId = stockId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getActiveStatus() { return activeStatus; }
    public void setActiveStatus(String activeStatus) { this.activeStatus = activeStatus; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(activeStatus);
    }
}
