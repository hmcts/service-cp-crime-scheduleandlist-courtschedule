package uk.gov.hmcts.cp.controllers;

import io.micrometer.tracing.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Tracer tracer;

    public GlobalExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        ErrorResponse error = ErrorResponse.builder()
                .error(String.valueOf(e.getStatusCode().value()))
                .message(e.getReason() != null ? e.getReason() : e.getMessage())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .traceId(Objects.requireNonNull(tracer.currentSpan()).context().traceId())
                .build();

        return ResponseEntity
                .status(e.getStatusCode())
                .body(error);
    }
}