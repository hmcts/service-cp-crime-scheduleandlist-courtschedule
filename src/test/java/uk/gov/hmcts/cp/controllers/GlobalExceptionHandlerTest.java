package uk.gov.hmcts.cp.controllers;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.ErrorResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private TraceContext context;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void handleResponseStatusException_should_returnErrorResponseWithCorrectFields() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("test-trace-id");

        String reason = "Test error";
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.NOT_FOUND, reason);

        // Act
        var response = handler.handleResponseStatusException(ex);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse error = response.getBody();
        assertNotNull(error);
        assertEquals("404", error.getError());
        assertEquals(reason, error.getMessage());

        assertNotNull(error.getTimestamp());
        assertTrue(error.getTimestamp() instanceof Instant);

        assertEquals("test-trace-id", error.getTraceId());
    }

    @Test
    void handleException_should_returnInternalServerError() {
        // Arrange
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("test-trace-id");

        RuntimeException ex = new RuntimeException("Unexpected error");

        // Act
        var response = handler.handleException(ex);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ErrorResponse error = response.getBody();
        assertNotNull(error);
        assertEquals("Unexpected error", error.getMessage());
        assertNotNull(error.getTimestamp());
        assertTrue(error.getTimestamp() instanceof Instant);
        assertEquals("test-trace-id", error.getTraceId());
    }
}
