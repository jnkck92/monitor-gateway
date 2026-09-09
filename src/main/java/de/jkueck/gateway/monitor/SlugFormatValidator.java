package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SlugFormatValidator {

    private final FilterProperties filterProperties;
    private Pattern validSlugPattern;

    @PostConstruct
    void init() {
        validSlugPattern = Pattern.compile(filterProperties.getSlug().getValid());
    }

    public boolean isValid(String slug) {
        return validSlugPattern.matcher(slug).matches();
    }
}