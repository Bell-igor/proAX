package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Состояние тарифа компании и история платежей. */
public record BillingDto(
        String planCode,
        String planTitle,
        boolean promoted,
        Instant promotedUntil,
        int monthlyPrice,
        BigDecimal totalSpent,
        List<PlanDto> plans,
        List<SubscriptionDto> history
) {
}
