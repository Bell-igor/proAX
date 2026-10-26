package pro.ax.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Документ, приложенный частным лицом к заявке на оформление статуса. */
@Entity
@Table(name = "application_documents",
        indexes = @Index(name = "idx_app_doc_application", columnList = "application_id"))
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LegalApplication application;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false, length = 255)
    private String storedPath;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ApplicationDocument() {
    }

    public ApplicationDocument(LegalApplication application, String title, String fileName,
                               String contentType, long size, String storedPath) {
        this.application = application;
        this.title = title;
        this.fileName = fileName;
        this.contentType = contentType;
        this.size = size;
        this.storedPath = storedPath;
    }

    public Long getId() {
        return id;
    }

    public LegalApplication getApplication() {
        return application;
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
