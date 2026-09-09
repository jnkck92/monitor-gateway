package de.jkueck.gateway;

import de.jkueck.gateway.GatewayErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayErrorHandlerTest {

    private final GatewayErrorHandler handler = new GatewayErrorHandler();

    @Test
    void connectException_shouldReturn503() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/monitor/x/status").build()
        );

        handler.handle(exchange, new ConnectException("Connection refused")).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void timeout_shouldReturn503() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/monitor/x/status").build()
        );

        handler.handle(exchange, new TimeoutException("timeout")).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void ioException_shouldReturn503() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/monitor/x/status").build()
        );

        handler.handle(exchange, new IOException("broken pipe")).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void responseStatusException_shouldPreserveStatus() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/api/v1/monitor/x/slug").build()
        );

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "No static resource")).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    void unexpectedException_shouldReturn500() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/monitor/x/status").build()
        );

        handler.handle(exchange, new NullPointerException("bug")).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }

    @Test
    void alreadyCommittedResponse_shouldPropagateError() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/monitor/x/status").build()
        );
        exchange.getResponse().setComplete().block(); // Response committen

        Throwable original = new RuntimeException("already sent");

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                handler.handle(exchange, original).block());
    }
}