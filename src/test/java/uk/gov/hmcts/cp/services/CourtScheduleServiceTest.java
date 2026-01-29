package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.clients.CourtScheduleClient;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.mappers.HearingsMapper;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtScheduleServiceTest {

    @Mock
    private CourtScheduleClient courtScheduleClient;

    @Mock
    private HearingsMapper hearingsMapper;

    @InjectMocks
    private CourtScheduleService courtScheduleService;

    @Test
    void shouldReturnStubbedCourtScheduleResponse_whenValidCaseUrnProvided() {
        String validCaseId= "123-ABC-456";
        HearingResponse hearingResponse = mock(HearingResponse.class);
        CourtScheduleResponse courtScheduleResponse = mock(CourtScheduleResponse.class);

        when(courtScheduleClient.getHearingResponse(validCaseId)).thenReturn(hearingResponse);
        when(hearingsMapper.mapCommonPlatformResponse(hearingResponse)).thenReturn(courtScheduleResponse);

        CourtScheduleResponse response = courtScheduleService.getCourtScheduleByCaseId(validCaseId);
        assertThat(response).isEqualTo(courtScheduleResponse);

        verify(courtScheduleClient).getHearingResponse(validCaseId);
        verify(hearingsMapper).mapCommonPlatformResponse(hearingResponse);
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