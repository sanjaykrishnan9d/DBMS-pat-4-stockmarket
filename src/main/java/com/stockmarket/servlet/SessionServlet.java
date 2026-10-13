package com.stockmarket.servlet;

import com.stockmarket.dao.AccountDAO;
import com.stockmarket.dao.UserDAO;
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
import java.util.Map;

@WebServlet("/api/auth/me")
public class SessionServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.errorResponse("Not logged in."));
            return;
        }

        User sessionUser = (User) session.getAttribute("user");
        User freshUser = userDAO.findById(sessionUser.getUserId());
        if (freshUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.errorResponse("User record not found."));
            return;
        }

        UserAccount account = accountDAO.getAccountByUserId(freshUser.getUserId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", freshUser.getUserId());
        data.put("fullName", freshUser.getFullName());
        data.put("username", freshUser.getUsername());
        data.put("email", freshUser.getEmail());
        data.put("phone", freshUser.getPhone());
        data.put("role", freshUser.getRole());
        data.put("cashBalance", account != null ? account.getCashBalance() : 0.0);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("Session active", data));
    }
}
