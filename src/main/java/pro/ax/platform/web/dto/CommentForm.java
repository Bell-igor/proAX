package pro.ax.platform.web.dto;

import jakarta.validation.constraints.Size;

/** Комментарий юрфирмы к заявке (например, список нужных документов). */
public record CommentForm(@Size(max = 1000, message = "Комментарий слишком длинный") String comment) {
}
