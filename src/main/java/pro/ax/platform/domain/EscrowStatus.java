package pro.ax.platform.domain;

/** Состояние защищённого платежа (эскроу) по заявке. */
public enum EscrowStatus {
    HELD("Удерживается"),
    RELEASED("Выплачено исполнителю"),
    REFUNDED("Возвращено заказчику");

    private final String title;

    EscrowStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
