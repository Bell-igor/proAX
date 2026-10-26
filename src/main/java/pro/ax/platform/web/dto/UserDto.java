package pro.ax.platform.web.dto;

/** Текущий пользователь и его компания. */
public record UserDto(Long id, String email, String displayName, String role,
                      String city, String phone,
                      Long companyId, String companySlug, String companyName, boolean admin) {
}
