package pro.ax.platform.web.dto;

import java.time.Instant;

/** Отзыв о компании. */
public record ReviewDto(Long id, String authorName, int rating, String text, Instant createdAt) {
}
