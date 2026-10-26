package pro.ax.platform.web.dto;

import java.util.List;

/** Диалог вместе с историей сообщений. */
public record ChatConversationDto(ChatThreadDto thread, List<ChatMessageDto> messages) {
}
