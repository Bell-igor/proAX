package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.Review;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.ReviewRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.ReviewDto;
import pro.ax.platform.web.dto.ReviewForm;

import java.util.List;

/** Отзывы и рейтинг компаний. */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final CompanyRepository companies;
    private final CompanyService companyService;
    private final DtoMapper mapper;

    public ReviewService(ReviewRepository reviews, CompanyRepository companies,
                         CompanyService companyService, DtoMapper mapper) {
        this.reviews = reviews;
        this.companies = companies;
        this.companyService = companyService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> forCompany(String slug) {
        Company company = companies.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Компания не найдена"));
        return reviews.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream()
                .map(mapper::review)
                .toList();
    }

    @Transactional
    public ReviewDto create(User user, String slug, ReviewForm form) {
        Company company = companies.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Компания не найдена"));
        if (CompanyService.isOwner(company, user)) {
            throw ApiException.badRequest("Нельзя оставить отзыв о собственной компании");
        }
        if (reviews.existsByCompanyIdAndAuthorId(company.getId(), user.getId())) {
            throw ApiException.conflict("Вы уже оставили отзыв об этой компании");
        }
        Review review = new Review(company, user, form.rating(),
                form.text() == null ? null : form.text().trim());
        Review saved = reviews.save(review);
        recalculate(company);
        return mapper.review(saved);
    }

    private void recalculate(Company company) {
        long count = reviews.countByCompanyId(company.getId());
        Double average = reviews.averageRating(company.getId());
        company.setReviewsCount((int) count);
        company.setRating(average == null ? 0d : average);
        companies.save(company);
    }
}
