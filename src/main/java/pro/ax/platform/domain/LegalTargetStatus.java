package pro.ax.platform.domain;

/** Целевой юридический статус, который оформляет частное лицо. */
public enum LegalTargetStatus {
    SELF_EMPLOYED("Самозанятый"),
    IP("Индивидуальный предприниматель"),
    LLC("ООО"),
    JSC("Акционерное общество");

    private final String title;

    LegalTargetStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
