package pro.ax.platform.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pro.ax.platform.domain.User;
import pro.ax.platform.security.AuthService;
import pro.ax.platform.service.CompanyService;
import pro.ax.platform.service.DocumentService;
import pro.ax.platform.web.dto.CompanyDetailDto;
import pro.ax.platform.web.dto.DocumentDto;

import java.util.List;
import java.util.Map;

/** Логотип и документы компании: загрузка владельцем в личном кабинете. */
@RestController
@RequestMapping("/api/cabinet")
public class UploadController {

    private final CompanyService companyService;
    private final DocumentService documentService;
    private final AuthService authService;

    public UploadController(CompanyService companyService, DocumentService documentService,
                            AuthService authService) {
        this.companyService = companyService;
        this.documentService = documentService;
        this.authService = authService;
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompanyDetailDto uploadLogo(@RequestParam("file") MultipartFile file, HttpSession session) {
        User user = authService.requireUser(session);
        return companyService.uploadLogo(user, file);
    }

    @DeleteMapping("/logo")
    public CompanyDetailDto removeLogo(HttpSession session) {
        User user = authService.requireUser(session);
        return companyService.removeLogo(user);
    }

    @GetMapping("/documents")
    public List<DocumentDto> documents(HttpSession session) {
        User user = authService.requireUser(session);
        return documentService.list(user);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDto uploadDocument(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "title", required = false) String title,
                                      HttpSession session) {
        User user = authService.requireUser(session);
        return documentService.upload(user, title, file);
    }

    @DeleteMapping("/documents/{id}")
    public Map<String, Object> deleteDocument(@PathVariable Long id, HttpSession session) {
        User user = authService.requireUser(session);
        documentService.delete(user, id);
        return Map.of("ok", true);
    }
}
