package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseUrnMapperService {

    private static final Logger LOG = LoggerFactory.getLogger(CaseUrnMapperService.class);

    private final RestTemplate restTemplate;

    @Value("${service.case-mapper-service.url}")
    private String caseMapperServiceUrl;

    private static final String CASEURN_ID = "caseurn/{caseurn}";

    public String getCaseId(final String caseUrn) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    getCaseIdUrl(caseUrn),
                    HttpMethod.GET,
                    getRequestEntity(),
                    String.class
            );
            return responseEntity.hasBody() ? responseEntity.getBody(): Strings.EMPTY;
        } catch (Exception e) {
            LOG.atError().log("Error while getting case id from case urn", e);
        }
        return null;
    }

    protected String getCaseIdUrl(String caseUrn) {
        return UriComponentsBuilder
                .fromUri(URI.create(caseMapperServiceUrl))
                .pathSegment(caseUrn)
                .buildAndExpand(caseUrn)
                .toUriString();
    }

    protected HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }
}
