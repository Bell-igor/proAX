package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.LegalApplicationService;
import pro.ax.platform.service.LegalCatalogService;
import pro.ax.platform.web.dto.CommentForm;
import pro.ax.platform.web.dto.ConfirmCodeForm;
import pro.ax.platform.web.dto.DocumentDto;
import pro.ax.platform.web.dto.LegalApplicationDto;
import pro.ax.platform.web.dto.LegalApplicationForm;
import pro.ax.platform.web.dto.LegalServiceDto;
import pro.ax.platform.web.dto.ScheduleForm;

import java.util.List;
import java.util.Map;

/** Юридические услуги: каталог исполнителей и заявки на оформление статуса. */
@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private final LegalCatalogService catalogService;
    private final LegalApplicationService applicationService;
    private final AuthService authService;

    public LegalController(LegalCatalogService catalogService, LegalApplicationService applicationService,
                           AuthService authService) {
        this.catalogService = catalogService;
        this.applicationService = applicationService;
        this.authService = authService;
    }

    @GetMapping("/services")
    public List<LegalServiceDto> services(@RequestParam(required = false) String target,
                                          @RequestParam(required = false) String city,
                                          @RequestParam(required = false, defaultValue = "default") String sort,
                                          @RequestParam(required = false) String q,
                                          HttpSession session) {
        User viewer = authService.currentUser(session).orElse(null);
        return catalogService.search(target, city, sort, q, viewer);
    }

    @GetMapping("/statuses")
    public Map<String, String> statuses() {
        return catalogService.targetStatuses();
    }

    @GetMapping("/providers/{slug}/services")
    public List<LegalServiceDto> providerServices(@PathVariable String slug,
                                                  @RequestParam(required = false) String city) {
        return catalogService.forProvider(slug, city);
    }

    @PostMapping("/applications")
    public LegalApplicationDto create(@Valid @RequestBody LegalApplicationForm form, HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.create(user, form);
    }

    @GetMapping("/applications")
    public List<LegalApplicationDto> myApplications(HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.myApplications(user);
    }

    @GetMapping("/applications/incoming")
    public List<LegalApplicationDto> incoming(HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.incoming(user);
    }

    @GetMapping("/applications/{id}")
    public LegalApplicationDto detail(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.detail(user, id);
    }

    @PostMapping(value = "/applications/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDto uploadDocument(@PathVariable Long id,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "title", required = false) String title,
                                      HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.uploadDocument(user, id, title, file);
    }

    @DeleteMapping("/applications/{id}/documents/{documentId}")
    public Map<String, Object> deleteDocument(@PathVariable Long id, @PathVariable Long documentId,
                                              HttpSession session) {
        User user = authService.requireUser(session);
        applicationService.deleteDocument(user, id, documentId);
        return Map.of("ok", true);
    }

    @PostMapping("/applications/{id}/pay")
    public LegalApplicationDto pay(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.pay(user, id);
    }

    @PostMapping("/applications/{id}/cancel")
    public LegalApplicationDto cancel(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.cancel(user, id);
    }

    @PostMapping("/applications/{id}/request-documents")
    public LegalApplicationDto requestDocuments(@PathVariable Long id,
                                                @Valid @RequestBody CommentForm form,
                                                HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.requestDocuments(user, id, form.comment());
    }

    @PostMapping("/applications/{id}/schedule")
    public LegalApplicationDto schedule(@PathVariable Long id, @Valid @RequestBody ScheduleForm form,
                                        HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.schedule(user, id, form.appointmentAt());
    }

    @PostMapping("/applications/{id}/complete")
    public LegalApplicationDto complete(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.complete(user, id);
    }

    @PostMapping("/applications/{id}/confirm")
    public LegalApplicationDto confirm(@PathVariable Long id, @Valid @RequestBody ConfirmCodeForm form,
                                       HttpSession session) {
        User user = authService.requireUser(session);
        return applicationService.confirm(user, id, form.code());
    }
}
