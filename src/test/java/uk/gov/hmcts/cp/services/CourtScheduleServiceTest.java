package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.repositories.CourtScheduleClient;
import uk.gov.hmcts.cp.repositories.InMemoryCourtScheduleClientImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourtScheduleServiceTest {

    private final CourtScheduleClient courtScheduleClient = new InMemoryCourtScheduleClientImpl();
    private final CourtScheduleService courtScheduleService = new CourtScheduleService(courtScheduleClient);

    @Test
    void shouldReturnStubbedCourtScheduleResponse_whenValidCaseUrnProvided() {
        String validCaseUrn = "123-ABC-456";

        CourtScheduleResponse response = courtScheduleService.getCourtScheduleByCaseId(validCaseUrn);

        assertThat(response).isNotNull();
        assertThat(response.getCourtSchedule()).isNotEmpty();
        assertThat(response.getCourtSchedule().get(0).getHearings()).isNotEmpty();
        assertThat(response.getCourtSchedule().get(0).getHearings().get(0).getCourtSittings()).isNotEmpty();
        assertThat(response.getCourtSchedule().get(0).getHearings().get(0).getHearingDescription())
                .isEqualTo("Sentencing for theft case");
    }

    @Test
    void shouldThrowBadRequestException_whenCaseUrnIsNull() {
        String nullCaseUrn = null;

        assertThatThrownBy(() -> courtScheduleService.getCourtScheduleByCaseId(nullCaseUrn))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("caseId is required");
    }

    @Test
    void shouldThrowBadRequestException_whenCaseUrnIsEmpty() {
        String emptyCaseUrn = "";

        assertThatThrownBy(() -> courtScheduleService.getCourtScheduleByCaseId(emptyCaseUrn))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("caseId is required");
    }
}