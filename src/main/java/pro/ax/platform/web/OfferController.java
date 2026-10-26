package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.OfferService;
import pro.ax.platform.web.dto.OfferDto;
import pro.ax.platform.web.dto.OfferForm;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OfferController {

    private final OfferService offerService;
    private final AuthService authService;

    public OfferController(OfferService offerService, AuthService authService) {
        this.offerService = offerService;
        this.authService = authService;
    }

    @GetMapping("/companies/{companyId}/offers")
    public List<OfferDto> forCompany(@PathVariable Long companyId, HttpSession session) {
        return offerService.forCompany(companyId, authService.currentUser(session).orElse(null));
    }

    @GetMapping("/offers/latest")
    public List<OfferDto> latest() {
        return offerService.latest();
    }

    @PostMapping("/offers")
    public OfferDto create(@Valid @RequestBody OfferForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return offerService.create(user, form);
    }

    @PutMapping("/offers/{id}")
    public OfferDto update(@PathVariable Long id, @Valid @RequestBody OfferForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return offerService.update(user, id, form);
    }

    @PatchMapping("/offers/{id}/active")
    public OfferDto toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body, HttpSession session) {
        User user = authService.requireUser(session);
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return offerService.changeActive(user, id, active);
    }

    @DeleteMapping("/offers/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        offerService.delete(user, id);
        return Map.of("ok", true);
    }
}
