package com.stockmarket.servlet;

import com.stockmarket.dao.StockDAO;
import com.stockmarket.model.Stock;
import com.stockmarket.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/stocks")
public class StockServlet extends HttpServlet {

    private final StockDAO stockDAO = new StockDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String idParam = request.getParameter("id");
        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                int stockId = Integer.parseInt(idParam.trim());
                Stock stock = stockDAO.getStockById(stockId);
                if (stock == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write(JsonUtil.errorResponse("Stock with ID " + stockId + " not found."));
                    return;
                }
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(JsonUtil.successResponse("Stock found", stock));
                return;
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(JsonUtil.errorResponse("Invalid stock ID format."));
                return;
            }
        }

        String query = request.getParameter("search");
        String sector = request.getParameter("sector");

        List<Stock> stocks;
        if ((query != null && !query.trim().isEmpty()) || (sector != null && !sector.trim().isEmpty())) {
            stocks = stockDAO.searchStocks(query, sector);
        } else {
            stocks = stockDAO.getAllActiveStocks();
        }

        List<String> sectors = stockDAO.getDistinctSectors();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stocks", stocks);
        result.put("sectors", sectors);
        result.put("totalCount", stocks.size());

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("Stocks retrieved", result));
    }
}
