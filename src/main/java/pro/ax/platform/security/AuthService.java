package pro.ax.platform.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.UserRepository;

import java.util.Optional;

/** Аутентификация на серверных сессиях (JSESSIONID) без внешних security-библиотек. */
@Service
public class AuthService {

    public static final String SESSION_USER_ID = "proax.userId";

    private final UserRepository users;
    private final PasswordHasher hasher;

    public AuthService(UserRepository users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            throw ApiException.unauthorized("Неверный e-mail или пароль");
        }
        User user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> ApiException.unauthorized("Неверный e-mail или пароль"));
        if (!user.isEnabled() || !hasher.matches(rawPassword, user.getPasswordHash())) {
            throw ApiException.unauthorized("Неверный e-mail или пароль");
        }
        return user;
    }

    public void login(HttpSession session, User user) {
        session.setAttribute(SESSION_USER_ID, user.getId());
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> currentUser(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(SESSION_USER_ID);
        if (value instanceof Long userId) {
            return users.findById(userId).filter(User::isEnabled);
        }
        return Optional.empty();
    }

    public User requireUser(HttpSession session) {
        return currentUser(session)
                .orElseThrow(() -> ApiException.unauthorized("Для этого действия нужно войти в аккаунт"));
    }

    public User requireAdmin(HttpSession session) {
        User user = requireUser(session);
        if (!user.isAdmin()) {
            throw ApiException.forbidden("Раздел доступен только администратору площадки");
        }
        return user;
    }
}
