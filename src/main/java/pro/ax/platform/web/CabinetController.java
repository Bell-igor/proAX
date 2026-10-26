package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.CabinetService;
import pro.ax.platform.service.FavoriteService;
import pro.ax.platform.web.dto.CabinetDto;
import pro.ax.platform.web.dto.CompanySummaryDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CabinetController {

    private final CabinetService cabinetService;
    private final FavoriteService favoriteService;
    private final AuthService authService;

    public CabinetController(CabinetService cabinetService, FavoriteService favoriteService,
                             AuthService authService) {
        this.cabinetService = cabinetService;
        this.favoriteService = favoriteService;
        this.authService = authService;
    }

    @GetMapping("/cabinet")
    public CabinetDto cabinet(HttpSession session) {
        User user = authService.requireUser(session);
        return cabinetService.load(user);
    }

    @GetMapping("/favorites")
    public List<CompanySummaryDto> favorites(HttpSession session) {
        User user = authService.requireUser(session);
        return favoriteService.list(user);
    }

    @PostMapping("/favorites/{companyId}")
    public Map<String, Object> addFavorite(@PathVariable Long companyId, HttpSession session) {
        User user = authService.requireUser(session);
        favoriteService.add(user, companyId);
        return Map.of("ok", true, "favorite", true);
    }

    @DeleteMapping("/favorites/{companyId}")
    public Map<String, Object> removeFavorite(@PathVariable Long companyId, HttpSession session) {
        User user = authService.requireUser(session);
        favoriteService.remove(user, companyId);
        return Map.of("ok", true, "favorite", false);
    }
}
