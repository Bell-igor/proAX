package pro.ax.platform.web.dto;

import java.util.List;

/** Описание тарифа для витрины тарифов. */
public record PlanDto(String code, String title, int price, String tagline,
                      List<String> features, boolean current) {
}
