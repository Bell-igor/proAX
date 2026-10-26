package pro.ax.platform.web.dto;

import java.time.Instant;

/** Сообщение в диалоге. */
public record ChatMessageDto(
        Long id,
        Long threadId,
        Long senderId,
        String senderName,
        String senderCompanyName,
        String body,
        Instant createdAt,
        boolean mine
) {
}
