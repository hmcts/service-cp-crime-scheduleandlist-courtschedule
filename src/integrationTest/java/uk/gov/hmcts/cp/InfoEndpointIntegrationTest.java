package uk.gov.hmcts.cp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.cp.config.IntegrationTestConfig;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        "service.case-mapper-service.url=https://CASE-MAPPER.org.uk",
        "service.court-schedule-client.url=https://COURT-SCHEDULE.org.uk",
        "service.court-schedule-client.cjscppuid=MOCK-CJSCPPUID",
        "management.tracing.enabled=true"
})
@Import(IntegrationTestConfig.class)
class InfoEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_info_with_git_details() throws Exception {
        mockMvc.perform(get("/info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.git").exists())
                .andExpect(jsonPath("$.git.branch").value(notNullValue()))
                .andExpect(jsonPath("$.git.commit.id").value(notNullValue()))
                .andExpect(jsonPath("$.git.commit.time").value(notNullValue()));
    }

    @Test
    void should_return_info_with_build_details() throws Exception {
        mockMvc.perform(get("/info"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.build").exists())
                .andExpect(jsonPath("$.build.artifact").value("service-cp-crime-scheduleandlist-courtschedule"))
                .andExpect(jsonPath("$.build.name").value("service-cp-crime-scheduleandlist-courtschedule"))
                .andExpect(jsonPath("$.build.time").value(notNullValue()))
                .andExpect(jsonPath("$.build.version").value(notNullValue()))
                .andExpect(jsonPath("$.build.group").value("uk.gov.hmcts.cp"));
    }
}

