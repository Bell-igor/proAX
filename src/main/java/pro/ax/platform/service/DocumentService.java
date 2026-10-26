package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.CompanyDocument;
import pro.ax.platform.domain.User;
import pro.ax.platform.error.ApiException;
import pro.ax.platform.repo.CompanyDocumentRepository;
import pro.ax.platform.web.DtoMapper;
import pro.ax.platform.web.dto.DocumentDto;

import java.util.List;

/** Документы компании: загрузка, список и удаление (только владелец компании). */
@Service
public class DocumentService {

    private final CompanyDocumentRepository documents;
    private final CompanyService companyService;
    private final FileStorageService storage;
    private final DtoMapper mapper;

    public DocumentService(CompanyDocumentRepository documents, CompanyService companyService,
                           FileStorageService storage, DtoMapper mapper) {
        this.documents = documents;
        this.companyService = companyService;
        this.storage = storage;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> list(User user) {
        Company company = companyService.requireMyCompany(user);
        return listFor(company.getId());
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listFor(Long companyId) {
        return documents.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(mapper::document)
                .toList();
    }

    @Transactional
    public DocumentDto upload(User user, String title, MultipartFile file) {
        Company company = companyService.requireMyCompany(user);
        String storedPath = storage.storeDocument(file);
        String cleanTitle = title == null || title.isBlank()
                ? (file.getOriginalFilename() == null ? "Документ" : file.getOriginalFilename())
                : title.trim();
        if (cleanTitle.length() > 160) {
            cleanTitle = cleanTitle.substring(0, 160);
        }
        CompanyDocument document = new CompanyDocument(
                company,
                cleanTitle,
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                storedPath
        );
        return mapper.document(documents.save(document));
    }

    @Transactional
    public void delete(User user, Long documentId) {
        Company company = companyService.requireMyCompany(user);
        CompanyDocument document = documents.findByIdAndCompanyId(documentId, company.getId())
                .orElseThrow(() -> ApiException.notFound("Документ не найден"));
        documents.delete(document);
        storage.delete(document.getStoredPath());
    }

    @Transactional(readOnly = true)
    public long count(Long companyId) {
        return documents.countByCompanyId(companyId);
    }
}
