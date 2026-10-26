package pro.ax.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Заявка частного лица на оформление юридического статуса.
 * Ведёт заказчик и юрфирма-исполнитель, деньги держит эскроу до подтверждения кодом.
 */
@Entity
@Table(name = "legal_applications", indexes = {
        @Index(name = "idx_legal_app_author", columnList = "author_id"),
        @Index(name = "idx_legal_app_provider", columnList = "provider_id")
})
public class LegalApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Company provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private LegalService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LegalTargetStatus targetStatus;

    @Column(nullable = false, length = 160)
    private String companyName;

    @Column(length = 12)
    private String inn;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 60)
    private String industryCode;

    @Column(length = 2000)
    private String comment;

    @Column(length = 1000)
    private String providerComment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LegalApplicationStatus status = LegalApplicationStatus.NEW;

    /** Согласованная дата и время очной явки в юрфирму. */
    private Instant appointmentAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    private Instant completedAt;

    protected LegalApplication() {
    }

    public LegalApplication(User author, Company provider, LegalService service,
                            LegalTargetStatus targetStatus, String companyName, String city, String industryCode) {
        this.author = author;
        this.provider = provider;
        this.service = service;
        this.targetStatus = targetStatus;
        this.companyName = companyName;
        this.city = city;
        this.industryCode = industryCode;
    }

    public Long getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public Company getProvider() {
        return provider;
    }

    public LegalService getService() {
        return service;
    }

    public LegalTargetStatus getTargetStatus() {
        return targetStatus;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public void setIndustryCode(String industryCode) {
        this.industryCode = industryCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getProviderComment() {
        return providerComment;
    }

    public void setProviderComment(String providerComment) {
        this.providerComment = providerComment;
    }

    public LegalApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(LegalApplicationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getAppointmentAt() {
        return appointmentAt;
    }

    public void setAppointmentAt(Instant appointmentAt) {
        this.appointmentAt = appointmentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
