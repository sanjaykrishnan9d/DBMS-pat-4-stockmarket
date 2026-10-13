package com.stockmarket.model;

import java.sql.Timestamp;

public class StockTransaction {
    private long transactionId;
    private int userId;
    private Integer stockId;
    private String symbol;
    private String company;
    private String transactionType; // 'BUY', 'SELL', 'DEPOSIT'
    private int quantity;
    private double pricePerShare;
    private double totalAmount;
    private Timestamp transactionDate;
    private String status; // 'COMPLETED', 'FAILED', 'CANCELLED'

    public StockTransaction() {}

    public StockTransaction(long transactionId, int userId, Integer stockId, String symbol, String company,
                            String transactionType, int quantity, double pricePerShare, double totalAmount,
                            Timestamp transactionDate, String status) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.stockId = stockId;
        this.symbol = symbol;
        this.company = company;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.totalAmount = totalAmount;
        this.transactionDate = transactionDate;
        this.status = status;
    }

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Integer getStockId() { return stockId; }
    public void setStockId(Integer stockId) { this.stockId = stockId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPricePerShare() { return pricePerShare; }
    public void setPricePerShare(double pricePerShare) { this.pricePerShare = pricePerShare; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public Timestamp getTransactionDate() { return transactionDate; }
    public void setTransactionDate(Timestamp transactionDate) { this.transactionDate = transactionDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
