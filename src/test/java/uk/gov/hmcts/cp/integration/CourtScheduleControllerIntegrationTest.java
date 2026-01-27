package uk.gov.hmcts.cp.integration;

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
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.domain.HearingResponse;

import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class CourtScheduleControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    AppPropertiesBackend appProperties;
    @MockitoBean
    RestTemplate restTemplate;
    @MockitoBean
    HttpClient httpClient;

    @Test
    void get_court_schedule_should_return_ok() throws Exception {
        String caseUrn = "test-case-urn";
        String caseId = UUID.randomUUID().toString();
        CaseMapperResponse caseMapperResponse = CaseMapperResponse.builder().caseId(caseId).build();
        mockMapperResponse(caseUrn, HttpStatus.OK, caseMapperResponse);
        HearingResponse hearingResponse = HearingResponse.builder().hearings(List.of(dummyHearing())).build();
        mockHearingsResponse(caseId, HttpStatus.OK, hearingResponse);

        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("courtSchedule[0].hearings[0].hearingDescription").value("Plea and Trial Preparation"));
    }

    private HearingResponse.HearingSchedule dummyHearing() {
        // TODO COLING implement tolerant mapper. Currently get rubbish error if these fields are null
        return HearingResponse.HearingSchedule.builder()
                .id(UUID.randomUUID().toString())
                .allocated(true)
                .type(HearingResponse.HearingSchedule.HearingType.builder().description("Plea and Trial Preparation").build())
                .judiciary(List.of())
                .hearingDays(List.of())
                .build();
    }

    private void mockMapperResponse(String caseUrn, HttpStatus httpStatus, CaseMapperResponse response) {
        String expectedUrl = String.format("%s%s/%s", appProperties.getCaseMapperUrl(), appProperties.getCaseMapperPath(), caseUrn);
        log.info("Mocking {} response", expectedUrl);
        when(restTemplate.exchange(
                eq(expectedUrl),
                eq(HttpMethod.GET),
                eq(expectedMapperHeaders()),
                eq(CaseMapperResponse.class)
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