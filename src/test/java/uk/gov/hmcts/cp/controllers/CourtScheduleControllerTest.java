package uk.gov.hmcts.cp.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponseCourtScheduleInner;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponseCourtScheduleInnerHearingsInner;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponseCourtScheduleInnerHearingsInnerCourtSittingsInner;
import uk.gov.hmcts.cp.services.CourtScheduleService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CourtScheduleControllerTest {

    private static final Logger log = LoggerFactory.getLogger(CourtScheduleControllerTest.class);

    @BeforeEach
    void setUp() {
    }

    @Test
    void getJudgeById_ShouldReturnJudgesWithOkStatus() {
        CourtScheduleController courtScheduleController = new CourtScheduleController(new CourtScheduleService());
        UUID caseUrn = UUID.randomUUID();
        log.info("Calling courtScheduleController.getCourtScheduleByCaseUrn with caseUrn: {}", caseUrn);
        ResponseEntity<?> response = courtScheduleController.getCourtScheduleByCaseUrn(caseUrn.toString());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        CourtScheduleResponse responseBody = (CourtScheduleResponse) response.getBody();
        assertNotNull(responseBody);
        assertNotNull(responseBody.getCourtSchedule());
        assertEquals(1, responseBody.getCourtSchedule().size());

        CourtScheduleResponseCourtScheduleInner schedule = responseBody.getCourtSchedule().get(0);
        assertNotNull(schedule.getHearings());
        assertEquals(1, schedule.getHearings().size());

        CourtScheduleResponseCourtScheduleInnerHearingsInner hearing = schedule.getHearings().get(0);
        assertNotNull(hearing.getHearingId());
        assertEquals("Requires interpreter", hearing.getListNote());
        assertEquals("Sentencing for theft case", hearing.getHearingDescription());
        assertEquals("Trial", hearing.getHearingType());
        assertNotNull(hearing.getCourtSittings());
        assertEquals(1, hearing.getCourtSittings().size());

        CourtScheduleResponseCourtScheduleInnerHearingsInnerCourtSittingsInner sitting =
                hearing.getCourtSittings().get(0);
        assertEquals("Central Criminal Court", sitting.getCourtHouse());
        assertNotNull(sitting.getSittingStart());
        assertTrue(sitting.getSittingEnd().isAfter(sitting.getSittingStart()));
        assertNotNull(sitting.getJudiciaryId());

    }

    @Test
    void getJudgeById_ShouldReturnBadRequestStatus() {
        CourtScheduleController courtScheduleController = new CourtScheduleController(new CourtScheduleService());

        log.info("Calling courtScheduleController.getCourtScheduleByCaseUrn with null caseUrn");
        ResponseEntity<?> response = courtScheduleController.getCourtScheduleByCaseUrn(null);
        log.debug("Received response: {}", response);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
} 