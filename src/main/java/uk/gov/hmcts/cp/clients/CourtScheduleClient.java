package uk.gov.hmcts.cp.clients;

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

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class CourtScheduleClient {

    private final AppPropertiesBackend appProperties;
    private final RestTemplate restTemplate;

    public HearingResponse getHearingResponse(final String caseId) {
        final String url = buildUrl(caseId);
        log.info("Getting hearings from {}", Encode.forJava(url));
        final ResponseEntity<HearingResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequestEntity(),
                HearingResponse.class
        );
        return response.getBody();
    }

    private String buildUrl(final String caseId) {
        return String.format("%s%s?caseId=%s", appProperties.getHearingsUrl(), appProperties.getHearingsPath(), caseId);
    }

    private HttpEntity<String> getRequestEntity() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/vnd.listing.search.hearings+json");
        headers.add("CJSCPPUID", appProperties.getHearingsCjscppuid());
        return new HttpEntity<>(headers);
    }
}
