package com.stockmarket.servlet;

import com.stockmarket.dao.UserDAO;
import com.stockmarket.model.User;
import com.stockmarket.util.JsonUtil;
import com.stockmarket.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (fullName == null || email == null || username == null || password == null) {
            String body = JsonUtil.readRequestBody(request.getReader());
            fullName = JsonUtil.extractString(body, "fullName");
            email = JsonUtil.extractString(body, "email");
            phone = JsonUtil.extractString(body, "phone");
            username = JsonUtil.extractString(body, "username");
            password = JsonUtil.extractString(body, "password");
            confirmPassword = JsonUtil.extractString(body, "confirmPassword");
        }

        // 1. Mandatory field validation
        if (isEmpty(fullName) || isEmpty(email) || isEmpty(phone) || 
            isEmpty(username) || isEmpty(password) || isEmpty(confirmPassword)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("All fields are mandatory. Please fill in all fields."));
            return;
        }

        fullName = fullName.trim();
        email = email.trim();
        phone = phone.trim();
        username = username.trim();

        // 2. Email format validation
        if (!EMAIL_REGEX.matcher(email).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Invalid email format. Please provide a valid email address."));
            return;
        }

        // 3. Password matching and strength validation
        if (!password.equals(confirmPassword)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Password and confirmation password do not match."));
            return;
        }

        if (password.length() < 6) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Password must be at least 6 characters in length."));
            return;
        }

        if (username.length() < 3) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtil.errorResponse("Username must be at least 3 characters in length."));
            return;
        }

        // 4. Check uniqueness in database
        if (userDAO.usernameExists(username)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write(JsonUtil.errorResponse("Username '" + username + "' is already taken. Please choose another."));
            return;
        }

        if (userDAO.emailExists(email)) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().write(JsonUtil.errorResponse("An account with email '" + email + "' already exists."));
            return;
        }

        // 5. BCrypt password hashing & account persistence
        try {
            String hash = PasswordUtil.hashPassword(password);
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setUsername(username);
            user.setPasswordHash(hash);
            user.setRole("USER");
            user.setAccountStatus("ACTIVE");

            // Grant starting virtual balance of $10,000 for realistic simulation
            boolean created = userDAO.createUser(user, 10000.00);
            if (created) {
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write(JsonUtil.successResponse("Registration successful! You may now log in.", null));
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(JsonUtil.errorResponse("Failed to create user account. Please try again later."));
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtil.errorResponse("Database error during registration: " + e.getMessage()));
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
