package uk.gov.hmcts.cp.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingResponse.HearingSchedule.Judiciary;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.cp.utils.Utils.getHttpClient;

@Component
@Primary
@RequiredArgsConstructor
@Profile("!pact-test")
public class CourtScheduleClientImpl implements CourtScheduleClient {
    private static final Logger LOG = LoggerFactory.getLogger(CourtScheduleClientImpl.class);
    private final HttpClient httpClient;

    @Getter
    @Value("${service.court-schedule-client.url}")
    private String courtScheduleClientUrl;

    @Getter
    @Value("${service.court-schedule-client.path}")
    private String courtScheduleClientPath;

    @Getter
    @Value("${service.court-schedule-client.cjscppuid}")
    private String cjscppuid;

    public CourtScheduleClientImpl() throws NoSuchAlgorithmException, KeyManagementException {
        this.httpClient = getHttpClient();
    }

    @Override
    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) {
        final List<Hearing> hearingList = getHearings(caseId);
        return CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                                CourtSchedule.builder()
                                        .hearings(hearingList)
                                        .build()
                        )
                ).build();
    }

    private List<Hearing> getHearings(final String caseId) {
        List<Hearing> hearingSchedule = Collections.emptyList();
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(buildUrl(caseId)))
                    .GET()
                    .header("Accept", "application/vnd.listing.search.hearings+json")
                    .header("CJSCPPUID", getCjscppuid())
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HttpStatus.OK.value()) {
                final ObjectMapper objectMapper = new ObjectMapper();
                final HearingResponse hearingResponse = objectMapper.readValue(
                        response.body(),
                        HearingResponse.class
                );

                hearingSchedule = getHearingData(hearingResponse);
                LOG.atInfo().log("Response Code: {}", response.statusCode());
            } else {
                LOG.atError().log("Failed to fetch hearing data. HTTP Status: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            LOG.atError().log("Exception occurred while fetching hearing data: {}", e.getMessage(), e);
        }
        return hearingSchedule;
    }


    private String buildUrl(final String caseId) {
        return UriComponentsBuilder
                .fromUri(URI.create(getCourtScheduleClientUrl()))
                .path(getCourtScheduleClientPath())
                .queryParam("caseId", caseId)
                .toUriString();
    }

    private List<Hearing> getHearingData(final HearingResponse hearingResponse)  {
        final List<Hearing> hearings = new ArrayList<>();
        hearingResponse.getHearings().stream().filter(HearingResponse.HearingSchedule::isAllocated).forEach(hr -> {
            final Hearing hearing = new Hearing();
            hearing.setHearingId(hr.getId());
            hearing.setHearingType(hr.getType().getDescription());
            hearing.setHearingDescription(hr.getType().getDescription());
            hearing.setListNote("sample list note");
            final List<CourtSitting> courtSittings = new ArrayList<>();
            final String judiciaryId = hr.getJudiciary().stream().map(Judiciary::getJudicialId).collect(Collectors.joining(","));

            for (final HearingResponse.HearingSchedule.HearingDay hearingDay : hr.getHearingDays()) {
                final CourtSitting courtSitting = getCourtSitting(hearingDay, judiciaryId);
                courtSittings.add(courtSitting);
            }
            hearing.setCourtSittings(courtSittings);
            hearings.add(hearing);
        });
        return hearings;
    }

    private static CourtSitting getCourtSitting(final HearingResponse.HearingSchedule.HearingDay hearingDay, final String judiciaryId) {
        final CourtSitting courtSitting = new CourtSitting();
        courtSitting.setSittingStart(OffsetDateTime.parse(hearingDay.getStartTime()));
        courtSitting.setSittingEnd(OffsetDateTime.parse(hearingDay.getEndTime()));
        courtSitting.setJudiciaryId(judiciaryId);

        courtSitting.setCourtHouse(hearingDay.getCourtCentreId());
        courtSitting.setCourtRoom(hearingDay.getCourtRoomId());
        return courtSitting;
    }
}
