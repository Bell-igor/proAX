package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Выбор тарифа. */
public record UpgradeForm(@NotBlank(message = "Укажите тариф") String plan) {
}
