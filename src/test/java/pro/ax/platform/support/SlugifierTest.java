package pro.ax.platform.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugifierTest {

    @Test
    void transliteratesCyrillic() {
        assertThat(Slugifier.slugify("УралМеталл", "company")).isEqualTo("uralmetall");
        assertThat(Slugifier.slugify("ТараПак", "company")).isEqualTo("tarapak");
    }

    @Test
    void stripsQuotesAndPunctuation() {
        assertThat(Slugifier.slugify("Завод «ТехноЛит»", "company")).isEqualTo("zavod-tehnolit");
        assertThat(Slugifier.slugify("Логопарк «Урал»", "company")).isEqualTo("logopark-ural");
    }

    @Test
    void fallsBackWhenNothingRemains() {
        assertThat(Slugifier.slugify("", "company")).isEqualTo("company");
        assertThat(Slugifier.slugify("!!!", "company")).isEqualTo("company");
        assertThat(Slugifier.slugify(null, "fallback")).isEqualTo("fallback");
    }
}
