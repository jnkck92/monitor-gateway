package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.DashboardProperties;
import de.jkueck.gateway.monitor.TenantResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TenantResolverTest {

    @Test
    void knownSlug_shouldReturnTenant() {
        DashboardProperties props = new DashboardProperties();
        props.setTenants(Map.of("a".repeat(64), "feuerwehr-musterstadt"));
        TenantResolver resolver = new TenantResolver(props);

        Optional<String> result = resolver.resolve("a".repeat(64));

        assertTrue(result.isPresent());
        assertEquals("feuerwehr-musterstadt", result.get());
    }

    @Test
    void unknownSlug_shouldReturnEmpty() {
        DashboardProperties props = new DashboardProperties();
        props.setTenants(Map.of("a".repeat(64), "feuerwehr-musterstadt"));
        TenantResolver resolver = new TenantResolver(props);

        assertTrue(resolver.resolve("b".repeat(64)).isEmpty());
    }

    @Test
    void emptyTenantMap_shouldReturnEmpty() {
        DashboardProperties props = new DashboardProperties();
        TenantResolver resolver = new TenantResolver(props);

        assertTrue(resolver.resolve("anything").isEmpty());
    }

    @Test
    void nullSlug_shouldReturnEmpty() {
        DashboardProperties props = new DashboardProperties();
        props.setTenants(Map.of("a".repeat(64), "feuerwehr-musterstadt"));
        TenantResolver resolver = new TenantResolver(props);

        assertTrue(resolver.resolve(null).isEmpty());
    }
}