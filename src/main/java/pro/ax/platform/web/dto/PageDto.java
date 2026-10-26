package pro.ax.platform.web.dto;

import java.util.List;

/** Страница результатов с метаданными пагинации. */
public record PageDto<T>(List<T> items, int page, int size, long total, int totalPages) {

    public static <T> PageDto<T> of(List<T> items, int page, int size, long total) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageDto<>(items, page, size, total, totalPages);
    }
}
