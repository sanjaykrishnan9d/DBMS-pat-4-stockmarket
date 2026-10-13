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

@WebServlet("/api/transactions")
public class TransactionServlet extends HttpServlet {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.errorResponse("Unauthorized. Please log in to trade."));
            return;
        }

        User user = (User) session.getAttribute("user");
        int userId = user.getUserId();

        String stockIdStr = request.getParameter("stockId");
        String type = request.getParameter("type");
        String quantityStr = request.getParameter("quantity");

        if (stockIdStr == null || type == null || quantityStr == null) {
            String body = JsonUtil.readRequestBody(request.getReader());
            stockIdStr = JsonUtil.extractString(body, "stockId");
            type = JsonUtil.extractString(body, "type");
            quantityStr = JsonUtil.extractString(body, "quantity");
        }

        if (stockIdStr == null || type == null || quantityStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Missing required transaction fields: stockId, type, quantity."));
            return;
        }

        int stockId;
        int quantity;
        try {
            stockId = Integer.parseInt(stockIdStr.trim());
            quantity = Integer.parseInt(quantityStr.trim());
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Invalid number format for stock ID or quantity."));
            return;
        }

        if (quantity <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Quantity must be a positive whole integer greater than zero."));
            return;
        }

        type = type.trim().toUpperCase();
        if (!"BUY".equals(type) && !"SELL".equals(type)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Invalid transaction type. Must be BUY or SELL."));
            return;
        }

        try {
            StockTransaction receipt;
            if ("BUY".equals(type)) {
                receipt = transactionDAO.executeBuy(userId, stockId, quantity);
            } else {
                receipt = transactionDAO.executeSell(userId, stockId, quantity);
            }

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtil.successResponse("Transaction executed successfully!", receipt));
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse(e.getMessage()));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtil.errorResponse("Transaction processing failed: " + e.getMessage()));
        }
    }
}
