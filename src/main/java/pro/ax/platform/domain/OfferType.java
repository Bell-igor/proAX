package pro.ax.platform.domain;

/** Тип предложения компании. */
public enum OfferType {
    PRODUCT("Товар"),
    SERVICE("Услуга");

    private final String title;

    OfferType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
