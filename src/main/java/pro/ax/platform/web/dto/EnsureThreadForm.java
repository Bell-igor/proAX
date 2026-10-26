package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotNull;

/** Запрос на открытие диалога «заявка — компания». */
public record EnsureThreadForm(@NotNull(message = "Не указана заявка") Long needId,
                               @NotNull(message = "Не указана компания") Long companyId) {
}
