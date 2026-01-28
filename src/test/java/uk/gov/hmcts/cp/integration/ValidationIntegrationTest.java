package uk.gov.hmcts.cp.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class ValidationIntegrationTest extends IntegrationTestBase {

    @Test
    void random_urn_should_throw_404() throws Exception {
        mockMvc.perform(get("/something-else", ""))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint GET /something-else."));
    }

    @Test
    void empty_caseUrn_should_throw_404() throws Exception {
        String caseUrn = "";
        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint GET /case//courtschedule."));
    }
}
