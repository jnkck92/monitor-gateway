package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import de.jkueck.gateway.monitor.PathAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathAuthorizationServiceTest {

    @ParameterizedTest
    @ValueSource(strings = {"status", "stream"})
    void allowedPaths_shouldReturnTrue(String path) {
        FilterProperties props = new FilterProperties();
        props.setAllowedPaths(List.of("status", "stream"));
        PathAuthorizationService service = new PathAuthorizationService(props);

        assertTrue(service.isAllowed(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "config", "../secrets", ""})
    void disallowedPaths_shouldReturnFalse(String path) {
        FilterProperties props = new FilterProperties();
        props.setAllowedPaths(List.of("status", "stream"));
        PathAuthorizationService service = new PathAuthorizationService(props);

        assertFalse(service.isAllowed(path));
    }

    @Test
    void emptyAllowedPaths_shouldRejectEverything() {
        FilterProperties props = new FilterProperties();
        props.setAllowedPaths(List.of());
        PathAuthorizationService service = new PathAuthorizationService(props);

        assertFalse(service.isAllowed("status"));
    }

    @Test
    void caseSensitivity_shouldNotMatch() {
        FilterProperties props = new FilterProperties();
        props.setAllowedPaths(List.of("status"));
        PathAuthorizationService service = new PathAuthorizationService(props);

        assertFalse(service.isAllowed("STATUS"));
    }
}