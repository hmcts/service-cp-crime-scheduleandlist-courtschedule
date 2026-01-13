package uk.gov.hmcts.cp.services;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;


@Service
@RequiredArgsConstructor
@Slf4j
public class CaseUrnMapperService {
    private final RestTemplate restTemplate;

    @Value("${service.case-mapper-service.url}")
    @Getter
    private String caseMapperServiceUrl;

    @Value("${service.case-mapper-service.path}")
    @Getter
    private String caseMapperServicePath;


    public String getCaseId(final String caseUrn) {
        final String sanitizedCaseUrn = Encode.forJava(caseUrn);
        final String url = getCaseIdUrl(caseUrn);
        log.info("Getting caseId from {}", url);
        try {
            final ResponseEntity<CaseMapperResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    getRequestEntity(),
                    CaseMapperResponse.class
            );
            log.debug(" CaseMapperResponse is : {} and body : {} caseurn : {} ",
                    responseEntity.getStatusCode(),
                    Encode.forJava(responseEntity.getBody().toString()),
                    sanitizedCaseUrn);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                final CaseMapperResponse body = responseEntity.getBody();
                return body.getCaseId();
            }
        } catch (RestClientException e) {
            log.error("Error while getting case id from case urn: {}", sanitizedCaseUrn, e);
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found by urn: " + caseUrn);
    }

    private String getCaseIdUrl(final String caseUrn) {
        return UriComponentsBuilder
                .fromUriString(getCaseMapperServiceUrl())
                .pathSegment(getCaseMapperServicePath())
                .pathSegment(caseUrn)
                .buildAndExpand(caseUrn)
                .toUriString();
    }

    @NonNull
    private HttpEntity<String> getRequestEntity() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(headers);
    }
}
