package com.stockmarket.servlet;

import com.stockmarket.dao.StockDAO;
import com.stockmarket.model.Stock;
import com.stockmarket.model.User;
import com.stockmarket.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/admin/stocks")
public class AdminStockServlet extends HttpServlet {

    private final StockDAO stockDAO = new StockDAO();

    private boolean checkAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.errorResponse("Unauthorized. Please log in."));
            return false;
        }
        User user = (User) session.getAttribute("user");
        if (!user.isAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(JsonUtil.errorResponse("Access denied. Admin privileges required."));
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!checkAdmin(request, response)) return;

        List<Stock> stocks = stockDAO.getAllStocksAdmin();
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("All stocks retrieved for admin", stocks));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!checkAdmin(request, response)) return;

        String action = request.getParameter("action");
        String body = "";
        if (action == null) {
            body = JsonUtil.readRequestBody(request.getReader());
            action = JsonUtil.extractString(body, "action");
        }

        if ("toggleStatus".equalsIgnoreCase(action)) {
            String idStr = request.getParameter("stockId");
            String status = request.getParameter("status");
            if (idStr == null) {
                idStr = JsonUtil.extractString(body, "stockId");
                status = JsonUtil.extractString(body, "status");
            }
            try {
                int stockId = Integer.parseInt(idStr);
                boolean ok = stockDAO.setStockStatus(stockId, status != null ? status : "ACTIVE");
                if (ok) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write(JsonUtil.successResponse("Stock status updated to " + status, null));
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write(JsonUtil.errorResponse("Failed to update status."));
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(JsonUtil.errorResponse("Error: " + e.getMessage()));
            }
            return;
        }

        if ("update".equalsIgnoreCase(action)) {
            handleUpdate(request, response, body);
            return;
        }

        // Otherwise action is create
        String symbol = request.getParameter("symbol");
        String stockName = request.getParameter("stockName");
        String company = request.getParameter("company");
        String sector = request.getParameter("sector");
        String priceStr = request.getParameter("currentPrice");

        if (symbol == null) {
            symbol = JsonUtil.extractString(body, "symbol");
            stockName = JsonUtil.extractString(body, "stockName");
            company = JsonUtil.extractString(body, "company");
            sector = JsonUtil.extractString(body, "sector");
            priceStr = JsonUtil.extractString(body, "currentPrice");
        }

        if (symbol == null || stockName == null || company == null || sector == null || priceStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Missing required fields: symbol, stockName, company, sector, currentPrice."));
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(JsonUtil.errorResponse("Price must be greater than zero."));
                return;
            }

            if (stockDAO.getStockBySymbol(symbol) != null) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write(JsonUtil.errorResponse("A stock with symbol " + symbol.toUpperCase() + " already exists."));
                return;
            }

            Stock s = new Stock();
            s.setSymbol(symbol.toUpperCase());
            s.setStockName(stockName);
            s.setCompany(company);
            s.setSector(sector);
            s.setCurrentPrice(price);
            s.setActiveStatus("ACTIVE");

            boolean ok = stockDAO.addStock(s);
            if (ok) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write(JsonUtil.successResponse("Stock created successfully!", s));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(JsonUtil.errorResponse("Failed to insert stock."));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Invalid price number format."));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtil.errorResponse("Database error: " + e.getMessage()));
        }
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response, String body) throws IOException {
        String idStr = request.getParameter("stockId");
        String stockName = request.getParameter("stockName");
        String company = request.getParameter("company");
        String sector = request.getParameter("sector");
        String priceStr = request.getParameter("currentPrice");
        String activeStatus = request.getParameter("activeStatus");

        if (idStr == null) {
            idStr = JsonUtil.extractString(body, "stockId");
            stockName = JsonUtil.extractString(body, "stockName");
            company = JsonUtil.extractString(body, "company");
            sector = JsonUtil.extractString(body, "sector");
            priceStr = JsonUtil.extractString(body, "currentPrice");
            activeStatus = JsonUtil.extractString(body, "activeStatus");
        }

        try {
            int stockId = Integer.parseInt(idStr);
            double price = Double.parseDouble(priceStr);

            Stock s = stockDAO.getStockById(stockId);
            if (s == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(JsonUtil.errorResponse("Stock not found."));
                return;
            }

            s.setStockName(stockName);
            s.setCompany(company);
            s.setSector(sector);
            s.setCurrentPrice(price);
            if (activeStatus != null) s.setActiveStatus(activeStatus);

            boolean ok = stockDAO.updateStock(s);
            if (ok) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(JsonUtil.successResponse("Stock updated successfully!", s));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(JsonUtil.errorResponse("Failed to update stock."));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Error updating stock: " + e.getMessage()));
        }
    }
}
