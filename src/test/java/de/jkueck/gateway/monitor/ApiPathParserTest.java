package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ApiPathParserTest {

    private ApiPathParser parser;

    @BeforeEach
    void setUp() {
        FilterProperties props = new FilterProperties();
        props.getSlug().setApiPattern("^/api/v1/([^/]+)/(news|events)$");
        parser = new ApiPathParser(props);
        parser.init();
    }

    @Test
    void validNewsPath_shouldExtractSlugAndResource() {
        Optional<MonitorPathMatch> result = parser.parse("/api/v1/abc123/news");

        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().slug());
        assertEquals("news", result.get().restPath());
    }

    @Test
    void validEventsPath_shouldExtractSlugAndResource() {
        Optional<MonitorPathMatch> result = parser.parse("/api/v1/abc123/events");

        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().slug());
        assertEquals("events", result.get().restPath());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/abc123/",
            "/api/v1/abc123",
            "/api/v1/abc123/status",          // nicht erlaubte Resource
            "/api/v1/abc123/news/extra",       // zu viele Segmente
            "/api/v1/monitor/abc123/news",     // monitor-Präfix statt slug
            "/api/v2/abc123/news",             // falsche Version
            "/other/path",
            ""
    })
    void malformedOrUnrelatedPaths_shouldReturnEmpty(String path) {
        assertTrue(parser.parse(path).isEmpty());
    }
}