package uk.gov.hmcts.cp.filters.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class TracingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String APPLICATION_NAME = "applicationName";

    private final String applicationName;
    private final Tracer tracer;

    public TracingFilter(@Value("${spring.application.name}") final String applicationName, final Tracer tracer) {
        super();
        this.applicationName = applicationName;
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        MDC.put(APPLICATION_NAME, applicationName);
        
        String traceId = request.getHeader(TRACE_ID);
        String spanId = request.getHeader(SPAN_ID);
        
        // If not in headers, try to get from Tracer (Spring Boot 4 auto-creates spans when tracing is enabled)
        if (traceId == null || spanId == null) {
            final Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                if (traceId == null) {
                    traceId = currentSpan.context().traceId();
        }
                if (spanId == null) {
                    spanId = currentSpan.context().spanId();
                }
            }
        }
        
        if (traceId != null) {
            MDC.put(TRACE_ID, traceId);
            response.setHeader(TRACE_ID, traceId);
        }
        if (spanId != null) {
            MDC.put(SPAN_ID, spanId);
            response.setHeader(SPAN_ID, spanId);
        }
        
        filterChain.doFilter(request, response);
    }
}
