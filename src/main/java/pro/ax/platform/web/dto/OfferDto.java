package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Товар или услуга компании. */
public record OfferDto(
        Long id,
        Long companyId,
        String companySlug,
        String companyName,
        String title,
        String description,
        String type,
        String typeTitle,
        BigDecimal priceFrom,
        String unit,
        String currency,
        boolean active,
        Instant createdAt,
        boolean mine
) {
}
