package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;
import uk.gov.hmcts.cp.httpclients.CourtScheduleClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtScheduleServiceTest {

    @Mock
    private CourtScheduleClient courtScheduleClient;

    @InjectMocks
    private CourtScheduleService courtScheduleService;

    @Test
    void do_getCourtScheduleByCaseId_should_returnStubbedResponseWhenValidCaseUrnProvided() {
        String validCaseUrn = "123-ABC-456";
        Instant sittingStartTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        CourtScheduleResponse mockResponse = CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                        CourtSchedule.builder()
                                .hearings(List.of(
                                        Hearing.builder()
                                                .hearingId(UUID.randomUUID().toString())
                                                .hearingDescription("Sentencing for theft case")
                                                .courtSittings(List.of(
                                                        CourtSitting.builder()
                                                                .sittingStart(sittingStartTime)
                                                                .sittingEnd(sittingStartTime.plus(60, ChronoUnit.MINUTES))
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .build()
                ))
                .build();

        when(courtScheduleClient.getCourtScheduleByCaseId(validCaseUrn)).thenReturn(mockResponse);

        CourtScheduleResponse response = courtScheduleService.getCourtScheduleByCaseId(validCaseUrn);

        assertThat(response).isNotNull();
        assertThat(response.getCourtSchedule()).isNotEmpty();
        assertThat(response.getCourtSchedule().get(0).getHearings()).isNotEmpty();
        assertThat(response.getCourtSchedule().get(0).getHearings().get(0).getCourtSittings()).isNotEmpty();
        assertThat(response.getCourtSchedule().get(0).getHearings().get(0).getHearingDescription())
                .isEqualTo("Sentencing for theft case");
    }

    @Test
    void do_getCourtScheduleByCaseId_should_throwBadRequestExceptionWhenCaseUrnIsNull() {
        String nullCaseUrn = null;

        assertThatThrownBy(() -> courtScheduleService.getCourtScheduleByCaseId(nullCaseUrn))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("caseId is required");
    }

    @Test
    void do_getCourtScheduleByCaseId_should_throwBadRequestExceptionWhenCaseUrnIsEmpty() {
        String emptyCaseUrn = "";

        assertThatThrownBy(() -> courtScheduleService.getCourtScheduleByCaseId(emptyCaseUrn))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("caseId is required");
    }
}