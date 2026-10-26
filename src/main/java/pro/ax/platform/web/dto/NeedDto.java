package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Заявка на площадке. */
public record NeedDto(
        Long id,
        String title,
        String description,
        String industryCode,
        String industryTitle,
        String industryIcon,
        String city,
        BigDecimal budget,
        LocalDate deadline,
        String status,
        String statusTitle,
        String authorName,
        String authorCompanyName,
        String authorCompanySlug,
        int proposalsCount,
        Instant createdAt,
        boolean mine,
        boolean canRespond,
        boolean alreadyResponded
) {
}
