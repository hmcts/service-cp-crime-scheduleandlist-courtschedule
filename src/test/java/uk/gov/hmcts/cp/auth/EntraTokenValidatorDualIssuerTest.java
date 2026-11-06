package uk.gov.hmcts.cp.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.auth.TokenValidationException.Reason;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** One pod trusting primary and CP: each tenant has its own key, so issuers must never mix. */
class EntraTokenValidatorDualIssuerTest {

    private static final String PRIMARY_TENANT = "11111111-1111-1111-1111-111111111111";
    private static final String PRIMARY_AUDIENCE = "22222222-2222-2222-2222-222222222222";
    private static final String CP_TENANT = "66666666-6666-6666-6666-666666666666";
    private static final String CP_AUDIENCE = "77777777-7777-7777-7777-777777777777";
    private static final String PRIMARY_ISSUER = issuerFor(PRIMARY_TENANT);
    private static final String CP_ISSUER = issuerFor(CP_TENANT);
    private static final String CLIENT_ID = "44444444-4444-4444-4444-444444444444";
    private static final String SERVICE_PRINCIPAL_OID = "55555555-5555-5555-5555-555555555555";
    private static final String KEY_ID = "shared-kid";

    private static RSAKey primaryKey;
    private static RSAKey cpKey;
    private static EntraTokenValidator dualValidator;
    private static EntraTokenValidator primaryOnlyValidator;

    @BeforeAll
    static void setUp() throws JOSEException {
        // Same kid in both tenants on purpose: a key must be found by issuer, never by kid alone.
        primaryKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        cpKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        final Map<String, JWKSource<SecurityContext>> keysByUri = Map.of(
                jwksUriFor(PRIMARY_TENANT), new ImmutableJWKSet<>(new JWKSet(primaryKey.toPublicJWK())),
                jwksUriFor(CP_TENANT), new ImmutableJWKSet<>(new JWKSet(cpKey.toPublicJWK())));
        final JwkSourceFactory factory = keysByUri::get;

        dualValidator = new EntraTokenValidator(new EntraAuthProperties(AuthMode.ENFORCE,
                PRIMARY_TENANT, PRIMARY_AUDIENCE, "", "", CP_TENANT, CP_AUDIENCE, "", "", 60, 600), factory);
        primaryOnlyValidator = new EntraTokenValidator(new EntraAuthProperties(AuthMode.ENFORCE,
                PRIMARY_TENANT, PRIMARY_AUDIENCE, "", "", "", "", "", "", 60, 600), factory);
    }

    @Test
    @DisplayName("a primary token for the primary audience is accepted and labelled primary")
    void acceptsPrimaryToken() throws TokenValidationException {
        final ValidatedCaller caller = dualValidator.validate(primaryToken(claims -> { }));

        assertThat(caller.clientId()).isEqualTo(UUID.fromString(CLIENT_ID));
        assertThat(caller.issuerName()).isEqualTo("primary");
    }

    @Test
    @DisplayName("a CP token for the CP audience is accepted and labelled cp")
    void acceptsCpToken() throws TokenValidationException {
        final ValidatedCaller caller = dualValidator.validate(cpToken(claims -> { }));

        assertThat(caller.clientId()).isEqualTo(UUID.fromString(CLIENT_ID));
        assertThat(caller.issuerName()).isEqualTo("cp");
    }

    @Test
    @DisplayName("a CP token minted for the primary audience is rejected — pairs are never mixed")
    void rejectsCpTokenForPrimaryAudience() {
        assertReason(dualValidator, cpToken(claims -> claims.audience(PRIMARY_AUDIENCE)), Reason.INVALID_CLAIMS);
    }

    @Test
    @DisplayName("a primary token minted for the CP audience is rejected — pairs are never mixed")
    void rejectsPrimaryTokenForCpAudience() {
        assertReason(dualValidator, primaryToken(claims -> claims.audience(CP_AUDIENCE)), Reason.INVALID_CLAIMS);
    }

