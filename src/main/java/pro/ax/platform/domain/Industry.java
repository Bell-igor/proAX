package pro.ax.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Отрасль / роль участника в производственной цепочке:
 * поставщик ресурсов, производство, логистика, склад, юристы и т.д.
 */
@Entity
@Table(name = "industries")
public class Industry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 400)
    private String description;

    /** Эмодзи-иконка для навигации по отраслям. */
    @Column(length = 16)
    private String icon;

    @Column(nullable = false)
    private int sortOrder;

    protected Industry() {
    }

    public Industry(String code, String title, String description, String icon, int sortOrder) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
