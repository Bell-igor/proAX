package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Код подтверждения выполнения услуги (передаёт заказчик, вводит исполнитель). */
public record ConfirmCodeForm(
        @NotBlank(message = "Введите код")
        @Pattern(regexp = "\\d{6}", message = "Код состоит из 6 цифр")
        String code
) {
}
