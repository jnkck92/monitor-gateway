package de.jkueck;

import de.jkueck.gateway.config.DashboardProperties;
import de.jkueck.gateway.config.FilterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DashboardProperties.class, FilterProperties.class})
public class EdgeGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdgeGatewayApplication.class, args);
    }

}
