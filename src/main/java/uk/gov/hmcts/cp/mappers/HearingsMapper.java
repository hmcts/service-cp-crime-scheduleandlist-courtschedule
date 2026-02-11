package uk.gov.hmcts.cp.mappers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.openapi.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class HearingsMapper {

    public CourtScheduleResponse mapCommonPlatformResponse(final HearingResponse hearingResponse) {
        final List<Hearing> hearingList = getHearingData(hearingResponse);
        return CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                        CourtSchedule.builder()
                                .hearings(hearingList)
                                .build()
                ))
                .build();
    }


    private List<Hearing> getHearingData(final HearingResponse hearingResponse) {
        final List<Hearing> hearings = new ArrayList<>();
        hearingResponse.getHearings()
                .forEach(hr -> {
                    final Hearing hearing = new Hearing();
                    hearing.setHearingId(hr.getId());
                    hearing.setHearingType(hr.getType().getDescription());
                    hearing.setHearingDescription(hr.getType().getDescription());
                    hearing.setListNote(Optional.ofNullable(hr.getPublicListNote())
                            .orElse(""));

                    final List<CourtSitting> courtSittings = getCourtSittingsForAllocatedHearing(hr);
                    updateForWeekCommencingHearing(hr, hearing);

                    hearing.setCourtSittings(courtSittings);
                    hearings.add(hearing);
                });

        return hearings;
    }

    private static void updateForWeekCommencingHearing(HearingResponse.HearingSchedule hr, Hearing hearing) {
        if(Objects.nonNull(hr.getWeekCommencingDurationInWeeks())) {
            HearingWeekCommencing hearingWeekCommencing = new HearingWeekCommencing();
            hearingWeekCommencing.setStartDate(hr.getWeekCommencingStartDate());
            hearingWeekCommencing.setEndDate(hr.getWeekCommencingEndDate());
            hearingWeekCommencing.setDurationInWeeks(hr.getWeekCommencingDurationInWeeks());
            hearingWeekCommencing.setCourtHouse(hr.getCourtCentreId());

            hearing.setWeekCommencing(hearingWeekCommencing);
        }
    }

    private List<CourtSitting> getCourtSittingsForAllocatedHearing(final HearingResponse.HearingSchedule hearingSchedule) {
        final List<CourtSitting> courtSittings = new ArrayList<>();
        if(hearingSchedule.isAllocated()) {
            final String judiciaryId = hearingSchedule.getJudiciary().stream()
                    .map(HearingResponse.HearingSchedule.Judiciary::getJudicialId)
                    .collect(Collectors.joining(","));

            for (final HearingResponse.HearingSchedule.HearingDay hearingDay : hearingSchedule.getHearingDays()) {
                courtSittings.add(getCourtSitting(hearingDay, judiciaryId));
            }
        }
        return courtSittings;
    }

    private CourtSitting getCourtSitting(
            final HearingResponse.HearingSchedule.HearingDay hearingDay,
            final String judiciaryId) {

        final CourtSitting courtSitting = new CourtSitting();
        courtSitting.setSittingStart(Instant.parse(hearingDay.getStartTime()));
        courtSitting.setSittingEnd(Instant.parse(hearingDay.getEndTime()));
        courtSitting.setJudiciaryId(judiciaryId);
        courtSitting.setCourtHouse(hearingDay.getCourtCentreId());
        courtSitting.setCourtRoom(hearingDay.getCourtRoomId());

        return courtSitting;
    }
}
