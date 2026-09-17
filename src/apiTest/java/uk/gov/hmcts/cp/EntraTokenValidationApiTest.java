package uk.gov.hmcts.cp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.auth.AuthMode;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * End-to-end check that token validation works against a <b>running</b> service, using a
 * <b>real</b> token obtained from Entra rather than a locally minted one.
 *
 * <p>This is the only test that proves the whole chain: Entra issues a token, the service fetches
 * that tenant's real JWKS over the network, and the signature verifies. Everything else in the
 * codebase stubs the JWKS in-process, so a wrong tenant, a wrong audience or blocked egress to
 * {@code login.microsoftonline.com} would pass every other test and fail in a deployed environment.
 *
 * <p><b>Manual only — {@link Disabled} so the pipeline never runs it.</b> It needs a real client
 * secret and outbound internet access, neither of which belongs in CI.
 *
 * <h2>Running it</h2>
 *
 * Configuration comes from environment variables — none have defaults here, since this repo has no
 * established dev tenant/audience/client yet (see docs/jwt-validation-spec.md, section 6). Keep them
 * in a git-ignored {@code .env} at the repository root and let <a href="https://direnv.net">direnv</a>
 * load it:
 *
 * <pre>
 * ENTRA_TENANT_ID=...
 * ENTRA_AUDIENCE=...
 * ENTRA_CLIENT_ID=...
 * ENTRA_CLIENT_SECRET=...
 * printf 'dotenv\n' &gt; .envrc
 * direnv allow
 * </pre>
 *
 * The client needs the {@code app.read} app role assignment on this API's app
 * registration, admin-consented. A registration alone is not enough: without the assignment Entra
 * issues a token carrying no {@code roles}, and every case then fails on the role check rather than
 * on whatever it was meant to exercise.
 *
 * <pre>
 * cd src/apiTest &amp;&amp; ../../gradlew apiTest --no-daemon \
 *   --tests 'uk.gov.hmcts.cp.EntraTokenValidationApiTest' \
 *   -Djunit.jupiter.conditions.deactivate='org.junit.*DisabledCondition'
 * </pre>
 *
 * <b>{@code -Djunit.jupiter.conditions.deactivate} is what lifts the {@link Disabled} above.</b>
 * Without it the class simply reports {@code SKIPPED} and nothing runs.
 *
 * <h2>Running against a deployed service</h2>
 *
 * Set {@code SERVICE_BASE_URL} to the service's ingress. Three things to get right, or the result
 * will mislead:
 *
 * <ol>
 *   <li><b>Go direct to the ingress, not through APIM or WAF.</b> APIM validates the token itself, so
 *       a bad token is rejected at the gateway and never reaches the service. The test would still
 *       see a 401 and pass while proving nothing about this service. If you must go via APIM, set
 *       {@code SERVICE_SUBSCRIPTION_KEY} — and read the 401s as the gateway's, not the service's.</li>
 *   <li><b>Tenant and audience must match that environment.</b></li>
 *   <li><b>{@code SERVICE_AUTH_MODE} must match the deployment.</b> Deployed environments are always
 *       {@code ENFORCE} once startup requires it — the default here is correct and you should not
 *       need to change it.</li>
 * </ol>
 *
 * <h2>Why the "accepted" assertions only check that auth did not reject</h2>
 *
 * Unlike a DB-backed service, this endpoint calls out to the CP backend on every request. A manual
 * run against a bare {@code bootRun} instance has no CP backend behind it, so even a token that
 * clears every auth check will not return 200 — it will fail further down the stack. The assertions
 * below therefore only check that the response is not {@code 401}/{@code 403}, which is the part
 * this test actually exercises.
 */
@Disabled("Manual only: needs a real Entra client secret and internet access. See the class javadoc.")
class EntraTokenValidationApiTest {

    private static final Logger LOG = LoggerFactory.getLogger(EntraTokenValidationApiTest.class);

    /**
     * Skips certificate verification for calls to the service under test — the {@code curl -k}
     * escape hatch, for an internal ingress whose private CA you do not have to hand.
     *
     * <p>Deliberately narrow: it applies only to the service call, never to the Entra token
     * request, because that request carries the client secret and must stay authenticated. Set it
     * in {@code .env} for any internal https target; it is off by default so the insecure path is
     * always a visible choice rather than a silent one.
     */
    private static final boolean TLS_INSECURE = Boolean.parseBoolean(env("SERVICE_TLS_INSECURE", "false"));

    /** No defaults: this repo has no established dev tenant/audience/client yet. */
    private static final String TENANT_ID = env("ENTRA_TENANT_ID", null);
    private static final String AUDIENCE = env("ENTRA_AUDIENCE", null);
    private static final String CLIENT_ID = env("ENTRA_CLIENT_ID", null);
    private static final String CLIENT_SECRET = env("ENTRA_CLIENT_SECRET", null);

