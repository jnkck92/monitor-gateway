package de.jkueck.gateway.monitor;

import de.jkueck.gateway.config.FilterProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class VehicleParameterExtractor {

    private final FilterProperties filterProperties;
    private Pattern vehiclePattern;

    @PostConstruct
    void init() {
        vehiclePattern = Pattern.compile(filterProperties.getVehicle().getPattern());
    }

    public String paramName() {
        return filterProperties.getVehicle().getParamName();
    }

    public Result extract(ServerHttpRequest request) {
        String raw = request.getQueryParams().getFirst(paramName());
        if (raw == null) {
            return Result.absent();
        }
        boolean valid = vehiclePattern.matcher(raw).matches();
        return valid ? Result.present(raw) : Result.invalid();
    }

    public record Result(State state, String value) {
        public enum State { ABSENT, VALID, INVALID }

        public static Result absent() { return new Result(State.ABSENT, null); }

        public static Result present(String value) { return new Result(State.VALID, value); }

        public static Result invalid() { return new Result(State.INVALID, null); }

        public boolean isInvalid() { return state == State.INVALID; }

        public Optional<String> asOptional() {
            return state == State.VALID ? Optional.of(value) : Optional.empty();
        }
    }
}