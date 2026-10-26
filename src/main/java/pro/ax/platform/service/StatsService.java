package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.NeedStatus;
import pro.ax.platform.domain.Plan;
import pro.ax.platform.repo.ChatThreadRepository;
import pro.ax.platform.repo.CompanyRepository;
import pro.ax.platform.repo.IndustryRepository;
import pro.ax.platform.repo.NeedRequestRepository;
import pro.ax.platform.repo.OfferRepository;
import pro.ax.platform.repo.ProposalRepository;
import pro.ax.platform.repo.UserRepository;
import pro.ax.platform.web.dto.StatsDto;

/** Сводные счётчики площадки для главной страницы и админки. */
@Service
public class StatsService {

    private final CompanyRepository companies;
    private final IndustryRepository industries;
    private final OfferRepository offers;
    private final NeedRequestRepository needs;
    private final ProposalRepository proposals;
    private final UserRepository users;
    private final ChatThreadRepository threads;

    public StatsService(CompanyRepository companies, IndustryRepository industries, OfferRepository offers,
                        NeedRequestRepository needs, ProposalRepository proposals, UserRepository users,
                        ChatThreadRepository threads) {
        this.companies = companies;
        this.industries = industries;
        this.offers = offers;
        this.needs = needs;
        this.proposals = proposals;
        this.users = users;
        this.threads = threads;
    }

    @Transactional(readOnly = true)
    public StatsDto stats() {
        long totalCompanies = companies.count();
        long verified = companies.countByVerifiedTrue();
        return new StatsDto(
                totalCompanies,
                verified,
                Math.max(totalCompanies - verified, 0),
                industries.count(),
                offers.countByActiveTrue(),
                needs.countByStatus(NeedStatus.OPEN),
                proposals.count(),
                companies.findDistinctCities().size(),
                users.count(),
                companies.countByPlanNot(Plan.FREE),
                threads.count()
        );
    }
}
