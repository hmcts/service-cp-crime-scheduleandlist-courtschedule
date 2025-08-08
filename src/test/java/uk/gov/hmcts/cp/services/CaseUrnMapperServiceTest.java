package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaseUrnMapperServiceTest {
    private CaseUrnMapperService caseUrnMapperService;

    private RestTemplate restTemplate;

    private final String mockUrl = "http://mock-server/mapper";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        caseUrnMapperService = new CaseUrnMapperService(restTemplate) {
            @Override
            public String getCaseMapperServiceUrl() {
                return mockUrl ;
            }
        };
    }

    @Test
    void shouldReturnCaseIdWhenResponseIsSuccessful() {
        String caseUrn = "test-case-urn";
        String caseId = "7a2e94c4-38af-43dd-906b-40d632d159b0";

        CaseMapperResponse response = CaseMapperResponse.builder()
                .caseId(caseId)
                .caseUrn(caseUrn)
                .build();
        ResponseEntity<CaseMapperResponse> responseEntity = ResponseEntity.ok(response);
        when(restTemplate.exchange(
                eq(mockUrl + "/" + caseUrn),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CaseMapperResponse.class)
        )).thenReturn(responseEntity);

        String result = caseUrnMapperService.getCaseId(caseUrn);

        assertEquals(caseId, result);
    }

    @Test
    void shouldReturnNullWhenExceptionOccurs() {
        String caseUrn = "test-case-urn";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CaseMapperResponse.class)
        )).thenThrow(new RuntimeException("Test exception"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            caseUrnMapperService.getCaseId(caseUrn);
        });
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Case not found by urn: test-case-urn");
        assertThat(exception.getMessage()).isEqualTo("404 NOT_FOUND \"Case not found by urn: test-case-urn\"");
    }
}