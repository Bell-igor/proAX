package pro.ax.platform.domain;

/** Статус отклика (коммерческого предложения) на заявку. */
public enum ProposalStatus {
    NEW("Новый"),
    ACCEPTED("Принят"),
    REJECTED("Отклонён");

    private final String title;

    ProposalStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
