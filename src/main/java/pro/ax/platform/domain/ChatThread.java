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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Диалог по заявке между её автором и откликнувшейся компанией.
 * На одну пару «заявка + компания» приходится один диалог.
 */
@Entity
@Table(name = "chat_threads",
        uniqueConstraints = @UniqueConstraint(name = "uk_thread_need_company", columnNames = {"need_id", "company_id"}))
public class ChatThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "need_id", nullable = false)
    private NeedRequest need;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id")
    private User initiator;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant lastMessageAt = Instant.now();

    @Column(nullable = false)
    private int messagesCount = 0;

    @Column(length = 400)
    private String lastMessagePreview;

    protected ChatThread() {
    }

    public ChatThread(NeedRequest need, Company company, User initiator) {
        this.need = need;
        this.company = company;
        this.initiator = initiator;
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

    public User getInitiator() {
        return initiator;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public int getMessagesCount() {
        return messagesCount;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void registerMessage(String preview) {
        this.messagesCount += 1;
        this.lastMessageAt = Instant.now();
        this.lastMessagePreview = preview == null ? null
                : (preview.length() > 160 ? preview.substring(0, 157) + "…" : preview);
    }
}
