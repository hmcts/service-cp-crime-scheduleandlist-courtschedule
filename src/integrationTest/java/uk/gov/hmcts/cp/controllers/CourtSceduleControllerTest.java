package uk.gov.hmcts.cp.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class CourtSceduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("Should /case/{case_urn}/courtschedule request with 200 response code")
    @Test
    void shouldCallActuatorAndGet200() throws Exception {
        mockMvc.perform(get("/case/123/courtschedule"))
            .andDo(print())
            .andExpect(status().isOk());
    }
}
