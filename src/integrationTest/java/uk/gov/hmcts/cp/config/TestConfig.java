package uk.gov.hmcts.cp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.controllers.CourtScheduleController;
import uk.gov.hmcts.cp.repositories.InMemoryCourtScheduleClientImpl;
import uk.gov.hmcts.cp.services.CaseUrnMapperService;
import uk.gov.hmcts.cp.services.CourtScheduleService;

import java.util.UUID;

@Configuration
public class TestConfig {
    @Bean("courtScheduleService")
    public CourtScheduleService courtScheduleService() {
        return new CourtScheduleService(new InMemoryCourtScheduleClientImpl());
    }

    @Bean("testCaseUrnMapperService")
    public CaseUrnMapperService testCaseUrnMapperService() {
        RestTemplate restTemplate = new RestTemplate();
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
            CourtScheduleService courtScheduleService,
            CaseUrnMapperService testCaseUrnMapperService) {
        return new CourtScheduleController(courtScheduleService, testCaseUrnMapperService);
    }
}