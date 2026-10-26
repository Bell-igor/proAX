package pro.ax.platform.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Заявка на оформление статуса: для заказчика, юрфирмы и админа. */
public record LegalApplicationDto(
        Long id,
        Long authorId,
        String authorName,
        String authorEmail,
        String authorCity,
        String authorPhone,
        Long providerId,
        String providerName,
        String providerSlug,
        String providerCity,
        Long serviceId,
        String serviceTitle,
        String targetStatusCode,
        String targetStatusTitle,
        String companyName,
        String inn,
        String city,
        String industryCode,
        String industryTitle,
        String comment,
        String providerComment,
        String status,
        String statusTitle,
        Instant appointmentAt,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        EscrowDto payment,
        List<DocumentDto> documents,
        String role,
        boolean canUploadDocuments,
        boolean canPay,
        boolean canCancel,
        boolean canRequestDocuments,
        boolean canSchedule,
        boolean canComplete,
        boolean canConfirm,
        BigDecimal amount
) {
}
