package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import de.jkueck.gateway.monitor.SlugFormatValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlugFormatValidatorTest {

    private SlugFormatValidator validator;

    @BeforeEach
    void setUp() {
        FilterProperties props = new FilterProperties();
        props.getSlug().setValid("^[a-f0-9]{64}$");
        validator = new SlugFormatValidator(props);
        validator.init();
    }

    @ParameterizedTest
    @MethodSource("validSlugs")
    void validSlugs_shouldPass(String slug) {
        assertTrue(validator.isValid(slug));
    }

    private static Stream<String> validSlugs() {
        return Stream.of(
                "a".repeat(64),
                "0123456789abcdef".repeat(4),
                "f".repeat(64)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid-slug",
            "abc123",                      // zu kurz
            "<script>alert(1)</script>",
            "../../etc/passwd",
            "AAAA0000000000000000000000000000000000000000000000000000000000", // Großbuchstaben, 66 Zeichen
            " ",
            ""
    })
    void invalidSlugs_shouldFail(String slug) {
        assertFalse(validator.isValid(slug));
    }

    @org.junit.jupiter.api.Test
    void tooLongSlug_shouldFail() {
        assertFalse(validator.isValid("a".repeat(65)));
    }

    @org.junit.jupiter.api.Test
    void tooShortSlug_shouldFail() {
        assertFalse(validator.isValid("a".repeat(63)));
    }
}