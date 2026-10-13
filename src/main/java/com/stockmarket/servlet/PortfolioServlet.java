package com.stockmarket.servlet;

import com.stockmarket.dao.AccountDAO;
import com.stockmarket.dao.PortfolioDAO;
import com.stockmarket.model.PortfolioItem;
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

@WebServlet("/api/portfolio")
public class PortfolioServlet extends HttpServlet {

    private final PortfolioDAO portfolioDAO = new PortfolioDAO();
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

        List<PortfolioItem> holdings = portfolioDAO.getUserPortfolio(userId);
        Map<String, Object> totals = portfolioDAO.getPortfolioTotals(userId);
        UserAccount account = accountDAO.getAccountByUserId(userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("holdings", holdings);
        data.put("summary", totals);
        data.put("cashBalance", account != null ? account.getCashBalance() : 0.0);
        data.put("totalNetWorth", (account != null ? account.getCashBalance() : 0.0) + (Double) totals.getOrDefault("currentTotalValue", 0.0));

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("Portfolio holdings loaded", data));
    }
}
