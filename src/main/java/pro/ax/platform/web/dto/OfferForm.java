package pro.ax.platform.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Товар или услуга в каталоге компании. */
public record OfferForm(
        @NotBlank(message = "Укажите название")
        @Size(max = 160, message = "Название слишком длинное")
        String title,

        @Size(max = 2000, message = "Описание слишком длинное")
        String description,

        String type,

        @DecimalMin(value = "0", message = "Цена не может быть отрицательной")
        BigDecimal priceFrom,

        @Size(max = 30, message = "Единица измерения слишком длинная")
        String unit,

        Boolean active
) {
}
