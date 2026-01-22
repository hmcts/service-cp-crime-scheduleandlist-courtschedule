package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseUrnMapperServiceTest {
    @Mock
    private RestTemplate restTemplate;

    private CaseUrnMapperService caseUrnMapperService;

    private final String mockUrl = "http://mock-server/mapper";
    private final String mockPath = "/urnmapper";

    private CaseUrnMapperService createCaseUrnMapperService() {
        return new CaseUrnMapperService(restTemplate) {
            @Override
            public String getCaseMapperServiceUrl() {
                return mockUrl;
            }

            @Override
            public String getCaseMapperServicePath() {
                return mockPath;
            }
        };
    }

    @Test
    void do_getCaseId_should_returnCaseIdWhenResponseIsSuccessful() {
        caseUrnMapperService = createCaseUrnMapperService();
        String caseUrn = "test-case-urn";
        String caseId = "7a2e94c4-38af-43dd-906b-40d632d159b0";

        CaseMapperResponse response = CaseMapperResponse.builder()
                .caseId(caseId)
                .caseUrn(caseUrn)
                .build();
        ResponseEntity<CaseMapperResponse> responseEntity = ResponseEntity.ok(response);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CaseMapperResponse.class)
        )).thenReturn(responseEntity);

        String result = caseUrnMapperService.getCaseId(caseUrn);

        assertEquals(caseId, result);
    }

    @Test
    void do_getCaseId_should_throwResponseStatusExceptionWhenExceptionOccurs() {
        caseUrnMapperService = createCaseUrnMapperService();
        String caseUrn = "test-case-urn";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CaseMapperResponse.class)
        )).thenThrow(new RestClientException("Test exception"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            caseUrnMapperService.getCaseId(caseUrn);
        });
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Case not found by urn: test-case-urn");
        assertThat(exception.getMessage()).isEqualTo("404 NOT_FOUND \"Case not found by urn: test-case-urn\"");
    }
}