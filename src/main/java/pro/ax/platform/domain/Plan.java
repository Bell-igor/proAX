package pro.ax.platform.domain;

/** Тариф компании на площадке. */
public enum Plan {
    FREE("Базовый", 0, 0),
    PRO("PRO", 4900, 1),
    PREMIUM("Premium", 14900, 2);

    private final String title;
    private final int monthlyPrice;
    private final int rank;

    Plan(String title, int monthlyPrice, int rank) {
        this.title = title;
        this.monthlyPrice = monthlyPrice;
        this.rank = rank;
    }

    public String getTitle() {
        return title;
    }

    public int getMonthlyPrice() {
        return monthlyPrice;
    }

    public int getRank() {
        return rank;
    }

    public boolean isPaid() {
        return this != FREE;
    }
}
