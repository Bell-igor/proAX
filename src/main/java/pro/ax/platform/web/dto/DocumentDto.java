package pro.ax.platform.web.dto;

import java.time.Instant;

/** Документ компании, загруженный владельцем. */
public record DocumentDto(Long id, String title, String fileName, String contentType, long size,
                          String url, Instant createdAt) {
}
