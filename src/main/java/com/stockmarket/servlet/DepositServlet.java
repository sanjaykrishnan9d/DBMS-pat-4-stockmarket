package com.stockmarket.servlet;

import com.stockmarket.dao.AccountDAO;
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
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/account/deposit")
public class DepositServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
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

        String amountStr = request.getParameter("amount");
        if (amountStr == null) {
            String body = JsonUtil.readRequestBody(request.getReader());
            amountStr = JsonUtil.extractString(body, "amount");
        }

        if (amountStr == null || amountStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Please specify an amount to deposit."));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim());
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Invalid numerical amount."));
            return;
        }

        if (amount <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Deposit amount must be greater than zero."));
            return;
        }

        if (amount > 1_000_000) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Maximum single deposit limit is $1,000,000."));
            return;
        }

        try {
            boolean success = accountDAO.addDeposit(userId, amount);
            if (success) {
                UserAccount account = accountDAO.getAccountByUserId(userId);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("depositedAmount", amount);
                data.put("newCashBalance", account != null ? account.getCashBalance() : 0.0);
                data.put("isSimulated", true);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(JsonUtil.successResponse(
                    String.format("Successfully deposited simulated funds of $%.2f into your trading account.", amount),
                    data
                ));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(JsonUtil.errorResponse("Failed to deposit funds."));
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtil.errorResponse("Database error processing deposit: " + e.getMessage()));
        }
    }
}
