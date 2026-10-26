package pro.ax.platform.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Отзыв о компании. */
public record ReviewForm(
        @Min(value = 1, message = "Оценка — от 1 до 5")
        @Max(value = 5, message = "Оценка — от 1 до 5")
        int rating,

        @Size(max = 2000, message = "Текст отзыва слишком длинный")
        String text
) {
}
