package com.stockmarket.servlet;

import com.stockmarket.dao.ReportsDAO;
import com.stockmarket.model.User;
import com.stockmarket.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/reports")
public class ReportsServlet extends HttpServlet {

    private final ReportsDAO reportsDAO = new ReportsDAO();

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

        Map<String, Object> report = reportsDAO.generateUserFinancialReport(userId);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("Financial report generated", report));
    }
}
