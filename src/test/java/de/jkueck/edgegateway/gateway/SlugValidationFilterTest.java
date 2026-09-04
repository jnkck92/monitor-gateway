package de.jkueck.edgegateway.gateway;

import de.jkueck.gateway.SlugValidationFilter;
import de.jkueck.gateway.config.DashboardProperties;
import de.jkueck.gateway.config.FilterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlugValidationFilterTest {

    @Mock
    private DashboardProperties dashboardProperties;

    @Mock
    private FilterProperties filterProperties;

    @InjectMocks
    private SlugValidationFilter filter;

    @BeforeEach
    void setUp() {
        FilterProperties.Slug slug = new FilterProperties.Slug();
        slug.setValid("^[a-f0-9]{64}$");
        slug.setPattern("^/api/v1/monitor/([^/]+)/(.*)$");

        when(filterProperties.getSlug()).thenReturn(slug);
        when(filterProperties.getAllowedPaths()).thenReturn(List.of("status", "stream"));
        lenient().when(dashboardProperties.getTenants()).thenReturn(Map.of(
                "a".repeat(64), "feuerwehr-musterstadt"
        ));

        filter.init();
    }

    @Test
    void validSlug_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/monitor/" + "a".repeat(64) + "/status").build()
        );

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        assertEquals("nosniff", exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", exchange.getResponse().getHeaders().getFirst("X-Frame-Options"));
        assertEquals("no-store", exchange.getResponse().getHeaders().getFirst("Cache-Control"));
    }

    @Test
    void invalidSlugFormat_shouldReturnBadRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/monitor/invalid-slug/status").build()
        );

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    }

    @Test
    void unknownSlug_shouldReturnUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/monitor/" + "b".repeat(64) + "/status").build()
        );

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void disallowedPath_shouldReturnForbidden() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/monitor/" + "a".repeat(64) + "/admin").build()
        );

        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void validSlug_shouldStripSlugFromPath() {
        String slug = "a".repeat(64);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/monitor/" + slug + "/status").build()
        );

        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("/api/v1/monitor/status",
            capturedExchange.get().getRequest().getURI().getPath());
    }

    @Test
    void validSlug_shouldAddTenantHeader() {
        String slug = "a".repeat(64);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/monitor/" + slug + "/status").build()
        );

        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals("feuerwehr-musterstadt",
            capturedExchange.get().getRequest().getHeaders().getFirst("X-Tenant"));
    }
}