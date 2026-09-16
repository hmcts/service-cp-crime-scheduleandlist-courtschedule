package uk.gov.hmcts.cp.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.hmcts.cp.openapi.api.CourtScheduleApi;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationPolicyTest {

    /**
     * Every path the OpenAPI contract publishes, read by reflection from the generated API
     * interface rather than restated here. A new operation therefore arrives in this suite on the
     * next contract bump, with no list to remember to update.
     *
     * <p>The source matters: these come from the <i>contract</i>, which is independent of
     * {@link AuthorizationPolicy}. Deriving them from the policy's own exemption lists instead would
     * make the deny-by-default test tautological — widen an exemption and it would keep passing.
     */
    static Stream<String> contractPaths() {
        return Stream.of(CourtScheduleApi.class.getMethods())
                .map(AuthorizationPolicyTest::mappedPaths)
                .filter(Objects::nonNull)
                .flatMap(java.util.Arrays::stream)
                .map(AuthorizationPolicyTest::withConcreteIds)
                .distinct()
                .sorted();
    }

    private static String[] mappedPaths(final Method method) {
        // findMergedAnnotation also resolves @GetMapping and friends, which are meta-annotated
        // with @RequestMapping, so this does not depend on which form the generator emitted.
        final RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        return mapping == null || mapping.value().length == 0 ? null : mapping.value();
    }

    /** {@code /case/{case_urn}/courtschedule} is never a real request URI; the filter sees a concrete one. */
    private static String withConcreteIds(final String templated) {
        return PATH_VARIABLE.matcher(templated).replaceAll(CONCRETE_ID);
    }

    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]+}");
    private static final String CONCRETE_ID = "ABCD1234567";

    private final AuthorizationPolicy policy = new AuthorizationPolicy("/actuator");

    @Test
    @DisplayName("path discovery actually found the contract, so the deny-by-default test cannot pass vacuously")
    void contractDiscoveryIsNotEmpty() {
        // Without this, a generator change that stopped emitting @RequestMapping would empty the
        // stream and the parameterised case below would silently pass having asserted nothing.
        assertThat(contractPaths()).isNotEmpty();
    }

    @ParameterizedTest
    @MethodSource("contractPaths")
    @DisplayName("deny by default: every path in the contract requires a token")
    void everyContractPathRequiresAToken(final String path) {
        assertThat(policy.isExemptFromValidation(path))
                .withFailMessage("path %s must require a token", path)
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/error", "/actuator", "/actuator/health", "/actuator/info", "/actuator/prometheus"})
    void publicPathsDoNotRequireAToken(final String path) {
        assertThat(policy.isExemptFromValidation(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/errorx", "/actuatorx", "/health", "/info", "/prometheus"})
    @DisplayName("a path that merely starts with a public path is not public")
    void publicPrefixDoesNotLeakToNeighbouringPaths(final String path) {
        assertThat(policy.isExemptFromValidation(path)).isFalse();
    }

    // ------------------------------------------------------------------ roles

    @Test
    @DisplayName("the known role is accepted")
    void knownRoleIsAccepted() {
        assertThatCode(() -> policy.assertAuthorized(caller(AuthorizationPolicy.ROLE_READ)))
                .doesNotThrowAnyException();
    }

    @Test
    void callerWithAnUnrecognisedRoleIsRejected() {
        assertThatThrownBy(() -> policy.assertAuthorized(caller("app.somethingelse")))
                .isInstanceOf(TokenValidationException.class)
                .extracting(ex -> ((TokenValidationException) ex).getReason())
                .isEqualTo(TokenValidationException.Reason.INSUFFICIENT_ROLE);
    }

    @Test
    void callerWithNoRolesIsRejected() {
        assertThatThrownBy(() -> policy.assertAuthorized(new ValidatedCaller(UUID.randomUUID(), List.of(), true)))
                .isInstanceOf(TokenValidationException.class);
    }

    @Test
    @DisplayName("only app.read is recognised")
    void onlyOneRoleExists() {
        assertThat(policy.knownRoles()).containsExactly(AuthorizationPolicy.ROLE_READ);
    }

    private static ValidatedCaller caller(final String... roles) {
        return new ValidatedCaller(UUID.randomUUID(), List.of(roles), true);
    }
}
