package pro.ax.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Документ компании: устав, лицензия, сертификат и т.п. Виден только владельцу и админу. */
@Entity
@Table(name = "company_documents")
public class CompanyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false)
    private long size;

    /** Путь для скачивания, например /uploads/docs/{uuid}.pdf */
    @Column(nullable = false, length = 255)
    private String storedPath;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected CompanyDocument() {
    }

    public CompanyDocument(Company company, String title, String fileName, String contentType,
                           long size, String storedPath) {
        this.company = company;
        this.title = title;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.storedPath = storedPath;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getTitle() {
        return title;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
