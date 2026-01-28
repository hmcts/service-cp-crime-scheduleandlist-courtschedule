package uk.gov.hmcts.cp.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HearingsMapperTest {

    @InjectMocks
    HearingsMapper mapper = new HearingsMapper();

    private String hearingId = "8d20aff7-3275-447c-8bcc-72db68944da3";
    private String courtId = "6c7fd04c-0dae-4c96-aaff-bc60f4e0d431";
    private String courtRoomId = "1ef27392-ee5f-4b81-b783-a80b08037cbc";

    @Test
    void cp_hearings_should_map_to_amp_response() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(dummyHearing())).build();

        CourtScheduleResponse response = mapper.mapCommonPlatformResponse(hearingResponse);

        assertThat(response.getCourtSchedule()).hasSize(1);
        Hearing hearing0 = response.getCourtSchedule().getFirst().getHearings().getFirst();
        assertThat(hearing0.getHearingId()).isEqualTo(hearingId);
        // assert EVERY single field !

        assertThat(hearing0.getListNote()).isEqualTo("ListNote");
    }

    private HearingResponse.HearingSchedule dummyHearing() {
        HearingResponse.HearingSchedule.HearingDay hearingDay = HearingResponse.HearingSchedule.HearingDay.builder()
                .startTime("2026-01-21T11:00:00Z")
                .endTime("2026-01-21T11:20:00Z")
                .courtCentreId(courtId)
                .courtRoomId(courtRoomId)
                .build();
        return HearingResponse.HearingSchedule.builder()
                .id(hearingId)
                .allocated(true)
                .type(HearingResponse.HearingSchedule.HearingType.builder().description("Plea and Trial Preparation").build())
                .judiciary(List.of())
                .hearingDays(List.of(hearingDay))
                .publicListNote("ListNote")
                .build();
    }
}