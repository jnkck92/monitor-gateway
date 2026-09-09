package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import de.jkueck.gateway.monitor.VehicleParameterExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class VehicleParameterExtractorTest {

    private VehicleParameterExtractor extractor;

    @BeforeEach
    void setUp() {
        FilterProperties props = new FilterProperties();
        props.getVehicle().setParamName("fahrzeug");
        props.getVehicle().setPattern("^[A-Za-z0-9_-]{1,32}$");
        extractor = new VehicleParameterExtractor(props);
        extractor.init();
    }

    @Test
    void noParam_shouldReturnAbsent() {
        ServerHttpRequest request = MockServerHttpRequest.get("/api/v1/monitor/x/status").build();

        VehicleParameterExtractor.Result result = extractor.extract(request);

        assertEquals(VehicleParameterExtractor.Result.State.ABSENT, result.state());
        assertTrue(result.asOptional().isEmpty());
        assertFalse(result.isInvalid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rw1", "RW-123", "abc_def", "A", "1"})
    void validParam_shouldReturnValid(String vehicle) {
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/monitor/x/status?fahrzeug=" + vehicle)
                .build();

        VehicleParameterExtractor.Result result = extractor.extract(request);

        assertEquals(VehicleParameterExtractor.Result.State.VALID, result.state());
        assertEquals(vehicle, result.asOptional().orElseThrow());
        assertFalse(result.isInvalid());
    }

    @Test
    void emptyParam_shouldBeInvalid() {
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/monitor/x/status?fahrzeug=")
                .build();

        VehicleParameterExtractor.Result result = extractor.extract(request);

        assertTrue(result.isInvalid());
    }

    @Test
    void tooLongParam_shouldBeInvalid() {
        String longValue = "x".repeat(33);
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/monitor/x/status?fahrzeug=" + longValue)
                .build();

        VehicleParameterExtractor.Result result = extractor.extract(request);

        assertTrue(result.isInvalid());
    }

    @Test
    void specialCharacters_shouldBeInvalid() {
        ServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/monitor/x/status?fahrzeug=%3Cscript%3E")
                .build();

        VehicleParameterExtractor.Result result = extractor.extract(request);

        assertTrue(result.isInvalid());
    }

    @Test
    void paramName_shouldBeConfigurable() {
        assertEquals("fahrzeug", extractor.paramName());
    }
}