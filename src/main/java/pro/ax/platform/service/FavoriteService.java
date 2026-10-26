package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.Favorite;
import pro.ax.platform.domain.User;
import pro.ax.platform.repo.FavoriteRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.CompanySummaryDto;

import java.util.List;
import java.util.Set;

/** Избранные компании пользователя. */
@Service
public class FavoriteService {

    private final FavoriteRepository favorites;
    private final CompanyService companyService;
    private final DtoMapper mapper;

    public FavoriteService(FavoriteRepository favorites, CompanyService companyService, DtoMapper mapper) {
        this.favorites = favorites;
        this.companyService = companyService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CompanySummaryDto> list(User user) {
        return favorites.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(Favorite::getCompany)
                .map(company -> mapper.summary(company, true))
                .toList();
    }

    @Transactional
    public void add(User user, Long companyId) {
        Company company = companyService.requireCompany(companyId);
        if (!favorites.existsByUserIdAndCompanyId(user.getId(), companyId)) {
            favorites.save(new Favorite(user, company));
        }
    }

    @Transactional
    public void remove(User user, Long companyId) {
        favorites.findByUserIdAndCompanyId(user.getId(), companyId)
                .ifPresent(favorites::delete);
    }

    @Transactional(readOnly = true)
    public Set<Long> ids(User user) {
        return companyService.favoriteIds(user);
    }
}
