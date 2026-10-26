package pro.ax.platform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.ax.platform.domain.Company;
import pro.ax.platform.domain.LegalService;
import pro.ax.platform.domain.LegalTargetStatus;
import pro.ax.platform.domain.User;
import pro.ax.platform.repo.LegalServiceRepository;
import pro.ax.platform.support.CityGeo;
import pro.ax.platform.web.dto.LegalServiceDto;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Каталог юруслуг: оформление статуса от самозанятого до АО.
 * Сортировка по расстоянию, рейтингу, цене и оплаченному продвижению.
 */
@Service
public class LegalCatalogService {

    private final LegalServiceRepository services;

    public LegalCatalogService(LegalServiceRepository services) {
        this.services = services;
    }

    @Transactional(readOnly = true)
    public List<LegalServiceDto> search(String targetStatusCode, String originCity, String sort,
                                        String query, User viewer) {
        String origin = firstNonBlank(originCity, viewer == null ? null : viewer.getCity());
        String text = query == null ? null : query.trim().toLowerCase(Locale.ROOT);

        return services.findByActiveTrueOrderByPriceAsc().stream()
                .filter(service -> targetStatusCode == null || targetStatusCode.isBlank()
                        || service.getTargetStatus().name().equalsIgnoreCase(targetStatusCode))
                .filter(service -> matches(service, text))
                .map(service -> toDto(service, origin))
                .sorted(comparator(sort))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LegalServiceDto> forProvider(String slug, String originCity) {
        return services.findAll().stream()
                .filter(LegalService::isActive)
                .filter(service -> service.getProvider().getSlug().equals(slug))
                .map(service -> toDto(service, originCity))
                .sorted(Comparator.comparing(LegalServiceDto::price))
                .toList();
    }

    public Map<String, String> targetStatuses() {
        return java.util.Arrays.stream(LegalTargetStatus.values())
                .collect(Collectors.toMap(Enum::name, LegalTargetStatus::getTitle,
                        (a, b) -> a, java.util.LinkedHashMap::new));
    }

    private static boolean matches(LegalService service, String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String providerName = service.getProvider().getName().toLowerCase(Locale.ROOT);
        String haystack = (service.getTitle() + " " + nullSafe(service.getDescription())
                + " " + providerName + " " + nullSafe(service.getProvider().getCity())).toLowerCase(Locale.ROOT);
        return haystack.contains(text);
    }

    private static Comparator<LegalServiceDto> comparator(String sort) {
        Comparator<LegalServiceDto> byDistance = Comparator.comparing(
                LegalServiceDto::distanceKm, Comparator.nullsLast(Integer::compareTo));
        Comparator<LegalServiceDto> byRating = Comparator
                .comparingDouble(LegalServiceDto::providerRating).reversed()
                .thenComparing(Comparator.comparingInt(LegalServiceDto::providerReviewsCount).reversed());
        Comparator<LegalServiceDto> byPromotion = Comparator.comparing(LegalServiceDto::promoted).reversed();

        return switch (sort == null ? "" : sort) {
            case "near" -> byDistance.thenComparing(byRating);
            case "rating" -> byRating.thenComparing(byDistance);
            case "price" -> Comparator.comparing(LegalServiceDto::price).thenComparing(byDistance);
            default -> byPromotion.thenComparing(byDistance).thenComparing(byRating);
        };
    }

    private static LegalServiceDto toDto(LegalService service, String originCity) {
        Company provider = service.getProvider();
        Integer distance = originCity == null ? null : CityGeo.distanceKm(originCity, provider.getCity());
        return new LegalServiceDto(
                service.getId(),
                service.getTitle(),
                service.getDescription(),
                service.getTargetStatus().name(),
                service.getTargetStatus().getTitle(),
                service.getPrice(),
                service.getDurationDays(),
                provider.getId(),
                provider.getSlug(),
                provider.getName(),
                provider.getCity(),
                provider.getLogoUrl(),
                provider.getAccentColor(),
                Math.round(provider.getRating() * 10d) / 10d,
                provider.getReviewsCount(),
                provider.isVerified(),
                provider.getPlan().name(),
                provider.isPromoted(),
                distance,
                service.getCreatedAt()
        );
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
