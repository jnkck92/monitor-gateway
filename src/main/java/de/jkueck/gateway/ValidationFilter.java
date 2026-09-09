package de.jkueck.gateway;

import de.jkueck.gateway.monitor.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationFilter implements GlobalFilter, Ordered {

    private final MonitorPathParser pathParser;

    private final SlugFormatValidator slugFormatValidator;

    private final TenantResolver tenantResolver;

    private final PathAuthorizationService pathAuthorizationService;

    private final VehicleParameterExtractor vehicleParameterExtractor;

    private final MonitorRequestRewriter requestRewriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        Optional<MonitorPathMatch> match = pathParser.parse(path);
        if (match.isEmpty()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        String slug = match.get().slug();
        String rest = match.get().restPath();

        if (!slugFormatValidator.isValid(slug)) {
            return reject(exchange, HttpStatus.BAD_REQUEST);
        }

        Optional<String> tenantOpt = tenantResolver.resolve(slug);
        if (tenantOpt.isEmpty()) {
            log.warn("Invalid slug attempt: ip={}", exchange.getRequest().getRemoteAddress());
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }
        String tenant = tenantOpt.get();

        log.info("Request: tenant={} path={} ip={}", tenant, rest, exchange.getRequest().getRemoteAddress());

        if (!pathAuthorizationService.isAllowed(rest)) {
            log.warn("Blocked path attempt: tenant={} path={}", tenant, rest);
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        VehicleParameterExtractor.Result vehicleResult =
                vehicleParameterExtractor.extract(exchange.getRequest());

        if (vehicleResult.isInvalid()) {
            log.warn("Invalid vehicle parameter attempt: tenant={} ip={}",
                    tenant, exchange.getRequest().getRemoteAddress());
            return reject(exchange, HttpStatus.BAD_REQUEST);
        }

        vehicleResult.asOptional().ifPresent(v -> log.info("Vehicle assigned: tenant={} vehicle={}", tenant, v));

        ServerHttpRequest mutatedRequest = requestRewriter.rewrite(
                exchange.getRequest(), rest, tenant, vehicleResult.asOptional());

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}