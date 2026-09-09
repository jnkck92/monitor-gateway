package de.jkueck.gateway.monitor;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MonitorRequestRewriter {

    private final VehicleParameterExtractor vehicleParameterExtractor;

    public ServerHttpRequest rewrite(ServerHttpRequest original, String restPath, String tenant, Optional<String> vehicle) {

        URI newUri = UriComponentsBuilder.fromUri(original.getURI())
                .replacePath("/api/v1/monitor/" + restPath)
                .replaceQueryParam(vehicleParameterExtractor.paramName())
                .build(true)
                .toUri();

        ServerHttpRequest.Builder builder = original.mutate()
                .uri(newUri)
                .header("X-Tenant", tenant);

        vehicle.ifPresent(v -> builder.header("X-Vehicle", v));

        return builder.build();
    }
}