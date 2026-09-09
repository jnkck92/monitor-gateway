package de.jkueck.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "filter")
public class FilterProperties {

    private List<String> allowedPaths = List.of();

    private Slug slug = new Slug();

    private Vehicle vehicle = new Vehicle();

    @Getter
    @Setter
    public static class Slug {

        private String valid;

        private String pattern;

    }

    @Getter
    @Setter
    public static class Vehicle {

        private String paramName = "vehicle";

        private String pattern = "^[A-Za-z0-9_-]{1,32}$";

    }

}
