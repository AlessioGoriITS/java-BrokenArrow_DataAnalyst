package it.alessiogori.battledebrief.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        ApiErrorCode error,
        String message,
        String path,
        Map<String, String> details
) {

    public ApiErrorResponse {
        details = details == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static ApiErrorResponse of(
            int status,
            ApiErrorCode error,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                Collections.emptyMap()
        );
    }

    public static ApiErrorResponse withDetails(
            int status,
            ApiErrorCode error,
            String message,
            String path,
            Map<String, String> details
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                path,
                details
        );
    }
}
