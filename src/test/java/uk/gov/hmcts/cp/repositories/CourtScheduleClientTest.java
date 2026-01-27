package uk.gov.hmcts.cp.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.httpclients.CourtScheduleClientImpl;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private CourtScheduleClientImpl courtScheduleClient;

    String courtId = "6c7fd04c-0dae-4c96-aaff-bc60f4e0d431";
    String courtRoomId = "1ef27392-ee5f-4b81-b783-a80b08037cbc";

    @Test
    void getCourtScheduleByCaseUrn_shouldReturnCourtScheduleResponse() {
        when(appProperties.getHearingsUrl()).thenReturn("http://localhost");
        when(appProperties.getHearingsPath()).thenReturn("/listing-query-api/query/api/rest/listing/hearings/allocated-and-unallocated");
        UUID caseUrn = UUID.randomUUID();
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(dummyHearing())).build();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(HearingResponse.class)
        )).thenReturn(ResponseEntity.ok(hearingResponse));

        CourtScheduleResponse response = courtScheduleClient.getCourtScheduleByCaseId(caseUrn.toString());

        assertNotNull(response.getCourtSchedule());
        assertEquals(1, response.getCourtSchedule().size());

        CourtSchedule schedule = response.getCourtSchedule().get(0);
        assertNotNull(schedule.getHearings());
        assertEquals(1, schedule.getHearings().size());

        Hearing hearing = schedule.getHearings().get(0);
        assertNotNull(hearing.getHearingId());
        // TODO COLING remove hard coding and map from CP
        assertEquals("sample list note", hearing.getListNote());
        assertEquals("Plea and Trial Preparation", hearing.getHearingDescription());
        assertEquals("Plea and Trial Preparation", hearing.getHearingType());
        assertNotNull(hearing.getCourtSittings());
        assertEquals(1, hearing.getCourtSittings().size());

        CourtSitting sitting =
                hearing.getCourtSittings().get(0);
        assertEquals(courtId, sitting.getCourtHouse());
        assertNotNull(sitting.getSittingStart());
        assertTrue(sitting.getSittingEnd().isAfter(sitting.getSittingStart()));
        assertNotNull(sitting.getJudiciaryId());
    }

    private HearingResponse.HearingSchedule dummyHearing() {
        // TODO COLING implement mapper then we dont need a populated object
        HearingResponse.HearingSchedule.HearingDay hearingDay = HearingResponse.HearingSchedule.HearingDay.builder()
                .startTime("2026-01-21T11:00:00Z")
                .endTime("2026-01-21T11:20:00Z")
                .courtCentreId(courtId)
                .courtRoomId(courtRoomId)
                .build();
        return HearingResponse.HearingSchedule.builder()
                .id(UUID.randomUUID().toString())
                .allocated(true)
                .type(HearingResponse.HearingSchedule.HearingType.builder().description("Plea and Trial Preparation").build())
                .judiciary(List.of())
                .hearingDays(List.of(hearingDay))
                .build();
    }
}