package de.jkueck.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "dashboard")
public class DashboardProperties {

    private Map<String, String> tenants = new HashMap<>();

}