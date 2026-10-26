package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.CompanyService;
import pro.ax.platform.service.ReviewService;
import pro.ax.platform.web.dto.CompanyDetailDto;
import pro.ax.platform.web.dto.CompanyForm;
import pro.ax.platform.web.dto.CompanySummaryDto;
import pro.ax.platform.web.dto.PageDto;
import pro.ax.platform.web.dto.ReviewDto;
import pro.ax.platform.web.dto.ReviewForm;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final ReviewService reviewService;
    private final AuthService authService;
    private final DtoMapper mapper;

    public CompanyController(CompanyService companyService, ReviewService reviewService,
                             AuthService authService, DtoMapper mapper) {
        this.companyService = companyService;
        this.reviewService = reviewService;
        this.authService = authService;
        this.mapper = mapper;
    }

    @GetMapping
    public PageDto<CompanySummaryDto> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String industry,
                                             @RequestParam(required = false) String city,
                                             @RequestParam(required = false) Boolean verified,
                                             @RequestParam(required = false, defaultValue = "default") String sort,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "12") int size,
                                             HttpSession session) {
        return companyService.search(q, industry, city, verified, sort, page, size, viewer(session));
    }

    @GetMapping("/featured")
    public List<CompanySummaryDto> featured() {
        return companyService.featured();
    }

    @GetMapping("/{slug}")
    public CompanyDetailDto detail(@PathVariable String slug, HttpSession session) {
        User viewer = viewer(session);
        if (slug.matches("\\d+")) {
            return companyService.detailById(Long.parseLong(slug), viewer);
        }
        return companyService.detail(slug, viewer);
    }

    @PutMapping("/{id}")
    public CompanyDetailDto update(@PathVariable Long id, @Valid @RequestBody CompanyForm form,
                                   HttpSession session) {
        User user = authService.requireUser(session);
        Company company = companyService.requireCompany(id);
        return companyService.updateProfile(company, form, user);
    }

    @GetMapping("/{slug}/reviews")
    public List<ReviewDto> reviews(@PathVariable String slug) {
        return reviewService.forCompany(slug);
    }

    @PostMapping("/{slug}/reviews")
    public ReviewDto addReview(@PathVariable String slug, @Valid @RequestBody ReviewForm form,
                               HttpSession session) {
        User user = authService.requireUser(session);
        return reviewService.create(user, slug, form);
    }

    /** Заготовка на будущее: карточка компании по её id для внутренних переходов. */
    @GetMapping("/id/{id}")
    public CompanyDetailDto byId(@PathVariable Long id, HttpSession session) {
        return companyService.detailById(id, viewer(session));
    }

    private User viewer(HttpSession session) {
        return authService.currentUser(session).orElse(null);
    }
}
