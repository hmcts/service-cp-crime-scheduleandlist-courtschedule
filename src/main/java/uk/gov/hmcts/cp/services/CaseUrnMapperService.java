package uk.gov.hmcts.cp.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static uk.gov.hmcts.cp.utils.Utils.ignoreCertificates;

import static uk.gov.hmcts.cp.utils.Utils.ignoreCertificates;

@Service
@RequiredArgsConstructor
public class CaseUrnMapperService {
    private static final Logger LOG = LoggerFactory.getLogger(CaseUrnMapperService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${service.case-mapper-service.url}")
    private String caseMapperServiceUrl;

    public String getCaseId(final String caseUrn) {
        try {
            ignoreCertificates();
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    getCaseIdUrl(caseUrn),
                    HttpMethod.GET,
                    getRequestEntity(),
                    String.class
            );
            return getCaseId(responseEntity);
        } catch (Exception e) {
            LOG.atError().log("Error while getting case id from case urn", e);
        }
        return null;
    }

    public String getCaseMapperServiceUrl() {
        return this.caseMapperServiceUrl;
    }

    public String getCaseId(ResponseEntity<String> responseEntity) throws JsonProcessingException {
        if (responseEntity != null || responseEntity.hasBody()) {
            JsonNode root = objectMapper.readTree(responseEntity.getBody());
            return root.get("caseId").asText();
        }
        return Strings.EMPTY;
    }

    private String getCaseIdUrl(String caseUrn) {
        LOG.atDebug().log("Fetching case id for case urn: {}", caseUrn);
        //return URI.create(getCaseMapperServiceUrl() + "/" + caseUrn).toString();
        return UriComponentsBuilder
                .fromUri(URI.create(getCaseMapperServiceUrl()))
                .pathSegment(caseUrn)
                .buildAndExpand(caseUrn)
                .toUriString();
    }

    private HttpEntity<String> getRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        //headers.set("Accept", "application/json, application/*+json");
        return new HttpEntity<>(headers);
    }
}
