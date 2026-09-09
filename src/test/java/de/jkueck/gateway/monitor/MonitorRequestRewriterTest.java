package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import de.jkueck.gateway.monitor.MonitorRequestRewriter;
import de.jkueck.gateway.monitor.VehicleParameterExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MonitorRequestRewriterTest {

    private MonitorRequestRewriter rewriter;

    @BeforeEach
    void setUp() {
        FilterProperties props = new FilterProperties();
        props.getVehicle().setParamName("fahrzeug");
        props.getVehicle().setPattern("^[A-Za-z0-9_-]{1,32}$");

        VehicleParameterExtractor extractor = new VehicleParameterExtractor(props);
        extractor.init();

        rewriter = new MonitorRequestRewriter(extractor);
    }

    @Test
    void shouldRewritePathAndAddTenantHeader() {
        ServerHttpRequest original = MockServerHttpRequest
                .get("/api/v1/monitor/" + "a".repeat(64) + "/status")
                .build();

        ServerHttpRequest result = rewriter.rewrite(
                original, "status", "feuerwehr-musterstadt", Optional.empty());

        assertEquals("/api/v1/monitor/status", result.getURI().getPath());
        assertEquals("feuerwehr-musterstadt", result.getHeaders().getFirst("X-Tenant"));
        assertNull(result.getHeaders().getFirst("X-Vehicle"));
    }

    @Test
    void shouldAddVehicleHeaderWhenPresent() {
        ServerHttpRequest original = MockServerHttpRequest
                .get("/api/v1/monitor/" + "a".repeat(64) + "/status?fahrzeug=rw1")
                .build();

        ServerHttpRequest result = rewriter.rewrite(
                original, "status", "feuerwehr-musterstadt", Optional.of("rw1"));

        assertEquals("rw1", result.getHeaders().getFirst("X-Vehicle"));
    }

    @Test
    void shouldStripVehicleQueryParamFromForwardedRequest() {
        ServerHttpRequest original = MockServerHttpRequest
                .get("/api/v1/monitor/" + "a".repeat(64) + "/status?fahrzeug=rw1&other=keep")
                .build();

        ServerHttpRequest result = rewriter.rewrite(
                original, "status", "feuerwehr-musterstadt", Optional.of("rw1"));

        assertNull(result.getQueryParams().getFirst("fahrzeug"));
    }

    @Test
    void shouldPreserveOtherQueryParams() {
        ServerHttpRequest original = MockServerHttpRequest
                .get("/api/v1/monitor/" + "a".repeat(64) + "/status?fahrzeug=rw1&other=keep")
                .build();

        ServerHttpRequest result = rewriter.rewrite(
                original, "status", "feuerwehr-musterstadt", Optional.of("rw1"));

        assertEquals("keep", result.getQueryParams().getFirst("other"));
    }

    @Test
    void shouldHandleNestedRestPath() {
        ServerHttpRequest original = MockServerHttpRequest
                .get("/api/v1/monitor/" + "a".repeat(64) + "/stream/live")
                .build();

        ServerHttpRequest result = rewriter.rewrite(
                original, "stream/live", "feuerwehr-musterstadt", Optional.empty());

        assertEquals("/api/v1/monitor/stream/live", result.getURI().getPath());
    }
}