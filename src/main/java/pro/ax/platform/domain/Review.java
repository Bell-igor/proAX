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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/** Отзыв о компании: рейтинг и текстовый опыт сотрудничества. */
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_company_author", columnNames = {"company_id", "author_id"}),
        indexes = @Index(name = "idx_review_company", columnList = "company_id"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private int rating;

    @Column(length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Review() {
    }

    public Review(Company company, User author, int rating, String text) {
        this.company = company;
        this.author = author;
        this.rating = rating;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public User getAuthor() {
        return author;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
