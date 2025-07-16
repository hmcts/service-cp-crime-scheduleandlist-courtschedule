package uk.gov.hmcts.cp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.cp.services.CaseUrnMapperService;
import uk.gov.hmcts.cp.services.InMemoryCaseUrnMapper;

@Configuration
public class TestConfig {

    @Bean
    public CaseUrnMapperService caseUrnMapperService() {
        return new InMemoryCaseUrnMapper();
    }
}