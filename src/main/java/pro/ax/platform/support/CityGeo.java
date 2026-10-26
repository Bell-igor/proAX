package pro.ax.platform.support;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Координаты городов площадки и расстояние между ними.
 * Нужен для сортировки исполнителей «рядом со мной» без внешних гео-сервисов.
 */
public final class CityGeo {

    public record Point(double latitude, double longitude) {
    }

    private static final Map<String, Point> CITIES = Map.ofEntries(
            Map.entry("екатеринбург", new Point(56.8389, 60.6057)),
            Map.entry("москва", new Point(55.7558, 37.6173)),
            Map.entry("санкт-петербург", new Point(59.9343, 30.3351)),
            Map.entry("казань", new Point(55.7963, 49.1088)),
            Map.entry("новосибирск", new Point(55.0084, 82.9357)),
            Map.entry("нижний новгород", new Point(56.3269, 44.0059)),
            Map.entry("челябинск", new Point(55.1644, 61.4368)),
            Map.entry("ростов-на-дону", new Point(47.2357, 39.7015)),
            Map.entry("самара", new Point(53.1959, 50.1002)),
            Map.entry("уфа", new Point(54.7388, 55.9721)),
            Map.entry("пермь", new Point(58.0105, 56.2502)),
            Map.entry("воронеж", new Point(51.6608, 39.2003))
    );

    private static final double EARTH_RADIUS_KM = 6371.0;

    private CityGeo() {
    }

    public static Optional<Point> pointOf(String city) {
        if (city == null || city.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CITIES.get(city.trim().toLowerCase(Locale.ROOT)));
    }

    /** Расстояние по прямой между городами; null, если координаты неизвестны. */
    public static Integer distanceKm(String fromCity, String toCity) {
        Optional<Point> from = pointOf(fromCity);
        Optional<Point> to = pointOf(toCity);
        if (from.isEmpty() || to.isEmpty()) {
            return null;
        }
        return (int) Math.round(haversine(from.get(), to.get()));
    }

    private static double haversine(Point a, Point b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(h)));
    }
}
