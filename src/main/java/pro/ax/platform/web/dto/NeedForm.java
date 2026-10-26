package pro.ax.platform.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Заявка: что компании нужно найти или заказать. */
public record NeedForm(
        @NotBlank(message = "Опишите, что нужно")
        @Size(max = 200, message = "Заголовок слишком длинный")
        String title,

        @Size(max = 4000, message = "Описание слишком длинное")
        String description,

        @NotBlank(message = "Выберите отрасль")
        String industryCode,

        @Size(max = 80, message = "Название города слишком длинное")
        String city,

        @DecimalMin(value = "0", message = "Бюджет не может быть отрицательным")
        BigDecimal budget,

        LocalDate deadline
) {
}
