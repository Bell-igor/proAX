package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.CompanySummaryDto;
import pro.ax.platform.web.dto.NeedDto;

import java.util.List;

/** Модерация площадки администратором. */
@Service
public class AdminService {

    private final CompanyRepository companies;
    private final CompanyService companyService;
    private final NeedService needService;
    private final DtoMapper mapper;

    public AdminService(CompanyRepository companies, CompanyService companyService,
                        NeedService needService, DtoMapper mapper) {
        this.companies = companies;
        this.companyService = companyService;
        this.needService = needService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CompanySummaryDto> companiesWithoutVerification() {
        return companies.findByVerifiedFalseOrderByCreatedAtDesc().stream()
                .map(company -> mapper.summary(company, false))
                .toList();
    }

    @Transactional
    public CompanySummaryDto verify(User admin, Long companyId, boolean verified) {
        if (!admin.isAdmin()) {
            throw ApiException.forbidden("Раздел доступен только администратору площадки");
        }
        Company company = companyService.requireCompany(companyId);
        company.setVerified(verified);
        return mapper.summary(companies.save(company), false);
    }

    @Transactional(readOnly = true)
    public List<NeedDto> recentNeeds() {
        return needService.latest();
    }
}
