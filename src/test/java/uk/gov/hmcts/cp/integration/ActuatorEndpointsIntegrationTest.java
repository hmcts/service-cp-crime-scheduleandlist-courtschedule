package uk.gov.hmcts.cp.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.cp.helper.JwtHelper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The auth counters are useless if nothing can read them. Exposing "prometheus" under
 * management.endpoints.web.exposure.include does nothing on its own — the endpoint only exists when
 * a registry implementation is on the classpath, and without one this scrape 404s.
 */
class ActuatorEndpointsIntegrationTest extends IntegrationTestBase {

    private WireMockServer wireMockServer;

    @BeforeEach
    void beforeEach() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8081));
        wireMockServer.start();
    }

    @AfterEach
    void afterEach() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    /**
     * Scrapers and probes send no Authorization header, so every exposed actuator endpoint has to
     * answer without one. AuthorizationPolicy exempts these paths; this checks the exemption
     * survives the real filter chain, with auth.mode=ENFORCE.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/actuator/info", "/actuator/prometheus"})
    void actuator_endpoints_should_answer_without_a_token(final String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void successful_auth_should_be_visible_as_a_counter() throws Exception {
        final String caseUrn = UUID.randomUUID().toString();
        // The downstream call is unstubbed and will not resolve to a 200 - irrelevant here, since
        // the auth counter increments in the filter before the controller is ever reached.
        mockMvc.perform(get("/case/{case_urn}/courtschedule", caseUrn)
                .header(AUTHORIZATION, JwtHelper.bearerTokenWithAzp(UUID.randomUUID().toString())));

        final String scrape = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(scrape).contains("cp_auth_success_total");
    }
}
