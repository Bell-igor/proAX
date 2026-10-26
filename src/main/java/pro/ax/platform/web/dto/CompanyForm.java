package pro.ax.platform.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Создание и редактирование компании из личного кабинета. */
public record CompanyForm(
        @NotBlank(message = "Укажите название компании")
        @Size(max = 160, message = "Название слишком длинное")
        String name,

        @NotBlank(message = "Выберите отрасль")
        String industryCode,

        @Size(max = 80, message = "Название города слишком длинное")
        String city,

        @Size(max = 4000, message = "Описание слишком длинное")
        String description,

        @Size(max = 255, message = "Адрес сайта слишком длинный")
        String website,

        @Size(max = 40, message = "Телефон слишком длинный")
        String phone,

        @Email(message = "Некорректный e-mail")
        @Size(max = 180, message = "E-mail слишком длинный")
        String email,

        String size,

        @Min(value = 1800, message = "Год основания — не раньше 1800")
        @Max(value = 2100, message = "Год основания — не позже 2100")
        Integer foundedYear
) {
}
