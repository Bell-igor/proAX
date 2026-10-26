package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Регистрация частного лица — без компании, с городом для подбора услуг рядом. */
public record PersonRegisterForm(
        @NotBlank(message = "Укажите e-mail")
        @jakarta.validation.constraints.Email(message = "Некорректный e-mail")
        @Size(max = 180, message = "E-mail слишком длинный")
        String email,

        @NotBlank(message = "Придумайте пароль")
        @Size(min = 6, max = 72, message = "Пароль должен быть от 6 до 72 символов")
        String password,

        @NotBlank(message = "Укажите имя")
        @Size(max = 120, message = "Имя слишком длинное")
        String displayName,

        @Size(max = 40, message = "Телефон слишком длинный")
        String phone,

        @Size(max = 80, message = "Город слишком длинный")
        String city
) {
}
