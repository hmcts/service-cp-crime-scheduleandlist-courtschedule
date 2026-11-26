package uk.gov.hmcts.cp.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntraAuthPropertiesTest {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String AUDIENCE = "22222222-2222-2222-2222-222222222222";

    @Test
    @DisplayName("off mode does not require audience or tenant")
    void offModeDoesNotRequireAudienceOrTenant() {
        assertThatCode(() -> build(AuthMode.OFF, "", ""))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a blank audience fails startup rather than accepting tokens for any resource")
    void blankAudienceFailsStartup() {
        assertThatThrownBy(() -> build(AuthMode.ENFORCE, TENANT, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.audience must be set");
    }

    @Test
    void blankTenantFailsStartup() {
        assertThatThrownBy(() -> build(AuthMode.ENFORCE, "", AUDIENCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.tenant-id must be set");
    }

    @Test
    @DisplayName("issuer and jwks uri are derived from the tenant when not set explicitly")
    void derivesIssuerAndJwksUriFromTenant() {
        final EntraAuthProperties properties = build(AuthMode.ENFORCE, TENANT, AUDIENCE);

        assertThat(properties.getIssuer())
                .isEqualTo("https://login.microsoftonline.com/" + TENANT + "/v2.0");
        assertThat(properties.getJwksUri())
                .isEqualTo("https://login.microsoftonline.com/" + TENANT + "/discovery/v2.0/keys");
    }

    @Test
    @DisplayName("clock skew is capped so a large value cannot effectively disable expiry")
    void clockSkewIsCapped() {
        final EntraAuthProperties properties = new EntraAuthProperties(
                AuthMode.ENFORCE, TENANT, AUDIENCE, "", "", 86_400, 600);

        assertThat(properties.getClockSkewSeconds()).isEqualTo(300);
    }

    private static EntraAuthProperties build(final AuthMode mode, final String tenantId, final String audience) {
        return new EntraAuthProperties(mode, tenantId, audience, "", "", 60, 600);
    }
}
