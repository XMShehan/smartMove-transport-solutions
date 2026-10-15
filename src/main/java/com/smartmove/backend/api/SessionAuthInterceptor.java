package com.smartmove.backend.api;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Server-side role checks. Hiding a React page alone is not authorization.
 */
@Component
public class SessionAuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) return true;
        if (path.startsWith("/api/auth/") || path.equals("/api/health/databases")) return true;

        HttpSession session = request.getSession(false);
        Object raw = session == null ? null : session.getAttribute(AuthController.SESSION_USER);
        if (!(raw instanceof Map<?, ?> user)) {
            return deny(response, 401, "Please sign in to continue.");
        }

        String role = String.valueOf(user.get("role"));
        String method = request.getMethod().toUpperCase();

        if (path.startsWith("/api/passenger/")) {
            if (!"PASSENGER".equals(role)) return deny(response, 403, "Passenger access only.");
            return true;
        }

        boolean managerResource = path.startsWith("/api/managers") || path.startsWith("/api/vehicles")
            || path.startsWith("/api/drivers") || path.startsWith("/api/routes")
            || path.startsWith("/api/passengers") || path.startsWith("/api/technicians")
            || path.startsWith("/api/trips") || path.startsWith("/api/bookings")
            || path.startsWith("/api/payments") || path.startsWith("/api/maintenance")
            || path.startsWith("/api/feedback") || path.startsWith("/api/reports/");
        if (managerResource && !"MANAGER".equals(role)) {
            return deny(response, 403, "Manager access is required for this operation.");
        }

        // Single-manager coursework mode: preserve existing manager records and their
        // foreign-key relationships; profile updates use the profile endpoint instead.
        boolean managerAccountEndpoint = path.equals("/api/managers") || path.startsWith("/api/managers/");
        if (managerAccountEndpoint && !"GET".equals(method) && !"HEAD".equals(method)) {
            return deny(response, 403, "Manager account creation, editing and deletion are disabled. Update your own profile instead.");
        }

        // The management content console is manager-only for reads and writes.
        // Passengers use /api/passenger/announcements and /api/passenger/reviews.
        if (path.startsWith("/api/content/") && !"MANAGER".equals(role)) {
            return deny(response, 403, "Only managers can access the content and media console.");
        }

        return true;
    }

    private static boolean deny(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
        return false;
    }
}