    private static final String BASE_URL = env("SERVICE_BASE_URL", "http://localhost:4550");

    /**
     * Only needed when {@code SERVICE_BASE_URL} points at APIM rather than straight at the ingress.
     * See the deployed-service notes in the class javadoc — going through APIM tests the gateway's
     * validation, not this service's.
     */
    private static final String SUBSCRIPTION_KEY = env("SERVICE_SUBSCRIPTION_KEY", null);

    /** The mode the service under test is running in — the expected outcomes depend on it. */
    private static final AuthMode MODE = AuthMode.valueOf(env("SERVICE_AUTH_MODE", "ENFORCE").toUpperCase(Locale.ROOT));

    /** A token-protected endpoint. The case URN does not need to be real — only the auth outcome is asserted. */
    private static final String PROTECTED_PATH = "/case/ABCD1234567/courtschedule";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeAll
    static void requireCredentials() {
        if (TENANT_ID == null || AUDIENCE == null || CLIENT_ID == null || CLIENT_SECRET == null) {
            throw new IllegalStateException(
                    "ENTRA_TENANT_ID, ENTRA_AUDIENCE, ENTRA_CLIENT_ID and ENTRA_CLIENT_SECRET must all be set. "
                            + "See the class javadoc.");
        }
        LOG.info("Service {}, tenant {}, audience {}, mode {}", BASE_URL, TENANT_ID, AUDIENCE, MODE);
    }

    // ------------------------------------------------------------ the token Entra actually issues

    @Test
    @DisplayName("a real Entra token is accepted, and its claims are the shape the service expects")
    void realEntraTokenIsAccepted() {
        final String token = acquireToken(AUDIENCE + "/.default");

        // Fail loudly on the two claim shapes that would otherwise cause a confusing 401 later.
        final String payload = decodePayload(token);
        assertThat(payload)
                .as("aud must be the bare GUID, not api://... — see docs/jwt-validation-spec.md")
                .contains("\"aud\":\"" + AUDIENCE + "\"");
        assertThat(payload).as("app-only tokens carry roles").contains("\"roles\"");
        LOG.info("Entra returned claims: {}", payload);

        assertNotAuthRejected("valid token", get(PROTECTED_PATH, BEARER_PREFIX + token));
    }

