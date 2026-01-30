package uk.gov.hmcts.cp.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HearingsMapperTest {

    HearingsMapper mapper = new HearingsMapper();

    private final String hearingId_allocated = "8d20aff7-3275-447c-8bcc-72db68944da3";
    private final String hearingId_unallocatedWeekCommencing = "9d20aff7-3275-447c-8bcc-72db68944da3";
    private final String courtId = "6c7fd04c-0dae-4c96-aaff-bc60f4e0d431";
    private final String courtRoomId = "1ef27392-ee5f-4b81-b783-a80b08037cbc";

    @Test
    void cp_hearings_should_map_to_amp_response() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(allocatedHearing(), unAllocatedWeekCommencingHearing())).build();

        CourtScheduleResponse response = mapper.mapCommonPlatformResponse(hearingResponse);

        CourtSchedule courtSchedule = response.getCourtSchedule().getFirst();
        assertForAllocatedHearing(getHearingFor(hearingId_allocated, courtSchedule).get());
        assertForUnallocatedWeekCommencingHearing(getHearingFor(hearingId_unallocatedWeekCommencing, courtSchedule).get());
    }

    private Optional<Hearing> getHearingFor(String id, CourtSchedule courtSchedule) {
        return courtSchedule.getHearings()
                .stream()
                .filter(hearing -> id.equals(hearing.getHearingId()))
                .findFirst();
    }

    private void assertForAllocatedHearing(Hearing hearing) {
        assertThat(hearing.getHearingType()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing.getHearingDescription()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing.getListNote()).isEqualTo("ListNote");

        CourtSitting courtSitting0 = hearing.getCourtSittings().getFirst();
        assertThat(courtSitting0.getSittingStart()).isEqualTo(Instant.parse("2026-01-21T11:00:00Z"));
        assertThat(courtSitting0.getJudiciaryId()).isEqualTo("judge-1,judge-2");
        assertThat(courtSitting0.getCourtHouse()).isEqualTo(courtId);
        assertThat(courtSitting0.getCourtRoom()).isEqualTo(courtRoomId);
    }

    private void assertForUnallocatedWeekCommencingHearing(Hearing hearing) {
        assertThat(hearing.getHearingType()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing.getHearingDescription()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing.getListNote()).isEqualTo("");

        assertThat(hearing.getCourtSittings().size()).isEqualTo(0);
        //ToDo after api version 1.0.6 is build
//        assertThat(hearing.getWeekCommencingStartDate()).isEqualTo("2026-02-16");
//        assertThat(hearing.getWeekCommencingEndDate()).isEqualTo("2026-02-22");
//        assertThat(hearing.weekCommencingDurationInWeeks()).isEqualTo(1);
    }

    private HearingResponse.HearingSchedule.HearingScheduleBuilder dummyHearing(String hearingId, boolean isAllocated) {
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
                .allocated(isAllocated)
                .type(HearingResponse.HearingSchedule.HearingType.builder().description("Plea and Trial Preparation").build())
                .judiciary(List.of(judiciary1, judiciary2))
                .hearingDays(List.of(hearingDay));
    }

    private HearingResponse.HearingSchedule allocatedHearing() {
        return dummyHearing(hearingId_allocated, true)
                .publicListNote("ListNote")
                .build();
    }

    private HearingResponse.HearingSchedule unAllocatedWeekCommencingHearing() {
        return dummyHearing(hearingId_unallocatedWeekCommencing, false)
                .weekCommencingDurationInWeeks(1)
                .weekCommencingStartDate("2026-02-16")
                .weekCommencingEndDate("2026-02-22")
                .build();
    }
}