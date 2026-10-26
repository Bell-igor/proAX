package pro.ax.platform.web.dto;

/** Отрасль / роль участника в цепочке поставок. */
public record IndustryDto(String code, String title, String description, String icon, long companiesCount) {
}
