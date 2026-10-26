package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Запись об оплате тарифа. */
public record SubscriptionDto(String planTitle, BigDecimal amount, Instant startedAt, Instant expiresAt) {
}
