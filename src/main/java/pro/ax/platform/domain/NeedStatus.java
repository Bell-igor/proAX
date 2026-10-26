package pro.ax.platform.domain;

/** Статус заявки (потребности) на площадке. */
public enum NeedStatus {
    OPEN("Открыта"),
    IN_PROGRESS("В работе"),
    CLOSED("Закрыта");

    private final String title;

    NeedStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
