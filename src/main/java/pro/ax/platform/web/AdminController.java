package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.AdminService;
import pro.ax.platform.service.StatsService;
import pro.ax.platform.web.dto.CompanySummaryDto;
import pro.ax.platform.web.dto.NeedDto;
import pro.ax.platform.web.dto.StatsDto;
import pro.ax.platform.web.dto.VerifyForm;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final StatsService statsService;
    private final AuthService authService;

    public AdminController(AdminService adminService, StatsService statsService, AuthService authService) {
        this.adminService = adminService;
        this.statsService = statsService;
        this.authService = authService;
    }

    @GetMapping("/pending-companies")
    public List<CompanySummaryDto> pending() {
        return adminService.companiesWithoutVerification();
    }

    @PatchMapping("/companies/{id}/verify")
    public CompanySummaryDto verify(@PathVariable Long id, @Valid @RequestBody VerifyForm form,
                                    HttpSession session) {
        User admin = authService.requireAdmin(session);
        return adminService.verify(admin, id, form.verified());
    }

    @GetMapping("/stats")
    public StatsDto stats() {
        return statsService.stats();
    }

    @GetMapping("/needs")
    public List<NeedDto> needs() {
        return adminService.recentNeeds();
    }
}
