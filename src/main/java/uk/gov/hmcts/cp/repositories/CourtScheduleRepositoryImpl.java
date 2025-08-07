package uk.gov.hmcts.cp.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class CourtScheduleRepositoryImpl implements CourtScheduleRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CourtScheduleRepositoryImpl.class);

    private final Map<String, CourtScheduleResponse> courtScheduleResponseMap = new ConcurrentHashMap<>();


    public void saveCourtSchedule(final String caseUrn, final CourtScheduleResponse courtScheduleResponse) {
        courtScheduleResponseMap.put(caseUrn, courtScheduleResponse);
    }

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseUrn) {
        if (!courtScheduleResponseMap.containsKey(caseUrn)) {
            saveCourtSchedule(caseUrn, createCourtScheduleResponse());
        }
        return courtScheduleResponseMap.get(caseUrn);
    }

    public void clearAll() {
        courtScheduleResponseMap.clear();
    }

    private CourtScheduleResponse createCourtScheduleResponse() {

        String res = getHearingData();
        LOG.info("infunction createCourtScheduleResponse Response Body: {} " + res);


        final OffsetDateTime sittingStartTime = OffsetDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS);

        final Hearing hearing = Hearing.builder()
                .hearingId(UUID.randomUUID().toString())
                .listNote("Requires interpreter")
                .hearingDescription("Sentencing for theft case")
                .hearingType("Trial")
                .courtSittings(List.of(
                        CourtSitting.builder()
                                .courtHouse("Central Criminal Court")
                                .sittingStart(sittingStartTime)
                                .sittingEnd(sittingStartTime.plusMinutes(60))
                                .judiciaryId(UUID.randomUUID().toString())
                                .build())
                ).build();

        return CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                                CourtSchedule.builder()
                                        .hearings(List.of(hearing)
                                        ).build()
                        )
                ).build();
    }

    private String getHearingData(){
        HttpResponse<String> response = null;
        try {
            // Create HttpClient instance
            HttpClient client = HttpClient.newHttpClient();

            // Build the request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://steccm64.ingress01.dev.nl.cjscp.org.uk/listing-query-api/query/api/rest/listing/hearings/allocated-and-unallocated?caseId=f552dee6-f092-415b-839c-5e5b5f46635e"))
                    .GET()
                    .header("Accept", "application/vnd.listing.search.hearings+json")
                    .header("CJSCPPUID", "d7c91866-646a-462c-9203-46678e8cddef")
                    .build();

            // Send the request
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() != HttpStatus.OK.value()) {
                LOG.error("Failed to fetch hearing data. HTTP Status: {}", response.statusCode());
                return null;
            }

            // Print response status and body
            LOG.info("Response Code: {} " + response.statusCode());
            LOG.info("Response Body: {} " + response.body());
            return response.body();
        } catch (Exception e) {
            LOG.error("Exception occurred while fetching hearing data: {}", e.getMessage(), e);
        }
        return null;
    }
}
