package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.BillingService;
import pro.ax.platform.web.dto.BillingDto;
import pro.ax.platform.web.dto.UpgradeForm;

/** Тарифы компании: подключение и история платежей (демо, без платёжного шлюза). */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final AuthService authService;

    public BillingController(BillingService billingService, AuthService authService) {
        this.billingService = billingService;
        this.authService = authService;
    }

    @GetMapping
    public BillingDto current(HttpSession session) {
        User user = authService.requireUser(session);
        return billingService.current(user);
    }

    @PostMapping("/upgrade")
    public BillingDto upgrade(@Valid @RequestBody UpgradeForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return billingService.upgrade(user, form.plan());
    }
}
