package pro.ax.platform.web.dto;

/** Краткая карточка компании для каталога и подборок. */
public record CompanySummaryDto(
        Long id,
        String slug,
        String name,
        String industryCode,
        String industryTitle,
        String industryIcon,
        String city,
        String shortDescription,
        boolean verified,
        double rating,
        int reviewsCount,
        String accentColor,
        String sizeTitle,
        Integer foundedYear,
        boolean favorite,
        String logoUrl,
        String planCode,
        String planTitle,
        boolean promoted
) {
}
