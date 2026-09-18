package de.jkueck.gateway;

import de.jkueck.gateway.monitor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationFilterTest {

    @Mock private MonitorPathParser pathParser;
    @Mock private ApiPathParser apiPathParser;
    @Mock private SlugFormatValidator slugFormatValidator;
    @Mock private TenantResolver tenantResolver;
    @Mock private PathAuthorizationService pathAuthorizationService;
    @Mock private VehicleParameterExtractor vehicleParameterExtractor;
    @Mock private MonitorRequestRewriter requestRewriter;
    @Mock private ApiRequestRewriter apiRequestRewriter;

    @InjectMocks
    private ValidationFilter filter;

    private MockServerWebExchange exchange;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/monitor/x/status").build());
        chain = mock(GatewayFilterChain.class);
    }

    // --- Monitor-Flow ---

    @Test
    void pathDoesNotMatch_shouldReturnUnauthorized() {
        when(pathParser.parse(any())).thenReturn(Optional.empty());
        when(apiPathParser.parse(any())).thenReturn(Optional.empty());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidSlugFormat_shouldReturnBadRequest() {
        when(pathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("bad", "status")));
        when(slugFormatValidator.isValid("bad")).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void unknownTenant_shouldReturnUnauthorized() {
        when(pathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "status")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.empty());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void disallowedPath_shouldReturnForbidden() {
        when(pathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "admin")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("admin")).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void invalidVehicle_shouldReturnBadRequest() {
        when(pathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "status")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("status")).thenReturn(true);
        when(vehicleParameterExtractor.extract(any()))
                .thenReturn(VehicleParameterExtractor.Result.invalid());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void allValid_shouldDelegateToChainWithRewrittenRequest() {
        when(pathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "status")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("status")).thenReturn(true);
        when(vehicleParameterExtractor.extract(any()))
                .thenReturn(VehicleParameterExtractor.Result.absent());

        ServerHttpRequest rewritten = MockServerHttpRequest.get("/api/v1/monitor/status").build();
        when(requestRewriter.rewrite(any(), eq("status"), eq("tenant-a"), eq(Optional.empty())))
                .thenReturn(rewritten);

        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verify(requestRewriter).rewrite(any(), eq("status"), eq("tenant-a"), eq(Optional.empty()));
    }

    @Test
    void allValidWithVehicle_shouldPassVehicleToRewriter() {
        when(pathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "status")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("status")).thenReturn(true);
        when(vehicleParameterExtractor.extract(any()))
                .thenReturn(VehicleParameterExtractor.Result.present("rw1"));

        ServerHttpRequest rewritten = MockServerHttpRequest.get("/api/v1/monitor/status").build();
        when(requestRewriter.rewrite(any(), eq("status"), eq("tenant-a"), eq(Optional.of("rw1"))))
                .thenReturn(rewritten);

        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(requestRewriter).rewrite(any(), eq("status"), eq("tenant-a"), eq(Optional.of("rw1")));
    }

    // --- Api-Flow (news/events) ---

    @Test
    void apiPath_invalidSlugFormat_shouldReturnBadRequest() {
        when(pathParser.parse(any())).thenReturn(Optional.empty());
        when(apiPathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("bad", "news")));
        when(slugFormatValidator.isValid("bad")).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apiPath_unknownTenant_shouldReturnUnauthorized() {
        when(pathParser.parse(any())).thenReturn(Optional.empty());
        when(apiPathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "news")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.empty());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apiPath_disallowedResource_shouldReturnForbidden() {
        when(pathParser.parse(any())).thenReturn(Optional.empty());
        when(apiPathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "admin")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("admin")).thenReturn(false);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void apiPath_news_allValid_shouldDelegateToChainWithRewrittenRequest() {
        when(pathParser.parse(any())).thenReturn(Optional.empty());
        when(apiPathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "news")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("news")).thenReturn(true);

        ServerHttpRequest rewritten = MockServerHttpRequest.get("/api/v1/news").build();
        when(apiRequestRewriter.rewrite(any(), eq("news"), eq("tenant-a")))
                .thenReturn(rewritten);

        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verify(apiRequestRewriter).rewrite(any(), eq("news"), eq("tenant-a"));
        verify(vehicleParameterExtractor, never()).extract(any());
    }

    @Test
    void apiPath_events_allValid_shouldDelegateToChainWithRewrittenRequest() {
        when(pathParser.parse(any())).thenReturn(Optional.empty());
        when(apiPathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "events")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("events")).thenReturn(true);

        ServerHttpRequest rewritten = MockServerHttpRequest.get("/api/v1/events").build();
        when(apiRequestRewriter.rewrite(any(), eq("events"), eq("tenant-a")))
                .thenReturn(rewritten);

        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verify(apiRequestRewriter).rewrite(any(), eq("events"), eq("tenant-a"));
    }

    @Test
    void monitorMatchTakesPrecedenceOverApiMatch() {
        // Sicherstellen, dass bei einem Treffer im Monitor-Parser der Api-Parser gar nicht erst befragt wird
        when(pathParser.parse(any())).thenReturn(Optional.of(new MonitorPathMatch("slug", "status")));
        when(slugFormatValidator.isValid("slug")).thenReturn(true);
        when(tenantResolver.resolve("slug")).thenReturn(Optional.of("tenant-a"));
        when(pathAuthorizationService.isAllowed("status")).thenReturn(true);
        when(vehicleParameterExtractor.extract(any()))
                .thenReturn(VehicleParameterExtractor.Result.absent());

        ServerHttpRequest rewritten = MockServerHttpRequest.get("/api/v1/monitor/status").build();
        when(requestRewriter.rewrite(any(), eq("status"), eq("tenant-a"), eq(Optional.empty())))
                .thenReturn(rewritten);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(apiPathParser, never()).parse(any());
    }
}