    @Test
    @DisplayName("a real token for a different resource is rejected on audience")
    void tokenForAnotherResourceIsRejected() {
        // A genuine, correctly signed Entra token from the same tenant — but minted for Graph.
        // Only the audience check rejects this, which is why it is the highest-value claim.
        final String graphToken = acquireToken("https://graph.microsoft.com/.default");

        assertAuthOutcome("real token, wrong audience", BEARER_PREFIX + graphToken, HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------ forgeries

    @Test
    @DisplayName("a token with a tampered signature is rejected")
    void tamperedTokenIsRejected() {
        final String token = acquireToken(AUDIENCE + "/.default");
        final String tampered = token.substring(0, token.length() - 6) + "AAAAAA";

        // Under OFF and OBSERVE the claims are still readable, so the request is allowed through --
        // that is what those modes mean, and confirming it is the point.
        assertAuthOutcome("tampered signature", BEARER_PREFIX + tampered, HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("an unsigned token (alg: none) is rejected")
    void unsignedTokenIsRejected() {
        final String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        final String claims = base64Url("{\"azp\":\"" + CLIENT_ID + "\",\"aud\":\"" + AUDIENCE + "\"}");

        assertAuthOutcome("alg:none", BEARER_PREFIX + header + "." + claims + ".", HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------ mode-independent behaviour

    @Test
    @DisplayName("no Authorization header is rejected in every mode, with a Bearer challenge")
    void missingTokenIsAlwaysRejected() {
        final ResponseEntity<String> response = get(PROTECTED_PATH, null);

        assertThat(response.getStatusCode())
                .as("a missing token is rejected even in OFF and OBSERVE")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getFirst("WWW-Authenticate"))
                .as("RFC 6750 challenge").startsWith("Bearer");
    }

    @Test
    @DisplayName("an exempt path needs no token in any mode")
    void exemptPathNeedsNoToken() {
        final ResponseEntity<String> response = get("/actuator/health", null);

        // Deployed ingresses and APIM products routinely do not expose actuator endpoints. A 404
        // means "not routed here", which is not a validation failure — abort rather than report one.
        assumeTrue(response.getStatusCode() != HttpStatus.NOT_FOUND,
                "/actuator/health is not routed at " + BASE_URL + " — nothing to assert");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a rejection never echoes the token")
    void rejectionDoesNotEchoTheToken() {
        final String token = acquireToken(AUDIENCE + "/.default");
        final String tampered = token.substring(0, token.length() - 6) + "AAAAAA";

        final ResponseEntity<String> response = get(PROTECTED_PATH, BEARER_PREFIX + tampered);
        final String body = response.getBody() == null ? "" : response.getBody();

        assertThat(body).doesNotContain(tampered).doesNotContain(token);
    }

    // ------------------------------------------------------------ helpers

    /**
     * Asserts the auth-layer outcome the running service should produce for a bad token, which
     * depends on the mode it was started in: ENFORCE rejects, OBSERVE and OFF let it through (logging
     * what would have been rejected) — checked as "not rejected" rather than 200, since the CP
     * backend behind this endpoint is not guaranteed to be running for a manual check.
     */
    private void assertAuthOutcome(final String what, final String authorization, final HttpStatus whenEnforcing) {
        final ResponseEntity<String> response = get(PROTECTED_PATH, authorization);
        LOG.info("{} in mode {} -> {}", what, MODE, response.getStatusCode());

        if (MODE == AuthMode.ENFORCE) {
            assertThat(response.getStatusCode()).as("%s, service running in %s", what, MODE).isEqualTo(whenEnforcing);
        } else {
            assertNotAuthRejected(what, response);
        }
    }

    private void assertNotAuthRejected(final String what, final ResponseEntity<String> response) {
        assertThat(response.getStatusCode())
                .as("%s must not be rejected by the auth layer", what)
                .isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> get(final String path, final String authorization) {
        RestClient.RequestHeadersSpec<?> request = serviceClient()
                .get()
                .uri(BASE_URL + path);
        if (authorization != null) {
            request = request.header(AUTHORIZATION, authorization);
        }
        if (SUBSCRIPTION_KEY != null) {
            request = request.header("Ocp-Apim-Subscription-Key", SUBSCRIPTION_KEY);
        }
        return request.retrieve()
                // Do not throw on 4xx: the status is what is being asserted.
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(String.class);
    }

    /**
     * The client used for the service under test. Verification is disabled only when
     * {@code SERVICE_TLS_INSECURE=true}, and never for a live-looking host — accepting any
     * certificate would also stop the test noticing it had reached the wrong endpoint entirely.
     */
    private static RestClient serviceClient() {
        return TLS_INSECURE ? insecureServiceClient() : RestClient.create();
    }

    private static RestClient insecureServiceClient() {
        final String host = BASE_URL.toLowerCase(Locale.ROOT);
        if (host.contains("prd") || host.contains(".lv.") || host.contains("live")) {
            throw new IllegalStateException(
                    "SERVICE_TLS_INSECURE must not be used against a live host: " + BASE_URL);
        }
        LOG.warn("TLS VERIFICATION DISABLED for {} — SERVICE_TLS_INSECURE is set. "
                + "Certificates are not checked, so this run does not prove which host answered.", BASE_URL);
        try {
            // Deliberate curl-k equivalent, opt-in only via SERVICE_TLS_INSECURE (default false),
            // refused above for a live-looking host, never used for the Entra token request, and
            // this whole class is @Disabled by default.
            final TrustManager[] trustAll = {new X509TrustManager() {
                @Override
                public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                    // deliberately permissive: see SERVICE_TLS_INSECURE
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
                    // deliberately permissive: see SERVICE_TLS_INSECURE
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, null); // codeql[java/insecure-trustmanager] see SERVICE_TLS_INSECURE above
            // The JDK client checks the hostname inside the engine, so a permissive trust manager
            // alone is not enough to match curl -k.
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
            return RestClient.builder()
                    .requestFactory(new JdkClientHttpRequestFactory(
                            HttpClient.newBuilder().sslContext(sslContext).build()))
                    .build();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("could not build an insecure SSL context", e);
        }
    }

    /** Client credentials grant against the configured tenant — the same call a consumer makes. */
    private String acquireToken(final String scope) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", CLIENT_ID);
        form.add("client_secret", CLIENT_SECRET);
        form.add("scope", scope);

        final String response = RestClient.create()
                .post()
                .uri("https://login.microsoftonline.com/" + TENANT_ID + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);

        final String token = readStringField(response, "access_token");
        assertThat(token).as("Entra returned no access_token for scope %s", scope).isNotBlank();
        return token;
    }

    @SuppressWarnings("unchecked")
    private static String readStringField(final String json, final String field) {
        try {
            final java.util.Map<String, Object> parsed = OBJECT_MAPPER.readValue(json, java.util.Map.class);
            final Object value = parsed.get(field);
            return value == null ? null : value.toString();
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("could not read field " + field + " from Entra response", e);
        }
    }

    private static String decodePayload(final String token) {
        final String segment = token.split("\\.")[1];
        return new String(Base64.getUrlDecoder()
                .decode(segment + "=".repeat((4 - segment.length() % 4) % 4)),
                StandardCharsets.UTF_8);
    }

    private static String base64Url(final String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String env(final String name, final String fallback) {
        final String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
