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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CourtScheduleControllerIntegrationTest extends IntegrationTestBase {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.findAndRegisterModules();
    }
    @Autowired
    AppPropertiesBackend appProperties;
    @MockitoBean
    RestTemplate restTemplate;

    String caseUrn = "test-case-urn";
    String caseId = UUID.randomUUID().toString();

    @Test
    void get_court_schedule_should_return_ok() throws Exception {
        CaseMapperResponse caseMapperResponse = CaseMapperResponse.builder().caseId(caseId).build();
        mockMapperResponse(caseUrn, HttpStatus.OK, caseMapperResponse);

        mockHearingsResponse(caseId, HttpStatus.OK, cp_allocated_and_unallocated_response());

        CourtScheduleResponse expectedResponse = expected_amp_response();

        MvcResult result = mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        CourtScheduleResponse actualResponse = MAPPER.readValue(result.getResponse().getContentAsString(), CourtScheduleResponse.class);
        assertThat(actualResponse).isEqualTo(expectedResponse);
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

    private HearingResponse cp_allocated_and_unallocated_response() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("cp_allocated_and_unallocated_response.json");
        String json = Files.readString(Path.of(resource.toURI()));
        return MAPPER.readValue(json, HearingResponse.class);
    }

    private CourtScheduleResponse expected_amp_response() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("expected_amp_response.json");
        String json = Files.readString(Path.of(resource.toURI()));
        return MAPPER.readValue(json, CourtScheduleResponse.class);
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
        //             final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpEntity expectedHearingsHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.listing.search.hearings+json");
        headers.set("CJSCPPUID", appProperties.getHearingsCjscppuid());
        return new HttpEntity<>(headers);
    }
}