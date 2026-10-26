package pro.ax.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Защищённый платёж по заявке. Деньги удерживаются платформой (в демо — имитация)
 * и уходят исполнителю только после подтверждения заказчиком одноразовым кодом.
 */
@Entity
@Table(name = "escrow_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_escrow_application", columnNames = "application_id"))
public class EscrowPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LegalApplication application;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EscrowStatus status = EscrowStatus.HELD;

    @Column(nullable = false)
    private Instant heldAt = Instant.now();

    private Instant releasedAt;

    private Instant refundedAt;

    /** Одноразовый код, который заказчик передаёт исполнителю после оказания услуги. */
    @Column(length = 6)
    private String otpCode;

    private Instant otpExpiresAt;

    @Column(nullable = false)
    private int otpAttempts = 0;

    protected EscrowPayment() {
    }

    public EscrowPayment(LegalApplication application, BigDecimal amount) {
        this.application = application;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public LegalApplication getApplication() {
        return application;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public EscrowStatus getStatus() {
        return status;
    }

    public Instant getHeldAt() {
        return heldAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void release() {
        this.status = EscrowStatus.RELEASED;
        this.releasedAt = Instant.now();
        this.otpCode = null;
        this.otpExpiresAt = null;
    }

    public void refund() {
        this.status = EscrowStatus.REFUNDED;
        this.refundedAt = Instant.now();
        this.otpCode = null;
        this.otpExpiresAt = null;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public Instant getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public int getOtpAttempts() {
        return otpAttempts;
    }

    public void issueOtp(String code, Instant expiresAt) {
        this.otpCode = code;
        this.otpExpiresAt = expiresAt;
        this.otpAttempts = 0;
    }

    public void registerAttempt() {
        this.otpAttempts += 1;
    }

    public boolean isOtpActive() {
        return otpCode != null && otpExpiresAt != null && otpExpiresAt.isAfter(Instant.now());
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }
}
