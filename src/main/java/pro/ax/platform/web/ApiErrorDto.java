package pro.ax.platform.web;

import java.util.Map;

/** Тело ошибки API. */
public record ApiErrorDto(String code, String message, Map<String, String> fields) {

    public static ApiErrorDto of(String code, String message) {
        return new ApiErrorDto(code, message, Map.of());
    }
}
