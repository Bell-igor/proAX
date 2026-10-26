package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Заявка частного лица на оформление юридического статуса. */
public record LegalApplicationForm(
        @NotNull(message = "Выберите услугу")
        Long serviceId,

        @NotBlank(message = "Укажите название будущей компании")
        @Size(max = 160, message = "Название слишком длинное")
        String companyName,

        @Size(max = 12, message = "ИНН не длиннее 12 цифр")
        String inn,

        @NotBlank(message = "Укажите город")
        @Size(max = 80, message = "Город слишком длинный")
        String city,

        @NotBlank(message = "Выберите отрасль будущего бизнеса")
        String industryCode,

        @Size(max = 2000, message = "Комментарий слишком длинный")
        String comment
) {
}
