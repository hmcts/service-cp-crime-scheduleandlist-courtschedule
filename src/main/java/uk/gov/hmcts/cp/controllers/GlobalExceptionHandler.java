package uk.gov.hmcts.cp.controllers;

import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Tracer tracer;

    public GlobalExceptionHandler(final Tracer tracer) {
        this.tracer = tracer;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(final ResponseStatusException responseStatusException) {
        log.error("GlobalExceptionHandler handleResponseStatusException");
        final ErrorResponse error = ErrorResponse.builder()
                .error(String.valueOf(responseStatusException.getStatusCode().value()))
                .message(responseStatusException.getReason() != null
                        ? responseStatusException.getReason()
                        : responseStatusException.getMessage())
                .timestamp(Instant.now())
                .traceId(Objects.requireNonNull(tracer.currentSpan()).context().traceId())
                .build();

        return ResponseEntity
                .status(responseStatusException.getStatusCode())
                .body(error);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleServerException(final HttpServerErrorException e) {
        log.error("GlobalExceptionHandler handleServerException");
        final ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .timestamp(Instant.now())
                .traceId(Objects.requireNonNull(tracer.currentSpan()).context().traceId())
                .build();
        return ResponseEntity
                .status(e.getStatusCode())
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e) {
        log.error("GlobalExceptionHandler handleException");
        final ErrorResponse error = ErrorResponse.builder()
                .message(e.getMessage())
                .timestamp(Instant.now())
                .traceId(Objects.requireNonNull(tracer.currentSpan()).context().traceId())
                .build();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
