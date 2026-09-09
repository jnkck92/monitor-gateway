package de.jkueck.gateway.security;

import de.jkueck.gateway.security.SecurityHeaderFilter;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHeaderFilterTest {

    private final SecurityHeaderFilter filter = new SecurityHeaderFilter();

    @Test
    void shouldSetAllSecurityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/anything").build());

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("nosniff", exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", exchange.getResponse().getHeaders().getFirst("X-Frame-Options"));
        assertEquals("no-store", exchange.getResponse().getHeaders().getFirst("Cache-Control"));
    }

    @Test
    void shouldDelegateToChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/anything").build());

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        org.mockito.Mockito.verify(chain).filter(exchange);
    }

    @Test
    void order_shouldBeHighestPrecedence() {
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }
}