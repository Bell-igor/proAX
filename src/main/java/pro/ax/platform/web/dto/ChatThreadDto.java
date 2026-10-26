package pro.ax.platform.web.dto;

import java.time.Instant;

/** Диалог по заявке в списке переписок. */
public record ChatThreadDto(
        Long id,
        Long needId,
        String needTitle,
        Long companyId,
        String companyName,
        String companySlug,
        String authorName,
        String role,
        String lastMessagePreview,
        Instant lastMessageAt,
        int messagesCount
) {
}
