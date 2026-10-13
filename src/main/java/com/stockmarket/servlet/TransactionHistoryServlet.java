package com.stockmarket.servlet;

import com.stockmarket.dao.TransactionDAO;
import com.stockmarket.model.StockTransaction;
import com.stockmarket.model.User;
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

@WebServlet("/api/transactions/history")
public class TransactionHistoryServlet extends HttpServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();

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

        String symbolFilter = request.getParameter("symbol");
        String typeFilter = request.getParameter("type");
        String limitParam = request.getParameter("limit");
        int limit = 100;
        if (limitParam != null && !limitParam.trim().isEmpty()) {
            try {
                limit = Integer.parseInt(limitParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        List<StockTransaction> list = transactionDAO.getUserTransactions(userId, symbolFilter, typeFilter, limit);
        int totalCount = transactionDAO.countUserTransactions(userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactions", list);
        data.put("totalCount", totalCount);
        data.put("filteredCount", list.size());

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("Transaction history loaded", data));
    }
}
