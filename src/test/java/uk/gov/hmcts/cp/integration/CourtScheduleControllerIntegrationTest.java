package uk.gov.hmcts.cp.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CourtScheduleControllerIntegrationTest extends IntegrationTestBase {

    String caseUrn = "ABCD1234567";
    String caseId = UUID.randomUUID().toString();

    protected WireMockServer wireMockServer;

    @BeforeEach
    void beforeEach() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8081));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8081);
    }

    @AfterEach
    void afterEach() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void get_court_schedule_for_allocated_and_weekcommencingunallocated_response_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_weekcommencingunallocated_response.json";
        String expected_amp_response = "expected_amp_allocated_and_weekcommencingunallocated_response.json";

        amp_endpoint_and_verify_response(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_response.json";
        String expected_amp_response = "expected_amp_allocated_response.json";

        amp_endpoint_and_verify_response(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_and_unallocated_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_unallocated_response.json";
        String expected_amp_response = "expected_amp_allocated_response.json";

        amp_endpoint_and_verify_response(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_and_weekcommencing_and_unallocated_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_weekcommencing_and_unallocated_response.json";
        String expected_amp_response = "expected_amp_allocated_and_weekcommencingunallocated_response.json";

        amp_endpoint_and_verify_response(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_allocated_and_unscheduled_response_should_return_ok() throws Exception {
        String cp_response = "cp_allocated_and_unscheduled_response.json";
        String expected_amp_response = "expected_amp_allocated_response.json";

        amp_endpoint_and_verify_response(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_all_types_response_should_return_ok() throws Exception {
        String cp_response = "cp_all_types_response.json";
        String expected_amp_response = "expected_amp_allocated_and_weekcommencingunallocated_response.json";

        amp_endpoint_and_verify_response(cp_response, expected_amp_response);
    }

    @Test
    void get_court_schedule_for_unallocated_response_should_return_ok() throws Exception {
        String cp_response = "cp_unallocated_response.json";
        String expected_amp_response = "empty_hearing_response.json";

        amp_endpoint_and_verify_response(cp_response, expected_amp_response);
    }

    @Test
    void bad_caseurn_should_return_404() throws Exception {
        String expectedUrl = String.format("%s/%s", appProperties.getCaseMapperPath(), caseUrn);
        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(HTTP_NOT_FOUND)
                .withHeader("Content-Type", "application/json");
        log.info("Stubbing mapping url:{}", expectedUrl);
        stubFor(WireMock.get(urlEqualTo(expectedUrl)).willReturn(mockResponse));


        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void bad_hearings_response_should_return_404() throws Exception {
        stubMappingResponse(caseUrn, caseId);
        String expectedHearingsUrl = String.format("%s%s?caseId=%s", appProperties.getHearingsUrl(), appProperties.getHearingsPath(), caseId);

        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(HTTP_NOT_FOUND)
                .withHeader("Content-Type", "application/json");
        log.info("Stubbing hearing response url:{}", expectedHearingsUrl);
        stubFor(WireMock.get(urlEqualTo(expectedHearingsUrl)).willReturn(mockResponse));

        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void empty_cp_hearings_response_should_return_404() throws Exception {
        stubMappingResponse(caseUrn, caseId);
        String expectedHearingsUrl = String.format("%s%s?caseId=%s", appProperties.getHearingsUrl(), appProperties.getHearingsPath(), caseId);

        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(HTTP_NOT_FOUND)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"hearings\": []}");
        log.info("Stubbing hearing response url:{}", expectedHearingsUrl);
        stubFor(WireMock.get(urlEqualTo(expectedHearingsUrl)).willReturn(mockResponse));

        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    private void amp_endpoint_and_verify_response(String cp_response_file, String expected_amp_response) throws Exception {
        stubMappingResponse(caseUrn, caseId);
        stubGetHearingsResponse(caseId, cp_response_file);

        String expectedResponse = readResourceContents(expected_amp_response);
        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    private void stubMappingResponse(String caseUrn, String caseId) {
        String expectedUrl = String.format("%s/%s", appProperties.getCaseMapperPath(), caseUrn);
        String responseBody = String.format("{\"caseUrn\":\"%s\", \"caseId\":\"%s\"}", caseUrn, caseId);
        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(HTTP_OK)
                .withHeader("Content-Type", "application/json")
                .withBody(responseBody);
        log.info("Stubbing mapping url:{}", expectedUrl);
        stubFor(WireMock.get(urlEqualTo(expectedUrl)).willReturn(mockResponse));
    }

    private void stubGetHearingsResponse(String caseId, String filename) {
        String expectedUrl = String.format("%s?caseId=%s", appProperties.getHearingsPath(), caseId);
        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(HTTP_OK)
                .withHeader("Content-Type", "application/json")
                .withBody(readResourceContents(filename));
        log.info("Stubbing hearings url:{}", expectedUrl);
        stubFor(WireMock.get(urlEqualTo(expectedUrl)).willReturn(mockResponse));
    }

    @SneakyThrows
    private String readResourceContents(final String resourceName) {
        URL resource = getClass().getClassLoader().getResource(resourceName);
        return Files.readString(Path.of(resource.toURI()));
    }
}