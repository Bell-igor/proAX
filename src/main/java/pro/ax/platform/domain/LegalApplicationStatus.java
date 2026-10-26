package pro.ax.platform.domain;

/** Статус заявки на оформление юридического статуса. */
public enum LegalApplicationStatus {
    NEW("Новая"),
    DOCUMENTS_REQUESTED("Нужны документы"),
    SCHEDULED("Назначена явка"),
    PAID("Оплачена, услуга оказывается"),
    COMPLETED("Выполнена"),
    CANCELLED("Отменена");

    private final String title;

    LegalApplicationStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
