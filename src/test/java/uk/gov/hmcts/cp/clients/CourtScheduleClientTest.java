package uk.gov.hmcts.cp.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.HearingResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtScheduleClientTest {
    @Mock
    AppPropertiesBackend appProperties;
    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    private CourtScheduleClient courtScheduleClient;

    String courtId = "6c7fd04c-0dae-4c96-aaff-bc60f4e0d431";
    String courtRoomId = "1ef27392-ee5f-4b81-b783-a80b08037cbc";

    @Test
    void getHearingResponseByCaseUrn_shouldReturnValidResponse() {
        HearingResponse hearingResponse = Mockito.mock(HearingResponse.class);
        when(appProperties.getHearingsUrl()).thenReturn("http://localhost");
        when(appProperties.getHearingsPath()).thenReturn("/listing-query-api/query/api/rest/listing/hearings/allocated-and-unallocated");
        UUID caseUrn = UUID.randomUUID();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(HearingResponse.class)
        )).thenReturn(ResponseEntity.ok(hearingResponse));

        HearingResponse response = courtScheduleClient.getHearingResponse(caseUrn.toString());
        assertEquals(hearingResponse, response);
    }
}