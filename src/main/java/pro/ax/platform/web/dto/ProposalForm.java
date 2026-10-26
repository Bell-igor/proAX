package pro.ax.platform.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Отклик компании на заявку. */
public record ProposalForm(
        @NotBlank(message = "Напишите предложение")
        @Size(max = 2000, message = "Сообщение слишком длинное")
        String message,

        @DecimalMin(value = "0", message = "Цена не может быть отрицательной")
        BigDecimal price
) {
}
