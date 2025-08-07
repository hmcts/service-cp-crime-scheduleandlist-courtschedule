package uk.gov.hmcts.cp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class CaseUrnMapperServiceTest {
    private CaseUrnMapperService caseUrnMapperService;

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;

    private final String mockUrl = "http://mock-server/mapper";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        caseUrnMapperService = new CaseUrnMapperService(restTemplate, objectMapper) {
            @Override
            public String getCaseMapperServiceUrl() {
                return mockUrl ;
            }
        };
    }

    //@Test  uncoment when casemapperserivce method is uncommented
    void shouldReturnCaseIdWhenResponseIsSuccessful() {
        String caseUrn = "test-case-urn";
        String caseId = "7a2e94c4-38af-43dd-906b-40d632d159b0";

        String json = """
        {
          "caseId": "7a2e94c4-38af-43dd-906b-40d632d159b0",
          "caseUrn": "28DI1953715",
          "originalResponse": {
            "mappingId": "818284bf-a1ed-464b-bc1a-b0298cf1ead1",
            "sourceId": "28DI1953715",
            "sourceType": "OU_URN",
            "targetId": "7a2e94c4-38af-43dd-906b-40d632d159b0",
            "targetType": "CASE_FILE_ID",
            "createdAt": "2025-07-31T10:07:46.578Z"
          }
        }
        """;
        ResponseEntity<String> responseEntity = ResponseEntity.ok(json);
        when(restTemplate.exchange(
                eq(mockUrl + "/" + caseUrn),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        String result = caseUrnMapperService.getCaseId(caseUrn);

        assertEquals(caseId, result);
    }

    //@Test  uncoment when casemapperserivce method is uncommented
    void shouldReturnNullWhenExceptionOccurs() {
        String caseUrn = "test-case-urn";

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Test exception"));

        String result = caseUrnMapperService.getCaseId(caseUrn);
        assertNull(result);
    }
}