package uk.gov.hmcts.cp.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;

@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class AppConfig {

    @Value("${spring.application.name:service-cp-crime-scheduleandlist-courtschedule}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean(Tracer.class) // To allow integration test to override
    public Tracer tracer(@Autowired(required = false) final OpenTelemetry openTelemetry) {
        // Use auto-configured OpenTelemetry if available, otherwise create a default one
        final OpenTelemetry otel = openTelemetry != null
            ? openTelemetry 
            : createDefaultOpenTelemetry();
        
        return new OtelTracer(
            otel.getTracerProvider().get(applicationName),
            new OtelCurrentTraceContext(),
            event -> {}
        );
    }

    private OpenTelemetry createDefaultOpenTelemetry() {
        final Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                io.opentelemetry.api.common.AttributeKey.stringKey("service.name"), 
                applicationName
            )));
        
        final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(SpanExporter.composite()))
            .setResource(resource)
            .build();
        
        return OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.noop())
            .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }
}

