package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.clients.CourtScheduleClient;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.filters.HearingResponseFilter;
import uk.gov.hmcts.cp.mappers.HearingsMapper;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourtScheduleServiceTest {

    @Mock
    private CourtScheduleClient courtScheduleClient;

    @Mock
    private HearingsMapper hearingsMapper;

    @Mock
    private HearingResponseFilter hearingResponseFilter;

    @InjectMocks
    private CourtScheduleService courtScheduleService;

    @Test
    void shouldReturnStubbedCourtScheduleResponse_whenValidCaseUrnProvided() {
        String validCaseId= "123-ABC-456";
        HearingResponse hearingResponse = mockNotEmptyHearingResponse();
        CourtScheduleResponse courtScheduleResponse = mock(CourtScheduleResponse.class);

        when(courtScheduleClient.getHearingResponse(validCaseId)).thenReturn(hearingResponse);
        when(hearingResponseFilter.filterHearingResponse(hearingResponse)).thenReturn(hearingResponse);
        when(hearingsMapper.mapCommonPlatformResponse(hearingResponse)).thenReturn(courtScheduleResponse);

        CourtScheduleResponse response = courtScheduleService.getCourtScheduleByCaseId(validCaseId);
        assertThat(response).isEqualTo(courtScheduleResponse);

        verify(courtScheduleClient).getHearingResponse(validCaseId);
        verify(hearingResponseFilter).filterHearingResponse(hearingResponse);
        verify(hearingsMapper).mapCommonPlatformResponse(hearingResponse);
    }

    @Test
    void shouldThrowBadRequestException_whenCaseUrnIsNull() {
        String nullCaseUrn = null;

        assertThatThrownBy(() -> courtScheduleService.getCourtScheduleByCaseId(nullCaseUrn))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("caseId is mandatory");
    }

    @Test
    void shouldThrowBadRequestException_whenCaseUrnIsEmpty() {
        String emptyCaseUrn = "";

        assertThatThrownBy(() -> courtScheduleService.getCourtScheduleByCaseId(emptyCaseUrn))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("caseId is mandatory");
    }

    @Test
    void shouldThrowBNotFoundException_whenHearingResponseIsEmpty() {
        String validCaseId= "123-ABC-456";
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearings(Collections.emptyList())
                .build();

        when(courtScheduleClient.getHearingResponse(validCaseId)).thenReturn(hearingResponse);

        assertThatThrownBy(() -> courtScheduleService.getCourtScheduleByCaseId(validCaseId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("hearing response should not be empty");
        verifyNoInteractions(hearingResponseFilter, hearingsMapper);
    }

    private HearingResponse mockNotEmptyHearingResponse() {
        HearingResponse.HearingSchedule hearing = mock(HearingResponse.HearingSchedule.class);
        return HearingResponse.builder()
                .hearings(List.of(hearing))
                .build();
    }
}