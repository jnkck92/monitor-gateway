package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.DashboardProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantResolver {

    private final DashboardProperties dashboardProperties;

    public Optional<String> resolve(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dashboardProperties.getTenants().get(slug));
    }
}