package uk.gov.hmcts.cp.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAPIConfiguration {

    private final OpenAPIConfigurationLoader openAPIConfigLoader = new OpenAPIConfigurationLoader();

    @Bean
    public OpenAPI openAPI() {
        return openAPIConfigLoader.openAPI();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
