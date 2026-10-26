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

/** Отклик компании на заявку — по сути коммерческое предложение. */
@Entity
@Table(name = "proposals", indexes = {
        @Index(name = "idx_proposal_need", columnList = "need_id"),
        @Index(name = "idx_proposal_company", columnList = "company_id")
})
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "need_id", nullable = false)
    private NeedRequest need;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(length = 2000)
    private String message;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.NEW;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Proposal() {
    }

    public Proposal(NeedRequest need, Company company, User author) {
        this.need = need;
        this.company = company;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public NeedRequest getNeed() {
        return need;
    }

    public Company getCompany() {
        return company;
    }

    public User getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
