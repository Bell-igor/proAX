package pro.ax.platform.web.dto;

/** Счётчики для главной страницы и админки. */
public record StatsDto(
        long companies,
        long verifiedCompanies,
        long pendingCompanies,
        long industries,
        long offers,
        long openNeeds,
        long proposals,
        long cities,
        long users,
        long paidCompanies,
        long threads
) {
}
