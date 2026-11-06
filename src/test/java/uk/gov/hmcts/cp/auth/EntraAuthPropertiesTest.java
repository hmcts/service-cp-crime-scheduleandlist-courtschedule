package uk.gov.hmcts.cp.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntraAuthPropertiesTest {

    private static final String TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String AUDIENCE = "22222222-2222-2222-2222-222222222222";
    private static final String CP_TENANT = "66666666-6666-6666-6666-666666666666";
    private static final String CP_AUDIENCE = "77777777-7777-7777-7777-777777777777";

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
                AuthMode.ENFORCE, TENANT, AUDIENCE, "", "", "", "", "", "", 86_400, 600);

        assertThat(properties.getClockSkewSeconds()).isEqualTo(300);
    }

    // ---------------------------------------------------------------- CP (legacy) issuer

    @Test
    @DisplayName("with no CP values only the primary issuer is trusted")
    void primaryOnlyByDefault() {
        final EntraAuthProperties properties = build(AuthMode.ENFORCE, TENANT, AUDIENCE);

        assertThat(properties.isCpIssuerEnabled()).isFalse();
        assertThat(properties.getTrustedIssuers())
                .extracting(TrustedIssuer::name)
                .containsExactly(EntraAuthProperties.PRIMARY_ISSUER_NAME);
    }

    @Test
    @DisplayName("a CP tenant and audience add a second, separate trusted issuer")
    void cpPairAddsSecondIssuer() {
        final EntraAuthProperties properties = buildWithCp(CP_TENANT, CP_AUDIENCE, "");

        assertThat(properties.getTrustedIssuers()).containsExactly(
                new TrustedIssuer("primary", "https://login.microsoftonline.com/" + TENANT + "/v2.0", TENANT,
                        java.util.Set.of(AUDIENCE),
                        "https://login.microsoftonline.com/" + TENANT + "/discovery/v2.0/keys"),
                new TrustedIssuer("cp", "https://login.microsoftonline.com/" + CP_TENANT + "/v2.0", CP_TENANT,
                        java.util.Set.of(CP_AUDIENCE),
                        "https://login.microsoftonline.com/" + CP_TENANT + "/discovery/v2.0/keys"));
    }

    @Test
    @DisplayName("a CP tenant without a CP audience fails startup")
    void cpTenantWithoutAudienceFailsStartup() {
        assertThatThrownBy(() -> buildWithCp(CP_TENANT, "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.cp-audience must be set");
    }

    @Test
    @DisplayName("a CP audience without a CP tenant fails startup rather than being ignored")
    void cpAudienceWithoutTenantFailsStartup() {
        assertThatThrownBy(() -> buildWithCp("", CP_AUDIENCE, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.cp-tenant-id must be set");
    }

    @Test
    @DisplayName("a half-set CP pair fails startup even in OFF mode")
    void halfSetCpPairFailsEvenWhenOff() {
        assertThatThrownBy(() -> new EntraAuthProperties(AuthMode.OFF, "", "", "", "",
                CP_TENANT, "", "", "", 60, 600))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("CP in the same tenant as the primary becomes one issuer accepting both audiences")
    void sameTenantMergesAudiences() {
        final EntraAuthProperties properties = buildWithCp(TENANT, CP_AUDIENCE, "");

        assertThat(properties.getTrustedIssuers()).singleElement().satisfies(trusted -> {
            assertThat(trusted.tenantId()).isEqualTo(TENANT);
            assertThat(trusted.audiences()).containsExactlyInAnyOrder(AUDIENCE, CP_AUDIENCE);
        });
    }

    @Test
    @DisplayName("a CP issuer equal to the primary issuer but in another tenant fails startup")
    void sameIssuerDifferentTenantFailsStartup() {
        final String primaryIssuer = "https://login.microsoftonline.com/" + TENANT + "/v2.0";

        assertThatThrownBy(() -> buildWithCp(CP_TENANT, CP_AUDIENCE, primaryIssuer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auth.cp-issuer equals auth.issuer");
    }

    @Test
    @DisplayName("tenant ids are trimmed and lower-cased, as Entra emits tid")
    void tenantIdsAreNormalised() {
        final EntraAuthProperties properties = buildWithCp("  " + CP_TENANT.toUpperCase() + " ", CP_AUDIENCE, "");

        assertThat(properties.getCpTenantId()).isEqualTo(CP_TENANT);
        assertThat(properties.getCpIssuer()).isEqualTo("https://login.microsoftonline.com/" + CP_TENANT + "/v2.0");
    }

    @Test
    @DisplayName("the same tenant in a different case is still one issuer accepting both audiences")
    void sameTenantDifferentCaseMerges() {
        final EntraAuthProperties properties = buildWithCp(TENANT.toUpperCase(), CP_AUDIENCE, "");

        assertThat(properties.getTrustedIssuers()).singleElement()
                .satisfies(trusted -> assertThat(trusted.audiences()).containsExactlyInAnyOrder(AUDIENCE, CP_AUDIENCE));
    }

    @Test
    @DisplayName("a CP pair identical to the primary pair starts as one issuer")
    void identicalCpPairStarts() {
        final EntraAuthProperties properties = buildWithCp(TENANT, AUDIENCE, "");

        assertThat(properties.getTrustedIssuers()).singleElement()
                .satisfies(trusted -> assertThat(trusted.audiences()).containsExactly(AUDIENCE));
    }

    private static EntraAuthProperties buildWithCp(final String cpTenant, final String cpAudience,
                                                   final String cpIssuer) {
        return new EntraAuthProperties(AuthMode.ENFORCE, TENANT, AUDIENCE, "", "",
                cpTenant, cpAudience, cpIssuer, "", 60, 600);
    }

    private static EntraAuthProperties build(final AuthMode mode, final String tenantId, final String audience) {
        return new EntraAuthProperties(mode, tenantId, audience, "", "", "", "", "", "", 60, 600);
    }
}
