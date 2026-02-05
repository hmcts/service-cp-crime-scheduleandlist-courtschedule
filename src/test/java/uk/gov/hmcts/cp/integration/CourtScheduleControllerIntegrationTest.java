package uk.gov.hmcts.cp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.domain.HearingResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CourtScheduleControllerIntegrationTest extends IntegrationTestBase {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    AppPropertiesBackend appProperties;
    @MockitoBean
    RestTemplate restTemplate;

    String caseUrn = "test-case-urn";
    String caseId = UUID.randomUUID().toString();

    @Test
    void get_court_schedule_for_allocated_and_weekcommencingunallocated_response_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_weekcommencingunallocated_response.json";
        String expected_amp_response = "expected_amp_allocated_and_weekcommencingunallocated_response.json";

        verifyResponse(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_response.json";
        String expected_amp_response = "expected_amp_allocated_response.json";

        verifyResponse(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_and_unallocated_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_unallocated_response.json";
        String expected_amp_response = "expected_amp_allocated_response.json";

        verifyResponse(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_and_weekcommencing_and_unallocated_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_weekcommencing_and_unallocated_response.json";
        String expected_amp_response = "expected_amp_allocated_and_weekcommencingunallocated_response.json";

        verifyResponse(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_and_unscheduled_response_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_unscheduled_response.json";
        String expected_amp_response = "expected_amp_allocated_response.json";

        verifyResponse(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_all_types_response_should_return_ok() throws Exception {
        String cp_response = "cp_all_types_response.json";
        String expected_amp_response = "expected_amp_allocated_and_weekcommencingunallocated_response.json";

        verifyResponse(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_unallocated_response_should_return_ok() throws Exception {
        String cp_response = "cp_unallocated_response.json";
        String expected_amp_response = "empty_hearing_response.json";

        verifyResponse(cp_response, expected_amp_response);
    }

    @Test
    void bad_caseurn_should_return_404() throws Exception {
        String expectedUrl = String.format("%s%s/%s", appProperties.getCaseMapperUrl(), appProperties.getCaseMapperPath(), caseUrn);
        log.info("Mocking {} error", expectedUrl);
        when(restTemplate.exchange(
                expectedUrl,
                HttpMethod.GET,
                expectedMapperHeaders(),
                CaseMapperResponse.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("message").value("404 NOT_FOUND"));
    }

    @Test
    void bad_hearings_response_should_return_500() throws Exception {
        CaseMapperResponse caseMapperResponse = CaseMapperResponse.builder().caseId(caseId).build();
        mockMapperResponse(caseUrn, HttpStatus.OK, caseMapperResponse);

        String expectedHearingsUrl = String.format("%s%s?caseId=%s", appProperties.getHearingsUrl(), appProperties.getHearingsPath(), caseId);
        log.info("Mocking {} error", expectedHearingsUrl);
        when(restTemplate.exchange(
                expectedHearingsUrl,
                HttpMethod.GET,
                expectedMapperHeaders(),
                CaseMapperResponse.class
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        String expectedParseError = "Cannot invoke \"org.springframework.http.ResponseEntity.getBody()\" because \"response\" is null";
        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is5xxServerError())
                // TODO COLING fix the error to surface and not try to parse / map the empty response
                .andExpect(jsonPath("message").value(expectedParseError));
    }

    private HearingResponse cp_response(final String resourceName) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourceName);
        String json = Files.readString(Path.of(resource.toURI()));
        return MAPPER.readValue(json, HearingResponse.class);
    }

    private String expected_amp_response(final String resourceName) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourceName);
        return Files.readString(Path.of(resource.toURI()));
    }

    private void verifyResponse(String cp_response, String expected_amp_response) throws Exception {
        CaseMapperResponse caseMapperResponse = CaseMapperResponse.builder().caseId(caseId).build();
        mockMapperResponse(caseUrn, HttpStatus.OK, caseMapperResponse);
        mockHearingsResponse(caseId, HttpStatus.OK, cp_response(cp_response));


        String expectedResponse = expected_amp_response(expected_amp_response);
        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    private void mockMapperResponse(String caseUrn, HttpStatus httpStatus, CaseMapperResponse response) {
        String expectedUrl = String.format("%s%s/%s", appProperties.getCaseMapperUrl(), appProperties.getCaseMapperPath(), caseUrn);
        log.info("Mocking {} response", expectedUrl);
        when(restTemplate.exchange(
                expectedUrl,
                HttpMethod.GET,
                expectedMapperHeaders(),
                CaseMapperResponse.class
        )).thenReturn(new ResponseEntity<>(response, httpStatus));
    }

    private HttpEntity expectedMapperHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return new HttpEntity<>(headers);
    }

    private void mockHearingsResponse(String caseId, HttpStatus httpStatus, HearingResponse response) {
        String expectedUrl = String.format("%s%s?caseId=%s", appProperties.getHearingsUrl(), appProperties.getHearingsPath(), caseId);
        log.info("Mocking {} response", expectedUrl);
        when(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.GET),
                eq(expectedHearingsHeaders()),
                eq(HearingResponse.class)
        )).thenReturn(new ResponseEntity<>(response, httpStatus));
    }

    private HttpEntity expectedHearingsHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.listing.search.hearings+json");
        headers.set("CJSCPPUID", appProperties.getHearingsCjscppuid());
        return new HttpEntity<>(headers);
    }
}