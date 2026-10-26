package pro.ax.platform.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.domain.NeedRequest;
import pro.ax.platform.domain.NeedStatus;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.NeedRequestRepository;
import pro.ax.platform.repo.ProposalRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.NeedDto;
import pro.ax.platform.web.dto.NeedForm;
import pro.ax.platform.web.dto.PageDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Лента потребностей: компании публикуют, что им нужно, и получают отклики. */
@Service
public class NeedService {

    private final NeedRequestRepository needs;
    private final ProposalRepository proposals;
    private final CompanyService companyService;
    private final IndustryService industryService;
    private final DtoMapper mapper;

    public NeedService(NeedRequestRepository needs, ProposalRepository proposals,
                       CompanyService companyService, IndustryService industryService, DtoMapper mapper) {
        this.needs = needs;
        this.proposals = proposals;
        this.companyService = companyService;
        this.industryService = industryService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageDto<NeedDto> search(String query, String industryCode, String status, String city,
                                   int page, int size, User viewer) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 60);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NeedRequest> result = needs.findAll(specification(query, industryCode, status, city), pageable);

        Company viewerCompany = companyOf(viewer);
        List<NeedDto> items = new ArrayList<>(result.getContent().size());
        for (NeedRequest need : result.getContent()) {
            boolean mine = isAuthor(need, viewer);
            boolean canRespond = viewerCompany != null && !mine && need.getStatus() == NeedStatus.OPEN;
            boolean responded = viewerCompany != null
                    && proposals.existsByNeedIdAndCompanyId(need.getId(), viewerCompany.getId());
            items.add(mapper.need(need, mine, canRespond, responded));
        }
        return PageDto.of(items, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<NeedDto> latest() {
        return needs.findTop8ByStatusOrderByCreatedAtDesc(NeedStatus.OPEN).stream()
                .map(need -> mapper.need(need, false, false, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public NeedDto detail(Long id, User viewer) {
        NeedRequest need = require(id);
        Company viewerCompany = companyOf(viewer);
        boolean mine = isAuthor(need, viewer);
        boolean canRespond = viewerCompany != null && !mine && need.getStatus() == NeedStatus.OPEN;
        boolean responded = viewerCompany != null
                && proposals.existsByNeedIdAndCompanyId(need.getId(), viewerCompany.getId());
        return mapper.need(need, mine, canRespond, responded);
    }

    @Transactional(readOnly = true)
    public List<NeedDto> myNeeds(User user) {
        return needs.findByAuthorIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(need -> mapper.need(need, true, false, true))
                .toList();
    }

    @Transactional
    public NeedDto create(User user, NeedForm form) {
        Industry industry = industryService.require(form.industryCode());
        if (form.deadline() != null && form.deadline().isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Срок исполнения не может быть в прошлом");
        }
        NeedRequest need = new NeedRequest(form.title().trim(), industry, user);
        need.setDescription(AccountService.trimToNull(form.description()));
        need.setCity(AccountService.trimToNull(form.city()));
        need.setBudget(form.budget());
        need.setDeadline(form.deadline());
        need.setAuthorCompany(companyOf(user));
        NeedRequest saved = needs.save(need);
        return mapper.need(saved, true, false, false);
    }

    @Transactional
    public NeedDto changeStatus(User user, Long id, String statusRaw) {
        NeedRequest need = require(id);
        if (!isAuthor(need, user) && !user.isAdmin()) {
            throw ApiException.forbidden("Менять статус заявки может только её автор");
        }
        need.setStatus(parseStatus(statusRaw));
        return mapper.need(needs.save(need), true, false, false);
    }

    @Transactional
    public void delete(User user, Long id) {
        NeedRequest need = require(id);
        if (!isAuthor(need, user) && !user.isAdmin()) {
            throw ApiException.forbidden("Удалить заявку может только её автор");
        }
        needs.delete(need);
    }

    @Transactional
    public void refreshProposalsCount(NeedRequest need) {
        int count = proposals.findByNeedIdOrderByCreatedAtDesc(need.getId()).size();
        need.setProposalsCount(count);
        needs.save(need);
    }

    @Transactional(readOnly = true)
    public NeedRequest require(Long id) {
        return needs.findById(id)
                .orElseThrow(() -> ApiException.notFound("Заявка не найдена"));
    }

    @Transactional(readOnly = true)
    public Company companyOf(User user) {
        return companyService.findMyCompany(user).orElse(null);
    }

    static boolean isAuthor(NeedRequest need, User viewer) {
        return viewer != null && need.getAuthor().getId().equals(viewer.getId());
    }

    private static NeedStatus parseStatus(String raw) {
        try {
            return NeedStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("Неизвестный статус заявки: " + raw);
        }
    }

    private static Specification<NeedRequest> specification(String query, String industryCode, String status,
                                                            String city) {
        Specification<NeedRequest> spec = null;
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            Specification<NeedRequest> byText = (root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
            );
            spec = spec == null ? byText : spec.and(byText);
        }
        if (industryCode != null && !industryCode.isBlank()) {
            Specification<NeedRequest> byIndustry = (root, cq, cb) -> {
                cq.distinct(true);
                return cb.equal(root.get("industry").get("code"), industryCode.trim());
            };
            spec = spec == null ? byIndustry : spec.and(byIndustry);
        }
        if (city != null && !city.isBlank()) {
            Specification<NeedRequest> byCity = (root, cq, cb) -> cb.equal(root.get("city"), city.trim());
            spec = spec == null ? byCity : spec.and(byCity);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            NeedStatus parsed = parseStatus(status);
            Specification<NeedRequest> byStatus = (root, cq, cb) -> cb.equal(root.get("status"), parsed);
            spec = spec == null ? byStatus : spec.and(byStatus);
        }
        return spec;
    }
}
