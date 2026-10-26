package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Смена статуса заявки. */
public record StatusForm(@NotBlank(message = "Укажите статус") String status) {
}
