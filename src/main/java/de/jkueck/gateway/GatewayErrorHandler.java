package de.jkueck.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.io.IOException;
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

        HttpStatusCode status;

        if (ex instanceof ResponseStatusException rse) {
            status = rse.getStatusCode();
            log.warn("Unhandled route/resource error: path={} status={}", exchange.getRequest().getURI().getPath(), status);
        } else if (ex instanceof IOException || ex instanceof TimeoutException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            log.warn("Backend nicht erreichbar: {}", ex.toString());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("Unerwarteter Gateway-Fehler", ex);
        }

        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        return response.writeWith(Mono.empty());
    }
}