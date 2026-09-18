package de.jkueck.gateway.monitor;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class ApiRequestRewriter {

    public ServerHttpRequest rewrite(ServerHttpRequest original, String restPath, String tenant) {

        URI newUri = UriComponentsBuilder.fromUri(original.getURI())
                .replacePath("/api/v1/" + restPath)
                .build(true)
                .toUri();

        return original.mutate()
                .uri(newUri)
                .header("X-Tenant", tenant)
                .build();
    }
}