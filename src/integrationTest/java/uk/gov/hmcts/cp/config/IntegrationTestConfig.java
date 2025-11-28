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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.cp.controllers.CourtScheduleController;
import uk.gov.hmcts.cp.repositories.InMemoryCourtScheduleClientImpl;
import uk.gov.hmcts.cp.services.CaseUrnMapperService;
import uk.gov.hmcts.cp.services.CourtScheduleService;

import java.util.UUID;

@Configuration
public class IntegrationTestConfig {
    @Bean("courtScheduleService")
    public CourtScheduleService courtScheduleService() {
        return new CourtScheduleService(new InMemoryCourtScheduleClientImpl());
    }

    @Bean("testCaseUrnMapperService")
    public CaseUrnMapperService testCaseUrnMapperService() {
        final RestTemplate restTemplate = new RestTemplate();
        return new CaseUrnMapperService(restTemplate) {
            @Override
            public String getCaseMapperServiceUrl() {
                return "http://mock-server/test-mapper";
            }
            public String getCaseId(final String caseUrn) {
                return UUID.randomUUID().toString();
            }
        };
    }

    @Bean("courtScheduleController")
    public CourtScheduleController courtScheduleController(
            final CourtScheduleService courtScheduleService,
            final CaseUrnMapperService testCaseUrnMapperService) {
        return new CourtScheduleController(courtScheduleService, testCaseUrnMapperService);
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        final Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        io.opentelemetry.api.common.AttributeKey.stringKey("service.name"), 
                        "test-service"
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
    public Tracer tracer(final OpenTelemetry openTelemetry) {
        // Create real OpenTelemetry-based Tracer (same as Spring Boot 3)
        return new OtelTracer(
                openTelemetry.getTracerProvider().get("test"),
                new OtelCurrentTraceContext(),
                event -> {}
        );
    }

    // Spring boot 4 needs this to be created manually as auto-inject do not work
    @Bean
    public MockMvc mockMvc(@Autowired final WebApplicationContext webApplicationContext) {
        return MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
}

