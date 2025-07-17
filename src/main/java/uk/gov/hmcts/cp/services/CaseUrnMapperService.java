package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CaseUrnMapperService {

    private static final Logger LOG = LoggerFactory.getLogger(CaseUrnMapperService.class);

    private final RestTemplate restTemplate;

    @Value("${service.case-mapper-service.url}")
    private String caseMapperServiceUrl;

    private static final String CASEURN_ID = "caseurn/{caseurn}";

    private final Map<String, String> caseUrnToCaseIdMap = new ConcurrentHashMap<>();



    public String getCaseId(final String caseUrn) {
        return UUID.randomUUID().toString();
        // This below code is commented out as it is will be used when the case mapper service is available.

/*        try {
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
        return null;*/
    }

    private String getCaseIdUrl(String caseUrn) {
        return UriComponentsBuilder
                .fromUri(URI.create(caseMapperServiceUrl))
                .pathSegment(caseUrn)
                .buildAndExpand(caseUrn)
                .toUriString();
    }

    private HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }
}
