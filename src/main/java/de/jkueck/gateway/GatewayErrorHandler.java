package de.jkueck.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        boolean backendUnreachable = ex instanceof IOException || ex instanceof TimeoutException;

        HttpStatus status = backendUnreachable ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.INTERNAL_SERVER_ERROR;

        if (backendUnreachable) {
            log.warn("backend not reachable: {}", ex.toString());
        } else {
            log.error("unexpected gateway error", ex);
        }

        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        return response.writeWith(Mono.empty());
    }
}