package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Отклик компании на заявку. */
public record ProposalDto(
        Long id,
        Long needId,
        String needTitle,
        Long companyId,
        String companySlug,
        String companyName,
        String message,
        BigDecimal price,
        String status,
        String statusTitle,
        Instant createdAt,
        boolean mine
) {
}
