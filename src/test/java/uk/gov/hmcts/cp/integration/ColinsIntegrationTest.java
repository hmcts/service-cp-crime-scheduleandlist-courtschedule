package uk.gov.hmcts.cp.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class ColinsIntegrationTest extends IntegrationTestBase {

    @Autowired
    AppPropertiesBackend appProperties;

    String caseUrn = "ABCD1234567";
    String caseId = UUID.randomUUID().toString();

    @BeforeEach
    void beforeEach() {
        super.beforeEach();
    }

    @AfterEach
    void afterEach() {
        super.afterEach();
    }

    @Test
    void get_court_schedule_should_return_ok() throws Exception {
        stubMappingResponse(caseUrn, caseId);
        stubGetHearingsResponse(caseId, "cp_allocated_and_unallocated_response.json");

        MvcResult result = mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String actualResponse = result.getResponse().getContentAsString();
        assertThat(actualResponse).contains("\"courtHouse\":\"63c1045a-7e41-3b6f-b2ae-df49086ac34e\"");
    }

    @Test
    void get_court_schedule_should_error_NOT() throws Exception {
        stubMappingResponse(caseUrn, caseId);
        stubGetHearingsResponse(caseId, "cp_bug.json");

        MvcResult result = mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String actualResponse = result.getResponse().getContentAsString();
        assertThat(actualResponse).contains("\"courtHouse\":\"f8254db1-1683-483e-afb3-b87fde5a0a26\"");
    }
}