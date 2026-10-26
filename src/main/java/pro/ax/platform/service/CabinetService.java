package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.User;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.CabinetDto;
import pro.ax.platform.web.dto.CompanyDetailDto;
import pro.ax.platform.web.dto.UserDto;

import java.util.List;
import java.util.Optional;

/** Сборка данных личного кабинета. */
@Service
public class CabinetService {

    private final CompanyService companyService;
    private final NeedService needService;
    private final ProposalService proposalService;
    private final FavoriteService favoriteService;
    private final ChatService chatService;
    private final DocumentService documentService;
    private final DtoMapper mapper;

    public CabinetService(CompanyService companyService, NeedService needService,
                          ProposalService proposalService, FavoriteService favoriteService,
                          ChatService chatService, DocumentService documentService, DtoMapper mapper) {
        this.companyService = companyService;
        this.needService = needService;
        this.proposalService = proposalService;
        this.favoriteService = favoriteService;
        this.chatService = chatService;
        this.documentService = documentService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CabinetDto load(User user) {
        Optional<Company> company = companyService.findMyCompany(user);
        CompanyDetailDto companyDto = company
                .map(entity -> companyService.detailForCompany(entity, user))
                .orElse(null);
        UserDto userDto = mapper.user(user, company.orElse(null));
        return new CabinetDto(
                userDto,
                companyDto,
                needService.myNeeds(user),
                proposalService.incoming(user),
                proposalService.myProposals(user),
                favoriteService.list(user),
                company.map(entity -> documentService.listFor(entity.getId())).orElse(List.of()),
                chatService.countFor(user)
        );
    }
}
