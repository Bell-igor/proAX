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

/** Оплата тарифа компании (в демо — без платёжного шлюза). */
@Entity
@Table(name = "subscriptions", indexes = @Index(name = "idx_subscription_company", columnList = "company_id"))
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Subscription() {
    }

    public Subscription(Company company, Plan plan, BigDecimal amount, Instant startedAt, Instant expiresAt) {
        this.company = company;
        this.plan = plan;
        this.amount = amount;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public Plan getPlan() {
        return plan;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
