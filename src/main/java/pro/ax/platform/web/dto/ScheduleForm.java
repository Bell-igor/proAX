package pro.ax.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Назначение даты и времени очной явки (ISO: 2026-11-05T10:00). */
public record ScheduleForm(@NotBlank(message = "Укажите дату и время") String appointmentAt) {
}
