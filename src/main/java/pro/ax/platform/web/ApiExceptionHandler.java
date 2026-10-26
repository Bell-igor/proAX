package pro.ax.platform.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pro.ax.platform.error.ApiException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Единый формат ошибок API. */
@RestControllerAdvice(basePackages = "pro.ax.platform.web")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDto> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiErrorDto.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        String message = fields.values().stream().findFirst().orElse("Проверьте заполнение полей");
        return ResponseEntity.badRequest()
                .body(new ApiErrorDto("validation_failed", message, fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDto> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorDto.of("bad_request", "Не удалось прочитать данные запроса"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorDto> handleUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorDto.of("file_too_large",
                        "Файл слишком большой. Логотип — до 2 МБ, документ — до 5 МБ."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDto> handleConstraint(DataIntegrityViolationException ex) {
        log.warn("Нарушение ограничения БД: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorDto.of("conflict", "Данные уже существуют или противоречат ограничениям"));
    }

    /** H2 открывает файл только для чтения, если база осталась заблокированной после аварийного завершения. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorDto> handleDataAccess(DataAccessException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        String lower = message == null ? "" : message.toLowerCase();
        if (lower.contains("read only") || lower.contains("read-only") || lower.contains("только для чтения")) {
            log.error("База данных открыта только для чтения: {}", message);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiErrorDto.of("db_read_only",
                            "База данных открыта только для чтения. Остановите и запустите приложение заново."));
        }
        log.error("Ошибка доступа к данным", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorDto.of("internal_error", "Ошибка доступа к данным. Попробуйте позже."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleUnexpected(Exception ex) {
        log.error("Необработанная ошибка API", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorDto.of("internal_error", "Внутренняя ошибка сервера. Попробуйте позже."));
    }
}
