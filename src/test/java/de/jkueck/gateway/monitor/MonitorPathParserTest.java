package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import de.jkueck.gateway.monitor.MonitorPathMatch;
import de.jkueck.gateway.monitor.MonitorPathParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MonitorPathParserTest {

    private MonitorPathParser parser;

    @BeforeEach
    void setUp() {
        FilterProperties props = new FilterProperties();
        props.getSlug().setPattern("^/api/v1/monitor/([^/]+)/(.*)$");
        parser = new MonitorPathParser(props);
        parser.init();
    }

    @Test
    void validPath_shouldExtractSlugAndRest() {
        Optional<MonitorPathMatch> result = parser.parse("/api/v1/monitor/abc123/status");

        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().slug());
        assertEquals("status", result.get().restPath());
    }

    @Test
    void nestedRestPath_shouldCaptureFullRemainder() {
        Optional<MonitorPathMatch> result = parser.parse("/api/v1/monitor/abc123/stream/live");

        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().slug());
        assertEquals("stream/live", result.get().restPath());
    }

    @Test
    void emptyRestSegment_shouldStillMatchWithEmptyString() {
        // Pattern erlaubt (.*) = leer, aber "/" muss trotzdem vorhanden sein
        Optional<MonitorPathMatch> result = parser.parse("/api/v1/monitor/abc123/");

        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().slug());
        assertEquals("", result.get().restPath());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/monitor/",
            "/api/v1/monitor",
            "/api/v1/monitor/abc123",   // kein zweites Segment
            "/api/v2/monitor/abc123/status", // falsche Version
            "/other/path",
            ""
    })
    void malformedOrUnrelatedPaths_shouldReturnEmpty(String path) {
        assertTrue(parser.parse(path).isEmpty());
    }
}