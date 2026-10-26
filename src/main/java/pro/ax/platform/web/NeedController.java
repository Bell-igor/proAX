package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.NeedService;
import pro.ax.platform.service.ProposalService;
import pro.ax.platform.web.dto.NeedDto;
import pro.ax.platform.web.dto.NeedForm;
import pro.ax.platform.web.dto.PageDto;
import pro.ax.platform.web.dto.ProposalDto;
import pro.ax.platform.web.dto.ProposalForm;
import pro.ax.platform.web.dto.StatusForm;

import java.util.List;
import java.util.Map;

/** Заявки и отклики: основной «рыночный» контур площадки. */
@RestController
@RequestMapping("/api")
public class NeedController {

    private final NeedService needService;
    private final ProposalService proposalService;
    private final AuthService authService;

    public NeedController(NeedService needService, ProposalService proposalService, AuthService authService) {
        this.needService = needService;
        this.proposalService = proposalService;
        this.authService = authService;
    }

    @GetMapping("/needs")
    public PageDto<NeedDto> search(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String industry,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String city,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size,
                                   HttpSession session) {
        return needService.search(q, industry, status, city, page, size, viewer(session));
    }

    @GetMapping("/needs/latest")
    public List<NeedDto> latest() {
        return needService.latest();
    }

    @GetMapping("/needs/{id}")
    public NeedDto detail(@PathVariable Long id, HttpSession session) {
        return needService.detail(id, viewer(session));
    }

    @PostMapping("/needs")
    public NeedDto create(@Valid @RequestBody NeedForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return needService.create(user, form);
    }

    @PatchMapping("/needs/{id}/status")
    public NeedDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return needService.changeStatus(user, id, form.status());
    }

    @DeleteMapping("/needs/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        needService.delete(user, id);
        return Map.of("ok", true);
    }

    @GetMapping("/needs/{id}/proposals")
    public List<ProposalDto> proposals(@PathVariable Long id, HttpSession session) {
        return proposalService.forNeed(id, authService.currentUser(session).orElse(null));
    }

    @PostMapping("/needs/{id}/proposals")
    public ProposalDto respond(@PathVariable Long id, @Valid @RequestBody ProposalForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return proposalService.create(user, id, form);
    }

    @PatchMapping("/proposals/{id}")
    public ProposalDto changeProposalStatus(@PathVariable Long id, @Valid @RequestBody StatusForm form,
                                            HttpSession session) {
        User user = authService.requireUser(session);
        return proposalService.changeStatus(user, id, form.status());
    }

    private User viewer(HttpSession session) {
        return authService.currentUser(session).orElse(null);
    }
}
