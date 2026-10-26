package pro.ax.platform.web.dto;

import java.time.Instant;
import java.util.List;

/** Полная карточка компании: профиль, предложения, отзывы. */
public record CompanyDetailDto(
        Long id,
        String slug,
        String name,
        String industryCode,
        String industryTitle,
        String industryIcon,
        String city,
        String description,
        String website,
        String phone,
        String email,
        String sizeTitle,
        Integer foundedYear,
        boolean verified,
        double rating,
        int reviewsCount,
        String accentColor,
        String ownerName,
        Instant createdAt,
        boolean favorite,
        boolean mine,
        int openNeeds,
        List<OfferDto> offers,
        List<ReviewDto> reviews,
        String logoUrl,
        String planCode,
        String planTitle,
        boolean promoted,
        int documentsCount
) {
}
