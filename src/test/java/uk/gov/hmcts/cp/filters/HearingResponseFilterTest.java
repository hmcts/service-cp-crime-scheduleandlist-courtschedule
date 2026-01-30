package uk.gov.hmcts.cp.filters;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.HearingResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HearingResponseFilterTest {

    private final HearingResponseFilter hearingResponseFilter = new HearingResponseFilter();

    private final String hearingId = "8d20aff7-3275-447c-8bcc-72db68944da3";

    @Test
    void filter_allocated_hearings_should_return_hearing() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(dummyHearing(true).build())).build();

        HearingResponse response = hearingResponseFilter.filterHearingResponse(hearingResponse);

        List<HearingResponse.HearingSchedule> hearings = response.getHearings();

        HearingResponse.HearingSchedule hearing = hearings.getFirst();
        assertThat(hearing.getId()).isEqualTo(hearingId);
        assertThat(hearing.isAllocated()).isEqualTo(true);
    }

    @Test
    void filter_week_commencing_hearings_should_return_hearing() {
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(weekCommencingHearing())).build();

        HearingResponse response = hearingResponseFilter.filterHearingResponse(hearingResponse);

        List<HearingResponse.HearingSchedule> hearings = response.getHearings();

        HearingResponse.HearingSchedule hearing = hearings.getFirst();
        assertThat(hearing.getId()).isEqualTo(hearingId);
        assertThat(hearing.isAllocated()).isEqualTo(false);
        assertThat(hearing.getWeekCommencingDurationInWeeks()).isEqualTo(1);
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
                .build();
        return HearingResponse.HearingSchedule.builder()
                .id(hearingId)
                .allocated(isAllocated)
                .hearingDays(List.of(hearingDay));
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