package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Новое сообщение в диалоге. */
public record MessageForm(
        @NotBlank(message = "Введите сообщение")
        @Size(max = 2000, message = "Сообщение слишком длинное")
        String body
) {
}
