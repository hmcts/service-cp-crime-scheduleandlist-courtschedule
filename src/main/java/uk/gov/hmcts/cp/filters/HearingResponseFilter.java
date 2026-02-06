package uk.gov.hmcts.cp.filters;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.domain.HearingResponse;

import java.util.List;
import java.util.Objects;

@Component
public class HearingResponseFilter {

    public HearingResponse filterHearingResponse(final HearingResponse response) {
        final List<HearingResponse.HearingSchedule> hearings = response.getHearings()
                .stream()
                .filter(hearingSchedule -> hearingSchedule.isAllocated()
                        || Objects.nonNull(hearingSchedule.getWeekCommencingDurationInWeeks()))
                .toList();
        return new HearingResponse(hearings);
    }
}
