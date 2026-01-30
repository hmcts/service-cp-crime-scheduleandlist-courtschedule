package uk.gov.hmcts.cp.mappers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

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
                    // PublicListNote is an optional field
                    hearing.setListNote(Optional.ofNullable(hr.getPublicListNote())
                            .orElse(""));

                    final List<CourtSitting> courtSittings = getCourtSittingsForAllocatedHearing(hr);
                    if(Objects.nonNull(hr.getWeekCommencingDurationInWeeks())) {
                        //ToDo after api version 1.0.6 is build
                       // hearing.setWeekCommencingStartDate(hr.getWeekCommencingStartDate());
                       // hearing.setWeekCommencingEndDate(hr.getWeekCommencingEndDate());
                       // hearing.setWeekCommencingDurationInWeeks(hr.getWeekCommencingDurationInWeeks());
                    }

                    hearing.setCourtSittings(courtSittings);
                    hearings.add(hearing);
                });

        return hearings;
    }

    private List<CourtSitting> getCourtSittingsForAllocatedHearing(HearingResponse.HearingSchedule hr) {
        List<CourtSitting> courtSittings = new ArrayList<>();
        if(hr.isAllocated()) {
            final String judiciaryId = hr.getJudiciary().stream()
                    .map(HearingResponse.HearingSchedule.Judiciary::getJudicialId)
                    .collect(Collectors.joining(","));

            for (final HearingResponse.HearingSchedule.HearingDay hearingDay : hr.getHearingDays()) {
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
