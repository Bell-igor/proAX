package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.NeedRequest;
import pro.ax.platform.domain.NeedStatus;
import pro.ax.platform.domain.Proposal;
import pro.ax.platform.domain.ProposalStatus;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.ProposalRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.ProposalDto;
import pro.ax.platform.web.dto.ProposalForm;

import java.util.List;
import java.util.Locale;

/** Отклики компаний на заявки: коммерческие предложения и их статусы. */
@Service
public class ProposalService {

    private final ProposalRepository proposals;
    private final NeedService needService;
    private final CompanyService companyService;
    private final ChatService chatService;
    private final DtoMapper mapper;

    public ProposalService(ProposalRepository proposals, NeedService needService,
                           CompanyService companyService, ChatService chatService, DtoMapper mapper) {
        this.proposals = proposals;
        this.needService = needService;
        this.companyService = companyService;
        this.chatService = chatService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProposalDto> forNeed(Long needId, User viewer) {
        NeedRequest need = needService.require(needId);
        if (viewer == null) {
            return List.of();
        }
        boolean author = NeedService.isAuthor(need, viewer);
        if (!author && !viewer.isAdmin()) {
            return companyService.findMyCompany(viewer)
                    .map(company -> proposals.findByNeedIdOrderByCreatedAtDesc(needId).stream()
                            .filter(proposal -> proposal.getCompany().getId().equals(company.getId()))
                            .map(proposal -> mapper.proposal(proposal, true))
                            .toList())
                    .orElse(List.of());
        }
        return proposals.findByNeedIdOrderByCreatedAtDesc(needId).stream()
                .map(proposal -> mapper.proposal(proposal, false))
                .toList();
    }

    @Transactional
    public ProposalDto create(User user, Long needId, ProposalForm form) {
        Company company = companyService.requireMyCompany(user);
        NeedRequest need = needService.require(needId);
        if (NeedService.isAuthor(need, user)) {
            throw ApiException.badRequest("Нельзя откликнуться на собственную заявку");
        }
        if (need.getStatus() != NeedStatus.OPEN) {
            throw ApiException.conflict("Заявка уже не принимает отклики");
        }
        if (proposals.existsByNeedIdAndCompanyId(needId, company.getId())) {
            throw ApiException.conflict("Вы уже откликнулись на эту заявку");
        }
        Proposal proposal = new Proposal(need, company, user);
        proposal.setMessage(form.message() == null ? null : form.message().trim());
        proposal.setPrice(form.price());
        Proposal saved = proposals.save(proposal);

        need.setProposalsCount(need.getProposalsCount() + 1);
        needService.refreshProposalsCount(need);

        // Отклик открывает диалог: стороны могут обсудить детали, не выходя с площадки
        chatService.ensureThreadForProposal(need, company, user);

        return mapper.proposal(saved, true);
    }

    @Transactional
    public ProposalDto changeStatus(User user, Long proposalId, String statusRaw) {
        Proposal proposal = proposals.findById(proposalId)
                .orElseThrow(() -> ApiException.notFound("Отклик не найден"));
        NeedRequest need = proposal.getNeed();
        if (!NeedService.isAuthor(need, user) && !user.isAdmin()) {
            throw ApiException.forbidden("Принимать отклики может только автор заявки");
        }
        ProposalStatus status = parseStatus(statusRaw);
        proposal.setStatus(status);
        if (status == ProposalStatus.ACCEPTED) {
            need.setStatus(NeedStatus.IN_PROGRESS);
        }
        return mapper.proposal(proposals.save(proposal), false);
    }

    @Transactional(readOnly = true)
    public List<ProposalDto> myProposals(User user) {
        return companyService.findMyCompany(user)
                .map(company -> proposals.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream()
                        .map(proposal -> mapper.proposal(proposal, true))
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<ProposalDto> incoming(User user) {
        return proposals.findByNeedAuthorIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(proposal -> mapper.proposal(proposal, false))
                .toList();
    }

    private static ProposalStatus parseStatus(String raw) {
        try {
            return ProposalStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("Неизвестный статус отклика: " + raw);
        }
    }
}
