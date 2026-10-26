package pro.ax.platform.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.CompanySize;
import pro.ax.platform.domain.Industry;
import pro.ax.platform.domain.NeedStatus;
import pro.ax.platform.domain.Offer;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.CompanyDocumentRepository;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.FavoriteRepository;
import pro.ax.platform.repo.NeedRequestRepository;
import pro.ax.platform.repo.OfferRepository;
import pro.ax.platform.repo.ReviewRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.CompanyDetailDto;
import pro.ax.platform.web.dto.CompanyForm;
import pro.ax.platform.web.dto.CompanySummaryDto;
import pro.ax.platform.web.dto.OfferDto;
import pro.ax.platform.web.dto.PageDto;
import pro.ax.platform.web.dto.ReviewDto;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Каталог компаний: поиск, фильтры, карточка и редактирование профиля. */
@Service
public class CompanyService {

    private final CompanyRepository companies;
    private final OfferRepository offers;
    private final ReviewRepository reviews;
    private final NeedRequestRepository needs;
    private final FavoriteRepository favorites;
    private final CompanyDocumentRepository documents;
    private final IndustryService industryService;
    private final FileStorageService storage;
    private final DtoMapper mapper;

    public CompanyService(CompanyRepository companies, OfferRepository offers, ReviewRepository reviews,
                          NeedRequestRepository needs, FavoriteRepository favorites,
                          CompanyDocumentRepository documents, IndustryService industryService,
                          FileStorageService storage, DtoMapper mapper) {
        this.companies = companies;
        this.offers = offers;
        this.reviews = reviews;
        this.needs = needs;
        this.favorites = favorites;
        this.documents = documents;
        this.industryService = industryService;
        this.storage = storage;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageDto<CompanySummaryDto> search(String query, String industryCode, String city, Boolean verifiedOnly,
                                             String sort, int page, int size, User viewer) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 60);
        Pageable pageable = PageRequest.of(safePage, safeSize, sortOf(sort));
        Page<Company> result = companies.findAll(specification(query, industryCode, city, verifiedOnly), pageable);
        Set<Long> favoriteIds = favoriteIds(viewer);
        List<CompanySummaryDto> items = result.getContent().stream()
                .map(company -> mapper.summary(company, favoriteIds.contains(company.getId())))
                .toList();
        return PageDto.of(items, safePage, safeSize, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CompanyDetailDto detail(String slug, User viewer) {
        Company company = companies.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Компания не найдена"));
        return detailOf(company, viewer);
    }

    @Transactional(readOnly = true)
    public CompanyDetailDto detailById(Long id, User viewer) {
        Company company = companies.findById(id)
                .orElseThrow(() -> ApiException.notFound("Компания не найдена"));
        return detailOf(company, viewer);
    }

    @Transactional(readOnly = true)
    public List<CompanySummaryDto> featured() {
        return companies.findTop6ByVerifiedTrueOrderByRatingDescReviewsCountDesc().stream()
                .map(company -> mapper.summary(company, false))
                .toList();
    }

    @Transactional
    public CompanyDetailDto updateProfile(Company company, CompanyForm form, User actor) {
        requireCanEdit(company, actor);
        Industry industry = industryService.require(form.industryCode());
        company.setName(form.name().trim());
        company.setIndustry(industry);
        company.setCity(AccountService.trimToNull(form.city()));
        company.setDescription(AccountService.trimToNull(form.description()));
        company.setWebsite(AccountService.normalizeWebsite(form.website()));
        company.setPhone(AccountService.trimToNull(form.phone()));
        company.setEmail(AccountService.trimToNull(form.email()));
        company.setFoundedYear(form.foundedYear());
        company.setSize(parseSize(form.size()));
        return detailOf(companies.save(company), actor);
    }

    @Transactional(readOnly = true)
    public Optional<Company> findMyCompany(User user) {
        return user == null ? Optional.empty() : companies.findByOwnerId(user.getId());
    }

    @Transactional(readOnly = true)
    public Company requireMyCompany(User user) {
        if (user == null) {
            throw ApiException.unauthorized("Для этого действия нужно войти в аккаунт");
        }
        return companies.findByOwnerId(user.getId())
                .orElseThrow(() -> ApiException.badRequest(
                        "У аккаунта ещё нет карточки компании. Заполните профиль компании в кабинете."));
    }

    @Transactional(readOnly = true)
    public Company requireCompany(Long id) {
        return companies.findById(id)
                .orElseThrow(() -> ApiException.notFound("Компания не найдена"));
    }

    @Transactional(readOnly = true)
    public CompanyDetailDto detailForCompany(Company company, User viewer) {
        return detailOf(company, viewer);
    }

    private CompanyDetailDto detailOf(Company company, User viewer) {
        boolean mine = isOwner(company, viewer);
        boolean favorite = viewer != null && favorites.existsByUserIdAndCompanyId(viewer.getId(), company.getId());
        List<Offer> companyOffers = mine
                ? offers.findByCompanyIdOrderByCreatedAtDesc(company.getId())
                : offers.findByCompanyIdAndActiveTrueOrderByCreatedAtDesc(company.getId());
        List<OfferDto> offerDtos = companyOffers.stream()
                .map(offer -> mapper.offer(offer, mine))
                .toList();
        List<ReviewDto> reviewDtos = reviews.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream()
                .map(mapper::review)
                .toList();
        int openNeeds = (int) needs.countByAuthorCompanyIdAndStatus(company.getId(), NeedStatus.OPEN);
        int documentsCount = (int) documents.countByCompanyId(company.getId());
        return mapper.detail(company, favorite, mine, openNeeds, offerDtos, reviewDtos, documentsCount);
    }

    @Transactional
    public CompanyDetailDto uploadLogo(User user, MultipartFile file) {
        Company company = requireMyCompany(user);
        String previous = company.getLogoUrl();
        String storedPath = storage.storeLogo(file);
        company.setLogoUrl(storedPath);
        companies.save(company);
        storage.delete(previous);
        return detailOf(company, user);
    }

    @Transactional
    public CompanyDetailDto removeLogo(User user) {
        Company company = requireMyCompany(user);
        String previous = company.getLogoUrl();
        company.setLogoUrl(null);
        companies.save(company);
        storage.delete(previous);
        return detailOf(company, user);
    }

    @Transactional(readOnly = true)
    public Set<Long> favoriteIds(User viewer) {
        if (viewer == null) {
            return Set.of();
        }
        return new HashSet<>(favorites.findCompanyIdsByUserId(viewer.getId()));
    }

    static boolean isOwner(Company company, User viewer) {
        return viewer != null && company.getOwner() != null
                && company.getOwner().getId().equals(viewer.getId());
    }

    private static void requireCanEdit(Company company, User actor) {
        if (actor == null || (!isOwner(company, actor) && !actor.isAdmin())) {
            throw ApiException.forbidden("Редактировать карточку может только владелец компании");
        }
    }

    private static CompanySize parseSize(String raw) {
        if (raw == null || raw.isBlank()) {
            return CompanySize.SMALL;
        }
        try {
            return CompanySize.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CompanySize.SMALL;
        }
    }

    private static Sort sortOf(String sort) {
        return switch (sort == null ? "" : sort) {
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "rating" -> Sort.by(Sort.Order.desc("planRank"), Sort.Order.desc("rating"),
                    Sort.Order.desc("reviewsCount"));
            default -> Sort.by(Sort.Order.desc("planRank"), Sort.Order.desc("verified"),
                    Sort.Order.desc("rating"), Sort.Order.asc("name"));
        };
    }

    private static Specification<Company> specification(String query, String industryCode, String city,
                                                        Boolean verifiedOnly) {
        Specification<Company> spec = null;

        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            Specification<Company> byText = (root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("city")), like)
            );
            spec = and(spec, byText);
        }
        if (industryCode != null && !industryCode.isBlank()) {
            Specification<Company> byIndustry = (root, cq, cb) -> {
                cq.distinct(true);
                return cb.equal(root.get("industry").get("code"), industryCode.trim());
            };
            spec = and(spec, byIndustry);
        }
        if (city != null && !city.isBlank()) {
            Specification<Company> byCity = (root, cq, cb) -> cb.equal(root.get("city"), city.trim());
            spec = and(spec, byCity);
        }
        if (Boolean.TRUE.equals(verifiedOnly)) {
            Specification<Company> verified = (root, cq, cb) -> cb.isTrue(root.get("verified"));
            spec = and(spec, verified);
        }
        return spec;
    }

    private static Specification<Company> and(Specification<Company> current, Specification<Company> next) {
        return current == null ? next : current.and(next);
    }
}
