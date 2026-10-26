package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginForm(
        @NotBlank(message = "Укажите e-mail") String email,
        @NotBlank(message = "Укажите пароль") String password
) {
}
