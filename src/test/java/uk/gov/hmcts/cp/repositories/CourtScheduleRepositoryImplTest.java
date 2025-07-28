package uk.gov.hmcts.cp.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class CourtScheduleRepositoryImplTest {

    private CourtScheduleRepositoryImpl courtScheduleRepository;

    @BeforeEach
    void setUp() {
        courtScheduleRepository = new CourtScheduleRepositoryImpl();
    }

    @Test
    void getCourtScheduleByCaseId_shouldReturnNewResponseIfNotExists() {
        String caseUrn = "test-case-urn";

        CourtScheduleResponse response = courtScheduleRepository.getCourtScheduleByCaseId(caseUrn);

        assertNotNull(response);
        assertEquals(1, response.getCourtSchedule().size());
    }

    @Test
    void getCourtScheduleByCaseId_shouldReturnExistingResponseIfExists() {
        String caseUrn = "test-case-urn";
        CourtScheduleResponse expectedResponse = CourtScheduleResponse.builder().build();

        courtScheduleRepository.saveCourtSchedule(caseUrn, expectedResponse);
        CourtScheduleResponse actualResponse = courtScheduleRepository.getCourtScheduleByCaseId(caseUrn);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void saveCourtSchedule_shouldStoreResponse() {
        String caseUrn = "test-case-urn";
        CourtScheduleResponse response = CourtScheduleResponse.builder().build();

        courtScheduleRepository.saveCourtSchedule(caseUrn, response);
        CourtScheduleResponse storedResponse = courtScheduleRepository.getCourtScheduleByCaseId(caseUrn);

        assertNotNull(storedResponse);
        assertEquals(response, storedResponse);
    }
}