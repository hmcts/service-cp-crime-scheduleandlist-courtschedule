package uk.gov.hmcts.cp.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HearingsMapperTest {

    HearingsMapper mapper = new HearingsMapper();

    private final String hearingId = "8d20aff7-3275-447c-8bcc-72db68944da3";
    private final String courtId = "6c7fd04c-0dae-4c96-aaff-bc60f4e0d431";
    private final String courtRoomId = "1ef27392-ee5f-4b81-b783-a80b08037cbc";

    @Test
    void cp_allocated_hearings_should_map_to_amp_response() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(dummyHearing())).build();

        CourtScheduleResponse response = mapper.mapCommonPlatformResponse(hearingResponse);

        Hearing hearing0 = response.getCourtSchedule().getFirst().getHearings().getFirst();
        assertThat(hearing0.getHearingId()).isEqualTo(hearingId);
        assertThat(hearing0.getHearingType()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing0.getHearingDescription()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing0.getListNote()).isEqualTo("ListNote");

        CourtSitting courtSitting0 = hearing0.getCourtSittings().getFirst();
        assertThat(courtSitting0.getSittingStart()).isEqualTo(Instant.parse("2026-01-21T11:00:00Z"));
        assertThat(courtSitting0.getJudiciaryId()).isEqualTo("judge-1,judge-2");
        assertThat(courtSitting0.getCourtHouse()).isEqualTo(courtId);
        assertThat(courtSitting0.getCourtRoom()).isEqualTo(courtRoomId);
    }

    private HearingResponse.HearingSchedule dummyHearing() {
        HearingResponse.HearingSchedule.HearingDay hearingDay = HearingResponse.HearingSchedule.HearingDay.builder()
                .startTime("2026-01-21T11:00:00Z")
                .endTime("2026-01-21T11:20:00Z")
                .courtCentreId(courtId)
                .courtRoomId(courtRoomId)
                .build();
        HearingResponse.HearingSchedule.Judiciary judiciary1 = HearingResponse.HearingSchedule.Judiciary.builder().judicialId("judge-1").build();
        HearingResponse.HearingSchedule.Judiciary judiciary2 = HearingResponse.HearingSchedule.Judiciary.builder().judicialId("judge-2").build();
        return HearingResponse.HearingSchedule.builder()
                .id(hearingId)
                .allocated(true)
                .type(HearingResponse.HearingSchedule.HearingType.builder().description("Plea and Trial Preparation").build())
                .judiciary(List.of(judiciary1, judiciary2))
                .hearingDays(List.of(hearingDay))
                .publicListNote("ListNote")
                .build();
    }
}