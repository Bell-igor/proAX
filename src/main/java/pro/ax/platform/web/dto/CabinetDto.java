package pro.ax.platform.web.dto;

import java.util.List;

/** Агрегированные данные личного кабинета одним запросом. */
public record CabinetDto(
        UserDto user,
        CompanyDetailDto company,
        List<NeedDto> needs,
        List<ProposalDto> incoming,
        List<ProposalDto> proposals,
        List<CompanySummaryDto> favorites,
        List<DocumentDto> documents,
        long threadsCount
) {
}
