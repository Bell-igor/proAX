package pro.ax.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pro.ax.platform.error.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Локальное файловое хранилище для логотипов и документов.
 * Файлы лежат в ./uploads и раздаются по /uploads/**.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long LOGO_MAX_BYTES = 2L * 1024 * 1024;
    private static final long DOCUMENT_MAX_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp"
    );

    private static final Map<String, String> DOCUMENT_TYPES = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg"
    );

    private final Path root;

    public FileStorageService(@Value("${proax.uploads-dir:uploads}") String uploadsDir) {
        this.root = Paths.get(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            log.warn("Не удалось создать каталог загрузок {}: {}", root, ex.getMessage());
        }
    }

    public String storeLogo(MultipartFile file) {
        return store(file, "logos", LOGO_MAX_BYTES, IMAGE_TYPES);
    }

    public String storeDocument(MultipartFile file) {
        return store(file, "docs", DOCUMENT_MAX_BYTES, DOCUMENT_TYPES);
    }

    public Path getRoot() {
        return root;
    }

    private String store(MultipartFile file, String subdir, long maxBytes, Map<String, String> allowed) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Файл не выбран");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest("Файл больше " + (maxBytes / (1024 * 1024)) + " МБ");
        }
        String extension = extension(file.getOriginalFilename());
        if (!allowed.containsKey(extension)) {
            throw ApiException.badRequest("Недопустимый формат файла. Разрешены: " + String.join(", ", allowed.keySet()));
        }
        String contentType = file.getContentType();
        String expected = allowed.get(extension);
        if (contentType != null && !contentType.isBlank()
                && !"application/octet-stream".equalsIgnoreCase(contentType)
                && !contentType.equalsIgnoreCase(expected)) {
            throw ApiException.badRequest("Содержимое файла не соответствует формату " + extension);
        }

        String name = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = root.resolve(subdir).normalize();
        if (!directory.startsWith(root)) {
            throw ApiException.badRequest("Некорректный путь загрузки");
        }
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(name);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + subdir + "/" + name;
        } catch (IOException ex) {
            log.error("Не удалось сохранить файл {}", name, ex);
            throw ApiException.badRequest("Не удалось сохранить файл. Попробуйте ещё раз.");
        }
    }

    public void delete(String urlPath) {
        if (urlPath == null || !urlPath.startsWith("/uploads/")) {
            return;
        }
        Path target = root.resolve(urlPath.substring("/uploads/".length())).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Не удалось удалить файл {}: {}", urlPath, ex.getMessage());
        }
    }

    private static String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
