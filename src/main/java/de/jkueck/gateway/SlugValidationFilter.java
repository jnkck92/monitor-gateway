package de.jkueck.gateway;

import de.jkueck.gateway.config.DashboardProperties;
import de.jkueck.gateway.config.FilterProperties;
import jakarta.annotation.PostConstruct;
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

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlugValidationFilter implements GlobalFilter, Ordered {

    private final DashboardProperties properties;

    private final FilterProperties filterProperties;

    private Pattern slugPattern;

    private Pattern validSlug;

    @PostConstruct
    public void init() {
        slugPattern = Pattern.compile(filterProperties.getSlug().getPattern());
        validSlug = Pattern.compile(filterProperties.getSlug().getValid());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        Set<String> allowedPaths = Set.copyOf(filterProperties.getAllowedPaths());

        String path = exchange.getRequest().getURI().getPath();
        Matcher matcher = slugPattern.matcher(path);

        if (matcher.matches()) {
            String slug = matcher.group(1);
            String rest = matcher.group(2);

            if (!validSlug.matcher(slug).matches()) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }

            if (!properties.getTenants().containsKey(slug)) {
                log.warn("Invalid slug attempt: ip={}", exchange.getRequest().getRemoteAddress());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String tenant = properties.getTenants().get(slug);

            log.info("Request: tenant={} path={} ip={}", tenant, rest, exchange.getRequest().getRemoteAddress());

            if (!allowedPaths.contains(rest)) {
                log.warn("Blocked path attempt: tenant={} path={}", tenant, rest);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .path("/api/v1/monitor/" + rest)
                    .header("X-Tenant", tenant)
                    .build();

            exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("Cache-Control", "no-store");

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}