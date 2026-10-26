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

/** Компания-участник площадки: поставщик, завод, перевозчик, склад, юридическая фирма и т.д. */
@Entity
@Table(name = "companies", indexes = {
        @Index(name = "idx_company_industry", columnList = "industry_id"),
        @Index(name = "idx_company_city", columnList = "city")
})
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @Column(length = 80)
    private String city;

    @Column(length = 4000)
    private String description;

    @Column(length = 255)
    private String website;

    @Column(length = 40)
    private String phone;

    @Column(length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CompanySize size = CompanySize.SMALL;

    private Integer foundedYear;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private double rating = 0d;

    @Column(nullable = false)
    private int reviewsCount = 0;

    /** Цвет монограммы-логотипа на карточках. */
    @Column(length = 20)
    private String accentColor = "#C8F169";

    /** Путь к загруженному логотипу, например /uploads/logos/{uuid}.png */
    @Column(length = 255)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan = Plan.FREE;

    /** Числовой вес тарифа для сортировки в каталоге (0 — базовый, 2 — Premium). */
    @Column(nullable = false)
    private int planRank = 0;

    /** До какого момента действует платное продвижение. */
    private Instant promotedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Company() {
    }

    public Company(String name, String slug, Industry industry) {
        this.name = name;
        this.slug = slug;
        this.industry = industry;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CompanySize getSize() {
        return size;
    }

    public void setSize(CompanySize size) {
        this.size = size;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan == null ? Plan.FREE : plan;
        this.planRank = this.plan.getRank();
    }

    public int getPlanRank() {
        return planRank;
    }

    public Instant getPromotedUntil() {
        return promotedUntil;
    }

    public void setPromotedUntil(Instant promotedUntil) {
        this.promotedUntil = promotedUntil;
    }

    /** Платное продвижение активно прямо сейчас. */
    public boolean isPromoted() {
        return plan != null && plan.isPaid() && promotedUntil != null && promotedUntil.isAfter(Instant.now());
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
