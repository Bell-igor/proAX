package pro.ax.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Единая точка контроля доступа к REST API:
 * изменяющие запросы требуют входа, раздел /api/admin — роли администратора.
 */
@Component
public class ApiAccessInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_POST = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/register-person");
    private static final Set<String> PROTECTED_GET = Set.of(
            "/api/favorites", "/api/cabinet", "/api/threads", "/api/billing", "/api/legal/applications");

    private final AuthService authService;

    public ApiAccessInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            return true;
        }
        String method = request.getMethod();
        HttpSession session = request.getSession(false);

        if (path.startsWith("/api/admin")) {
            authService.requireAdmin(session);
            return true;
        }
        if (PROTECTED_GET.stream().anyMatch(path::startsWith)) {
            authService.requireUser(session);
            return true;
        }
        boolean mutating = !("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method));
        if (mutating && !PUBLIC_POST.contains(path)) {
            authService.requireUser(session);
        }
        return true;
    }
}
