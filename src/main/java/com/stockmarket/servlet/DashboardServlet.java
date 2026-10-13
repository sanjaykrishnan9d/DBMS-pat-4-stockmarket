package com.stockmarket.servlet;

import com.stockmarket.dao.AccountDAO;
import com.stockmarket.dao.PortfolioDAO;
import com.stockmarket.dao.StockDAO;
import com.stockmarket.dao.TransactionDAO;
import com.stockmarket.model.StockTransaction;
import com.stockmarket.model.User;
import com.stockmarket.model.UserAccount;
import com.stockmarket.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    private final StockDAO stockDAO = new StockDAO();
    private final PortfolioDAO portfolioDAO = new PortfolioDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.errorResponse("Unauthorized. Please log in."));
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        // 1. Calculate and fetch live values from Oracle DB
        int availableStocksCount = stockDAO.countTotalStocks();
        UserAccount account = accountDAO.getAccountByUserId(userId);
        double cashBalance = (account != null) ? account.getCashBalance() : 0.0;

        Map<String, Object> portfolioTotals = portfolioDAO.getPortfolioTotals(userId);
        double totalInvestment = (Double) portfolioTotals.getOrDefault("totalInvestment", 0.0);
        double currentPortfolioValue = (Double) portfolioTotals.getOrDefault("currentTotalValue", 0.0);
        double profitLoss = (Double) portfolioTotals.getOrDefault("unrealizedProfitLoss", 0.0);
        double profitLossPercent = (Double) portfolioTotals.getOrDefault("profitLossPercent", 0.0);

        int transactionCount = transactionDAO.countUserTransactions(userId);
        List<StockTransaction> recentTransactions = transactionDAO.getUserTransactions(userId, null, null, 5);

        // 2. Prepare payload
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("userName", user.getFullName());
        data.put("userRole", user.getRole());
        data.put("cashBalance", cashBalance);
        data.put("availableStocksCount", availableStocksCount);
        data.put("totalInvestment", totalInvestment);
        data.put("currentPortfolioValue", currentPortfolioValue);
        data.put("profitLoss", profitLoss);
        data.put("profitLossPercent", profitLossPercent);
        data.put("transactionCount", transactionCount);
        data.put("recentTransactions", recentTransactions);
        data.put("holdings", portfolioDAO.getUserPortfolio(userId));

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("Dashboard data loaded", data));
    }
}
