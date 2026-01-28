package uk.gov.hmcts.cp.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseUrnMapperService {
    private final RestTemplate restTemplate;
    private final AppPropertiesBackend appProperties;

    public String getCaseId(final String caseUrn) {
        final String sanitizedCaseUrn = Encode.forJava(caseUrn);
        final String url = getCaseIdUrl(caseUrn);
        log.info("Getting caseId from {}", url);
        final ResponseEntity<CaseMapperResponse> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequestEntity(),
                CaseMapperResponse.class
        );
        log.info("CaseMapperResponse is {} for caseurn {}", responseEntity.getStatusCode(), sanitizedCaseUrn);
        return responseEntity.getBody().getCaseId();
    }

    private String getCaseIdUrl(final String caseUrn) {
        return String.format("%s%s/%s", appProperties.getCaseMapperUrl(), appProperties.getCaseMapperPath(), caseUrn);
    }

    @NonNull
    private HttpEntity<String> getRequestEntity() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(headers);
    }
}
