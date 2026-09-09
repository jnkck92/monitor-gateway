package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MonitorPathParser {

    private final FilterProperties filterProperties;
    private Pattern pattern;

    @PostConstruct
    void init() {
        pattern = Pattern.compile(filterProperties.getSlug().getPattern());
    }

    public Optional<MonitorPathMatch> parse(String path) {
        Matcher matcher = pattern.matcher(path);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new MonitorPathMatch(matcher.group(1), matcher.group(2)));
    }
}