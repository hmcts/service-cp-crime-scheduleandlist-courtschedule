package uk.gov.hmcts.cp.filters;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.HearingResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HearingResponseFilterTest {

    private final HearingResponseFilter hearingResponseFilter = new HearingResponseFilter();

    private final String hearingId = "8d20aff7-3275-447c-8bcc-72db68944da3";
    private final String courtId = "6c7fd04c-0dae-4c96-aaff-bc60f4e0d431";
    private final String courtRoomId = "1ef27392-ee5f-4b81-b783-a80b08037cbc";

    @Test
    void filter_allocated_hearings_should_return_hearing() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(dummyHearing(true).build())).build();

        HearingResponse response = hearingResponseFilter.filterHearingResponse(hearingResponse);

        List<HearingResponse.HearingSchedule> hearings = response.getHearings();

        HearingResponse.HearingSchedule hearing = hearings.getFirst();
        assertThat(hearing.getId()).isEqualTo(hearingId);
        assertThat(hearing.getType().getDescription()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing.getPublicListNote()).isEqualTo("ListNote");
        assertThat(hearing.isAllocated()).isEqualTo(true);

        List<String> JudicialIdList = hearing.getJudiciary().stream()
                .map(HearingResponse.HearingSchedule.Judiciary::getJudicialId)
                .toList();
        assertThat(JudicialIdList).contains("judge-1", "judge-2");

        HearingResponse.HearingSchedule.HearingDay hearingDay = hearing.getHearingDays().getFirst();
        assertThat(hearingDay.getStartTime()).isEqualTo("2026-01-21T11:00:00Z");
        assertThat(hearingDay.getEndTime()).isEqualTo("2026-01-21T11:20:00Z");
        assertThat(hearingDay.getCourtCentreId()).isEqualTo(courtId);
        assertThat(hearingDay.getCourtRoomId()).isEqualTo(courtRoomId);
    }

    @Test
    void filter_week_commencing_hearings_should_return_hearing() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(weekCommencingHearing())).build();

        HearingResponse response = hearingResponseFilter.filterHearingResponse(hearingResponse);

        List<HearingResponse.HearingSchedule> hearings = response.getHearings();

        HearingResponse.HearingSchedule hearing = hearings.getFirst();
        assertThat(hearing.getId()).isEqualTo(hearingId);
        assertThat(hearing.getType().getDescription()).isEqualTo("Plea and Trial Preparation");
        assertThat(hearing.getPublicListNote()).isEqualTo("ListNote");
        assertThat(hearing.isAllocated()).isEqualTo(false);
        assertThat(hearing.getWeekCommencingDurationInWeeks()).isEqualTo(1);

        List<String> JudicialIdList = hearing.getJudiciary().stream()
                .map(HearingResponse.HearingSchedule.Judiciary::getJudicialId)
                .toList();
        assertThat(JudicialIdList).contains("judge-1", "judge-2");

        HearingResponse.HearingSchedule.HearingDay hearingDay = hearing.getHearingDays().getFirst();
        assertThat(hearingDay.getStartTime()).isEqualTo("2026-01-21T11:00:00Z");
        assertThat(hearingDay.getEndTime()).isEqualTo("2026-01-21T11:20:00Z");
        assertThat(hearingDay.getCourtCentreId()).isEqualTo(courtId);
        assertThat(hearingDay.getCourtRoomId()).isEqualTo(courtRoomId);
    }

    @Test
    void filter_unAllocated_hearings_should_return_empty_response() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(dummyHearing(false).build())).build();

        HearingResponse response = hearingResponseFilter.filterHearingResponse(hearingResponse);

        List<HearingResponse.HearingSchedule> hearings = response.getHearings();
        assertThat(hearings.size()).isEqualTo(0);
    }

    @Test
    void filter_unScheduled_hearings_should_return_empty_response() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(unscheduledHearing())).build();

        HearingResponse response = hearingResponseFilter.filterHearingResponse(hearingResponse);

        List<HearingResponse.HearingSchedule> hearings = response.getHearings();
        assertThat(hearings.size()).isEqualTo(0);
    }

    private HearingResponse.HearingSchedule.HearingScheduleBuilder dummyHearing(boolean isAllocated) {
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
                .hearingDays(List.of(hearingDay))
                .publicListNote("ListNote");
    }

    private HearingResponse.HearingSchedule unscheduledHearing() {
        return dummyHearing(false)
                .unscheduled(true)
                .build();
    }

    private HearingResponse.HearingSchedule weekCommencingHearing() {
        return dummyHearing(false)
                .weekCommencingDurationInWeeks(1)
                .build();
    }
}