package uk.gov.hmcts.cp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.*;
import uk.gov.hmcts.cp.services.CaseUrnMapperService;
import uk.gov.hmcts.cp.services.CourtScheduleService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class CourtScheduleControllerTest {

    @Mock
    private CourtScheduleService courtScheduleService;

    @Mock
    private CaseUrnMapperService caseUrnMapperService;

    @InjectMocks
    private CourtScheduleController courtScheduleController;

    @Test
    void getCourtScheduleByCaseUrn_should_returnOkStatusWithJudges() {
        UUID caseUrn = UUID.randomUUID();
        String caseId = UUID.randomUUID().toString();
        String hearingId = UUID.randomUUID().toString();
        String judiciaryId = UUID.randomUUID().toString();
        Instant sittingStartTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        CourtScheduleResponse mockResponse = CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                        CourtSchedule.builder()
                                .hearings(List.of(
                                        Hearing.builder()
                                                .hearingId(hearingId)
                                                .listNote("Requires interpreter")
                                                .hearingDescription("Sentencing for theft case")
                                                .hearingType("Trial")
                                                .courtSittings(List.of(
                                                        CourtSitting.builder()
                                                                .courtHouse("Central Criminal Court")
                                                                .sittingStart(sittingStartTime)
                                                                .sittingEnd(sittingStartTime.plus(60, ChronoUnit.MINUTES))
                                                                .judiciaryId(judiciaryId)
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(caseUrnMapperService.getCaseId(anyString())).thenReturn(caseId);
        when(courtScheduleService.getCourtScheduleByCaseId(caseId)).thenReturn(mockResponse);

        ResponseEntity<?> response = courtScheduleController.getCourtScheduleByCaseUrn(caseUrn.toString());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        CourtScheduleResponse responseBody = (CourtScheduleResponse) response.getBody();
        assertNotNull(responseBody);
        assertNotNull(responseBody.getCourtSchedule());
        assertEquals(1, responseBody.getCourtSchedule().size());

        CourtSchedule courtSchedule = responseBody.getCourtSchedule().getFirst();
        assertNotNull(courtSchedule.getHearings());
        assertEquals(1, courtSchedule.getHearings().size());

        Hearing hearing = courtSchedule.getHearings().getFirst();
        assertNotNull(hearing.getHearingId());
        assertEquals("Requires interpreter", hearing.getListNote());
        assertEquals("Sentencing for theft case", hearing.getHearingDescription());
        assertEquals("Trial", hearing.getHearingType());
        assertNotNull(hearing.getCourtSittings());
        assertEquals(1, hearing.getCourtSittings().size());

        CourtSitting courtSitting =
                hearing.getCourtSittings().getFirst();
        assertEquals("Central Criminal Court", courtSitting.getCourtHouse());
        assertNotNull(courtSitting.getSittingStart());
        assertTrue(courtSitting.getSittingEnd().isAfter(courtSitting.getSittingStart()));
        assertNotNull(courtSitting.getJudiciaryId());
    }

    @Test
    void getCourtScheduleByCaseUrn_should_sanitizeCaseUrn() {
        String unsanitizedCaseUrn = "<script>alert('xss')</script>";
        String caseId = UUID.randomUUID().toString();
        CourtScheduleResponse mockResponse = CourtScheduleResponse.builder()
                .courtSchedule(List.of())
                .build();

        lenient().when(caseUrnMapperService.getCaseId(anyString())).thenReturn(caseId);
        lenient().when(courtScheduleService.getCourtScheduleByCaseId(caseId)).thenReturn(mockResponse);

        ResponseEntity<?> response = courtScheduleController.getCourtScheduleByCaseUrn(unsanitizedCaseUrn);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCourtScheduleByCaseUrn_should_returnBadRequestWhenCaseUrnIsNull() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            courtScheduleController.getCourtScheduleByCaseUrn(null);
        });
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("caseUrn is required");
        assertThat(exception.getMessage()).isEqualTo("400 BAD_REQUEST \"caseUrn is required\"");
    }
}