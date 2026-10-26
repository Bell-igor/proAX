package pro.ax.platform.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Формирование человекочитаемых URL-идентификаторов (slug) из названий на кириллице. */
public final class Slugifier {

    private static final Map<Character, String> TRANSLIT = Map.ofEntries(
            Map.entry('а', "a"), Map.entry('б', "b"), Map.entry('в', "v"), Map.entry('г', "g"),
            Map.entry('д', "d"), Map.entry('е', "e"), Map.entry('ё', "e"), Map.entry('ж', "zh"),
            Map.entry('з', "z"), Map.entry('и', "i"), Map.entry('й', "y"), Map.entry('к', "k"),
            Map.entry('л', "l"), Map.entry('м', "m"), Map.entry('н', "n"), Map.entry('о', "o"),
            Map.entry('п', "p"), Map.entry('р', "r"), Map.entry('с', "s"), Map.entry('т', "t"),
            Map.entry('у', "u"), Map.entry('ф', "f"), Map.entry('х', "h"), Map.entry('ц', "ts"),
            Map.entry('ч', "ch"), Map.entry('ш', "sh"), Map.entry('щ', "sch"), Map.entry('ъ', ""),
            Map.entry('ы', "y"), Map.entry('ь', ""), Map.entry('э', "e"), Map.entry('ю', "yu"),
            Map.entry('я', "ya")
    );

    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private Slugifier() {
    }

    public static String slugify(String source, String fallback) {
        if (source == null || source.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(normalized.length());
        for (char ch : normalized.toCharArray()) {
            String mapped = TRANSLIT.get(ch);
            if (mapped != null) {
                sb.append(mapped);
            } else if (ch < 128) {
                sb.append(ch);
            }
        }
        String slug = NON_SLUG.matcher(sb.toString()).replaceAll("-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        return slug.isBlank() ? fallback : slug;
    }
}
