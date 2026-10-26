package pro.ax.platform.domain;

/** Размер компании-участника. */
public enum CompanySize {
    MICRO("до 15 сотрудников"),
    SMALL("15–100 сотрудников"),
    MEDIUM("100–500 сотрудников"),
    LARGE("более 500 сотрудников");

    private final String title;

    CompanySize(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
