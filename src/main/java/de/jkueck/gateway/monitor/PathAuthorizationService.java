package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PathAuthorizationService {

    private final FilterProperties filterProperties;

    public boolean isAllowed(String restPath) {
        return Set.copyOf(filterProperties.getAllowedPaths()).contains(restPath);
    }
}