    @Test
    @DisplayName("a token claiming the CP issuer but signed with the primary key is rejected")
    void rejectsCpIssuerSignedWithPrimaryKey() {
        final String token = sign(primaryKey, claims(CP_ISSUER, CP_TENANT, CP_AUDIENCE));

        assertReason(dualValidator, token, Reason.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("a token claiming the primary issuer but signed with the CP key is rejected")
    void rejectsPrimaryIssuerSignedWithCpKey() {
        final String token = sign(cpKey, claims(PRIMARY_ISSUER, PRIMARY_TENANT, PRIMARY_AUDIENCE));

        assertReason(dualValidator, token, Reason.INVALID_SIGNATURE);
    }

    @Test
    @DisplayName("the CP issuer with the primary tenant id is rejected on tid")
    void rejectsCpIssuerWithPrimaryTenant() {
        assertReason(dualValidator, cpToken(claims -> claims.claim("tid", PRIMARY_TENANT)), Reason.INVALID_CLAIMS);
    }

    @Test
    @DisplayName("an issuer that is neither CP nor primary is rejected")
    void rejectsUnknownIssuer() {
        final String other = "99999999-9999-9999-9999-999999999999";

        assertReason(dualValidator, primaryToken(claims -> claims.issuer(issuerFor(other))), Reason.INVALID_CLAIMS);
    }

    @Test
    @DisplayName("a token with no iss is rejected")
    void rejectsMissingIssuer() {
        assertReason(dualValidator, primaryToken(claims -> claims.issuer(null)), Reason.INVALID_CLAIMS);
    }

    @Test
    @DisplayName("with the CP pair removed, a genuine CP token is rejected — the decommission switch")
    void cpTokenRejectedWhenCpNotConfigured() {
        assertReason(primaryOnlyValidator, cpToken(claims -> { }), Reason.INVALID_CLAIMS);
    }

    // ---------------------------------------------------------------- helpers

    private static void assertReason(final EntraTokenValidator validator, final String token, final Reason expected) {
        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(TokenValidationException.class)
                .extracting(ex -> ((TokenValidationException) ex).getReason())
                .isEqualTo(expected);
    }

    private static String primaryToken(final Consumer<JWTClaimsSet.Builder> customiser) {
        final JWTClaimsSet.Builder builder = claims(PRIMARY_ISSUER, PRIMARY_TENANT, PRIMARY_AUDIENCE);
        customiser.accept(builder);
        return sign(primaryKey, builder);
    }

    private static String cpToken(final Consumer<JWTClaimsSet.Builder> customiser) {
        final JWTClaimsSet.Builder builder = claims(CP_ISSUER, CP_TENANT, CP_AUDIENCE);
        customiser.accept(builder);
        return sign(cpKey, builder);
    }

    private static JWTClaimsSet.Builder claims(final String issuer, final String tenant, final String audience) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(SERVICE_PRINCIPAL_OID)
                .claim("oid", SERVICE_PRINCIPAL_OID)
                .claim("azp", CLIENT_ID)
                .claim("tid", tenant)
                .claim("ver", "2.0")
                .claim("roles", List.of(AuthorizationPolicy.ROLE_READ))
                .issueTime(secondsFromNow(-60))
                .notBeforeTime(secondsFromNow(-60))
                .expirationTime(secondsFromNow(3600));
    }

    private static String sign(final RSAKey key, final JWTClaimsSet.Builder claims) {
        try {
            final SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims.build());
            jwt.sign(new RSASSASigner(key));
            return "Bearer " + jwt.serialize();
        } catch (final JOSEException e) {
            throw new IllegalStateException("failed to mint test token", e);
        }
    }

    private static String issuerFor(final String tenant) {
        return "https://login.microsoftonline.com/" + tenant + "/v2.0";
    }

    private static String jwksUriFor(final String tenant) {
        return "https://login.microsoftonline.com/" + tenant + "/discovery/v2.0/keys";
    }

    private static Date secondsFromNow(final long seconds) {
        return Date.from(Instant.now().plus(seconds, ChronoUnit.SECONDS));
    }
}
