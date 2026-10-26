package pro.ax.platform.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Регистрация представителя компании вместе с карточкой компании. */
public record RegisterForm(
        @NotBlank(message = "Укажите e-mail")
        @Email(message = "Некорректный e-mail")
        @Size(max = 180, message = "E-mail слишком длинный")
        String email,

        @NotBlank(message = "Придумайте пароль")
        @Size(min = 6, max = 72, message = "Пароль должен быть от 6 до 72 символов")
        String password,

        @NotBlank(message = "Укажите имя")
        @Size(max = 120, message = "Имя слишком длинное")
        String displayName,

        @NotBlank(message = "Укажите название компании")
        @Size(max = 160, message = "Название слишком длинное")
        String companyName,

        @NotBlank(message = "Выберите отрасль")
        String industryCode,

        @Size(max = 80, message = "Название города слишком длинное")
        String city,

        @Size(max = 4000, message = "Описание слишком длинное")
        String description,

        @Size(max = 255, message = "Адрес сайта слишком длинный")
        String website,

        @Size(max = 40, message = "Телефон слишком длинный")
        String phone
) {
}
