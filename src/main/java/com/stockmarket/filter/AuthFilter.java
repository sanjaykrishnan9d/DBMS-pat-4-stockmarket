package com.stockmarket.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Authentication filter protecting protected HTML pages and API endpoints.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String relativePath = path.substring(contextPath.length());

        // Static resources and public pages bypass filter
        if (isPublicPath(relativePath)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        boolean loggedIn = (session != null && session.getAttribute("user") != null);

        if (!loggedIn) {
            if (relativePath.startsWith("/api/")) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.setCharacterEncoding("UTF-8");
                httpResponse.getWriter().write("{\"success\":false,\"message\":\"Unauthorized. Please log in first.\"}");
                return;
            } else {
                httpResponse.sendRedirect(contextPath + "/login.html");
                return;
            }
        }

        // Additional admin route protection
        if (relativePath.startsWith("/api/admin/")) {
            com.stockmarket.model.User user = (com.stockmarket.model.User) session.getAttribute("user");
            if (user == null || !user.isAdmin()) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json");
                httpResponse.setCharacterEncoding("UTF-8");
                httpResponse.getWriter().write("{\"success\":false,\"message\":\"Access forbidden. Administrator role required.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (path.equals("/") || path.equals("/index.html") || path.equals("/login.html") || path.equals("/register.html")) {
            return true;
        }
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.startsWith("/favicon")) {
            return true;
        }
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register") || path.equals("/api/auth/status")) {
            return true;
        }
        return false;
    }

    @Override
    public void destroy() {}
}
