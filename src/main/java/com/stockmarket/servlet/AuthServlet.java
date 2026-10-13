package com.stockmarket.servlet;

import com.stockmarket.dao.AccountDAO;
import com.stockmarket.dao.UserDAO;
import com.stockmarket.model.User;
import com.stockmarket.model.UserAccount;
import com.stockmarket.util.JsonUtil;
import com.stockmarket.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/auth/login")
public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final AccountDAO accountDAO = new AccountDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Support JSON request body if form parameters are null
        if (username == null || password == null) {
            String body = JsonUtil.readRequestBody(request.getReader());
            username = JsonUtil.extractString(body, "username");
            password = JsonUtil.extractString(body, "password");
        }

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Username and password are required."));
            return;
        }

        username = username.trim();

        // 1. Fetch user from Oracle DB
        User user = userDAO.findByUsername(username);
        if (user == null) {
            // Also attempt email login
            user = userDAO.findByEmail(username);
        }

        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.errorResponse("Invalid username or password."));
            return;
        }

        // 2. Check account status
        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(JsonUtil.errorResponse("Account is suspended or closed. Please contact administrator."));
            return;
        }

        // 3. Verify BCrypt password
        boolean valid = PasswordUtil.verifyPassword(password, user.getPasswordHash());
        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(JsonUtil.errorResponse("Invalid username or password."));
            return;
        }

        // 4. Secure Session Management: Invalidate old session and create fresh session
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());

        // 5. Build user response (excluding password hash)
        UserAccount account = accountDAO.getAccountByUserId(user.getUserId());
        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("userId", user.getUserId());
        userData.put("fullName", user.getFullName());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("phone", user.getPhone());
        userData.put("role", user.getRole());
        userData.put("cashBalance", account != null ? account.getCashBalance() : 0.0);

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.successResponse("Login successful!", userData));
    }
}
