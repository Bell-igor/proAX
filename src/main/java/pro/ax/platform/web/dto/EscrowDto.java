package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Состояние эскроу-платежа по заявке. */
public record EscrowDto(
        BigDecimal amount,
        String status,
        String statusTitle,
        Instant heldAt,
        Instant releasedAt,
        Instant refundedAt,
        boolean otpIssued,
        String otp,
        Instant otpExpiresAt
) {
}
