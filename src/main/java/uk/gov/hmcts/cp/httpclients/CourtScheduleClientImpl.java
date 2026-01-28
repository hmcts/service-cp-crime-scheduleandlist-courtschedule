package uk.gov.hmcts.cp.httpclients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingResponse.HearingSchedule.Judiciary;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class CourtScheduleClientImpl {

    private final AppPropertiesBackend appProperties;
    private final RestTemplate restTemplate;

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) {
        final List<Hearing> hearingList = getHearings(caseId);
        return CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                        CourtSchedule.builder()
                                .hearings(hearingList)
                                .build()
                ))
                .build();
    }

    private List<Hearing> getHearings(final String caseId) {
        final String url = buildUrl(caseId);
        log.info("Getting hearings from {}", Encode.forJava(url));
        final ResponseEntity<HearingResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequestEntity(),
                HearingResponse.class
        );
        return getHearingData(response.getBody());
    }

    private String buildUrl(final String caseId) {
        return String.format("%s%s?caseId=%s", appProperties.getHearingsUrl(), appProperties.getHearingsPath(), caseId);
    }

    private List<Hearing> getHearingData(final HearingResponse hearingResponse) {
        final List<Hearing> hearings = new ArrayList<>();

        hearingResponse.getHearings().stream()
                .filter(hearing -> hearing.isAllocated() || Objects.nonNull(hearing.getWeekCommencingDurationInWeeks()))
                .forEach(hr -> {
                    final Hearing hearing = new Hearing();
                    hearing.setHearingId(hr.getId());
                    hearing.setHearingType(hr.getType().getDescription());
                    hearing.setHearingDescription(hr.getType().getDescription());
                    // TODO COLING weird !
                    hearing.setListNote("sample list note");

                    final String judiciaryId = hr.getJudiciary().stream()
                            .map(Judiciary::getJudicialId)
                            .collect(Collectors.joining(","));

                    final List<CourtSitting> courtSittings = new ArrayList<>();
                    for (final HearingResponse.HearingSchedule.HearingDay hearingDay : hr.getHearingDays()) {
                        courtSittings.add(getCourtSitting(hearingDay, judiciaryId));
                    }

                    hearing.setCourtSittings(courtSittings);
                    hearings.add(hearing);
                });

        return hearings;
    }

    private static CourtSitting getCourtSitting(
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

    private HttpEntity<String> getRequestEntity() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/vnd.listing.search.hearings+json");
        headers.add("CJSCPPUID", appProperties.getHearingsCjscppuid());
        return new HttpEntity<>(headers);
    }
}
