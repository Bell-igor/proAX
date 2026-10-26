package pro.ax.platform.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesPassword() {
        String hash = hasher.hash("s3cret-pass");
        assertThat(hash).startsWith("pbkdf2$").doesNotContain("s3cret-pass");
        assertThat(hasher.matches("s3cret-pass", hash)).isTrue();
        assertThat(hasher.matches("s3cret-pas", hash)).isFalse();
        assertThat(hasher.matches("", hash)).isFalse();
    }

    @Test
    void producesDifferentSaltsForSamePassword() {
        String first = hasher.hash("same-password");
        String second = hasher.hash("same-password");
        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches("same-password", first)).isTrue();
        assertThat(hasher.matches("same-password", second)).isTrue();
    }

    @Test
    void rejectsMalformedStoredValues() {
        assertThat(hasher.matches("x", null)).isFalse();
        assertThat(hasher.matches("x", "plain-text")).isFalse();
        assertThat(hasher.matches("x", "pbkdf2$abc$def$ghi")).isFalse();
        assertThat(hasher.matches(null, hasher.hash("x"))).isFalse();
    }
}
