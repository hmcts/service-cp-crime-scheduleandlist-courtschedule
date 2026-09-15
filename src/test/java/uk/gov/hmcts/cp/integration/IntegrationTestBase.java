package uk.gov.hmcts.cp.integration;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.helper.JwtHelper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Integration tests run against real token validation, with the JWKS supplied in-process by
        // JwtHelper. They must not run with enforcement off, or they would stop covering the
        // authentication path entirely.
        "auth.mode=ENFORCE",
        "auth.tenant-id=" + JwtHelper.TENANT_ID,
        "auth.audience=" + JwtHelper.AUDIENCE
})
// TestJwksConfiguration is imported explicitly - Boot's nested-@TestConfiguration auto-detection
// only scans a test class's own enclosing classes, not an inherited superclass like this one.
@Import(IntegrationTestBase.TestJwksConfiguration.class)
@Slf4j
public abstract class IntegrationTestBase {

    /**
     * Serves the test signing key as the application's JWKS, replacing the bean that would otherwise
     * fetch Entra's.
     *
     * <p>Registered under its own name and marked {@code @Primary} rather than overriding the
     * production bean's name: a same-name override depends on registration order between the test
     * config and the component-scanned {@code AppConfig}, which is not guaranteed to resolve in the
     * test config's favour and silently falls back to the real (network) JWKS when it does not — the
     * trap the entra-token-validation standard specifically warns against. This is the only thing
     * stubbed — signature, issuer, audience, expiry, app-only and role checks all run for real.
     */
    @TestConfiguration
    static class TestJwksConfiguration {

        @Bean
        @Primary
        JWKSource<SecurityContext> testEntraJwkSource() {
            return JwtHelper.jwkSource();
        }
    }

    @Autowired
    AppPropertiesBackend appProperties;

    @Resource
    protected MockMvc mockMvc;
}
