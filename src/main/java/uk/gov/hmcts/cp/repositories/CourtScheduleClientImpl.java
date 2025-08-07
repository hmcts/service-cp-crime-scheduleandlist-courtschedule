package uk.gov.hmcts.cp.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.cp.utils.Utils.getHttpClient;

@Component
@Primary
@RequiredArgsConstructor
public class CourtScheduleClientImpl implements CourtScheduleClient {
    private static final Logger LOG = LoggerFactory.getLogger(CourtScheduleClientImpl.class);
    private final HttpClient httpClient;

    @Value("${service.court-schedule-client.url}")
    private String courtScheduleClientUrl;

    @Value("${service.court-schedule-client.cjscppuid}")
    private String cjscppuid;

    public CourtScheduleClientImpl() throws NoSuchAlgorithmException, KeyManagementException {
        this.httpClient = getHttpClient();
    }

    public CourtScheduleClientImpl(HttpClient httpClient,
                                   @Value("${service.court-schedule-client.url}") String courtScheduleClientUrl,
                                   @Value("${service.court-schedule-client.cjscppuid}") String cjscppuid) {
        this.httpClient = httpClient;
        this.courtScheduleClientUrl = courtScheduleClientUrl;
        this.cjscppuid = cjscppuid;
    }

    public String getCourtScheduleClientUrl() {
        return this.courtScheduleClientUrl;
    }

    public String getCjscppuid() {
        return this.cjscppuid;
    }

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) {
        List<Hearing> hearingList = getHearings(caseId);
        LOG.info("infunction createCourtScheduleResponse Response Body: {} " + hearingList);

        return CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                                CourtSchedule.builder()
                                        .hearings(hearingList)
                                        .build()
                        )
                ).build();
    }

    private List<Hearing> getHearings(String caseId){
        HttpResponse<String> response = null;
        try {
            //ignoreCertificates();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(buildUrl(caseId)))
                    .GET()
                    .header("Accept", "application/vnd.listing.search.hearings+json")
                    .header("CJSCPPUID", getCjscppuid())
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != HttpStatus.OK.value()) {
                LOG.error("Failed to fetch hearing data. HTTP Status: {}", response.statusCode());
                return null;
            }

            List<Hearing> hearingResult = getHearingData(response.body());
            LOG.info("Response Code: {}, Response Body: {}", response.statusCode(), response.body());
            return hearingResult;
        } catch (Exception e) {
            LOG.error("Exception occurred while fetching hearing data: {}", e.getMessage(), e);
        }
        return null;
    }

    private String buildUrl(String caseId) {
        return UriComponentsBuilder
                .fromUri(URI.create(getCourtScheduleClientUrl()))
                .queryParam("caseId", caseId)
                .toUriString();
    }

    private List<Hearing> getHearingData(String body) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(body);

        List<Hearing> hearings = new ArrayList<>();

        for (JsonNode hearingNode : root.path("hearings")) {
            String hearingId = hearingNode.path("id").asText();
            JsonNode typeNode = hearingNode.path("type");
            String typeDescription = typeNode.path("description").asText();

            String judiciaryId = getJudiciaryIdString(hearingNode);
            Hearing hearing = new Hearing();
            hearing.setHearingId(hearingId);
            hearing.setHearingType(typeDescription);
            hearing.setHearingDescription(typeDescription);
            hearing.setListNote("sample list note");

            List<CourtSitting> courtSittings = getCourtSittings(hearingNode, judiciaryId);
            hearing.setCourtSittings(courtSittings);
            hearings.add(hearing);
        }
        return hearings ;
    }

    private static String getJudiciaryIdString(JsonNode hearingNode) {
        StringBuilder sb = new StringBuilder();
        hearingNode.path("judiciary").forEach(jd -> {
            sb.append(jd.path("judicialId").asText());
            sb.append(",");
        });
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : sb.toString();
    }

    private List<CourtSitting> getCourtSittings(JsonNode hearingNode, String judiciaryId) {
        List<CourtSitting> courtSittings = new ArrayList<>();
        for (JsonNode hearingDay : hearingNode.path("hearingDays")) {
            CourtSitting cs = new CourtSitting();
            cs.setSittingStart(OffsetDateTime.parse(hearingDay.path("startTime").asText()));
            cs.setSittingEnd(OffsetDateTime.parse(hearingDay.path("endTime").asText()));
            cs.setJudiciaryId(judiciaryId);

            cs.setCourtHouse(hearingDay.path("courtCentreId").asText());
            courtSittings.add(cs);
        }
        return courtSittings;
    }
}
