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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Заявка (потребность) компании: что нужно найти, купить или заказать. */
@Entity
@Table(name = "need_requests", indexes = {
        @Index(name = "idx_need_status", columnList = "status"),
        @Index(name = "idx_need_author", columnList = "author_id")
})
public class NeedRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @Column(length = 80)
    private String city;

    private BigDecimal budget;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NeedStatus status = NeedStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_company_id")
    private Company authorCompany;

    @Column(nullable = false)
    private int proposalsCount = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected NeedRequest() {
    }

    public NeedRequest(String title, Industry industry, User author) {
        this.title = title;
        this.industry = industry;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Industry getIndustry() {
        return industry;
    }

    public void setIndustry(Industry industry) {
        this.industry = industry;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public NeedStatus getStatus() {
        return status;
    }

    public void setStatus(NeedStatus status) {
        this.status = status;
    }

    public User getAuthor() {
        return author;
    }

    public Company getAuthorCompany() {
        return authorCompany;
    }

    public void setAuthorCompany(Company authorCompany) {
        this.authorCompany = authorCompany;
    }

    public int getProposalsCount() {
        return proposalsCount;
    }

    public void setProposalsCount(int proposalsCount) {
        this.proposalsCount = proposalsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
