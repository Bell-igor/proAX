package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Услуга юрфирмы в каталоге. */
public record LegalServiceDto(
        Long id,
        String title,
        String description,
        String targetStatusCode,
        String targetStatusTitle,
        BigDecimal price,
        int durationDays,
        Long providerId,
        String providerSlug,
        String providerName,
        String providerCity,
        String providerLogoUrl,
        String providerAccentColor,
        double providerRating,
        int providerReviewsCount,
        boolean providerVerified,
        String providerPlanCode,
        boolean promoted,
        Integer distanceKm,
        Instant createdAt
) {
}
