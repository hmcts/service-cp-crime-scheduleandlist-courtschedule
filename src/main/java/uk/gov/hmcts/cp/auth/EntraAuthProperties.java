package uk.gov.hmcts.cp.auth;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Configuration for Entra access token validation.
 *
 * <p>Fails startup rather than degrading: a blank audience must never mean "accept any audience".
 * {@code auth.mode} itself is trusted as injected by the deployment (Kubernetes) — this service
 * does not second-guess it against which environment it is running in.
 *
 * <p>{@code auth.*} is the primary issuer (required). {@code auth.cp-*} is the legacy CP issuer
 * (optional); blank means primary-issuer tokens only.
 */
@Getter
@Service
@Slf4j
public class EntraAuthProperties {

    public static final String PRIMARY_ISSUER_NAME = "primary";
    public static final String CP_ISSUER_NAME = "cp";

    private static final int MAX_CLOCK_SKEW_SECONDS = 300;

    /** Whether a failing token is rejected. Not a token value — see {@link AuthMode}. */
    private final AuthMode mode;

    /**
     * The tenant that <b>issues</b> the tokens — matched against the token's {@code tid} claim, and
     * the value {@link #issuer} and {@link #jwksUri} are derived from when those are left blank.
     *
     * <p>Where the service is hosted in a different Azure tenant from the one issuing tokens, this is
     * the <b>issuing</b> tenant. The hosting tenant's directory id is the wrong value and would
     * reject every token.
     */
    private final String tenantId;

    /**
     * This API's own application id, matched against the token's {@code aud} claim.
     *
     * <p>The single highest-value check: {@code aud} is the only thing that rejects a genuine,
     * correctly signed token minted for a <i>different</i> resource — a Microsoft Graph token, or a
     * sibling API's. v2.0 tokens carry the bare GUID here, not {@code api://<...>}.
     */
    private final String audience;

    /**
     * Matched against the token's {@code iss} claim, exactly — never by prefix, which would admit
     * {@code .../v2.0.attacker.example}. Derived from {@link #tenantId} when not set.
     */
    private final String issuer;

    /**
     * Where the public signing keys are fetched from. The token's {@code kid} header selects one of
     * them; nothing in the token influences this URL, or a caller could nominate the key that
     * verifies it. Derived from {@link #tenantId} when not set.
     */
    private final String jwksUri;

    /**
     * CP issuing tenant. AMP-1380: CP tokens are accepted until consumers onboarded via CP APIM have
     * migrated; AMP-1381 removes this. Blank means CP tokens are rejected.
     */
    private final String cpTenantId;

    /** This API's own audience in the CP tenant. */
    private final String cpAudience;

    /** Derived from {@link #cpTenantId} when not set. */
    private final String cpIssuer;

    /** Derived from {@link #cpTenantId} when not set. */
    private final String cpJwksUri;

    /**
     * Tolerance applied to the token's {@code exp} and {@code nbf} claims, absorbing clock drift
     * between Entra and this pod. Capped at {@value #MAX_CLOCK_SKEW_SECONDS} seconds, since a large
     * enough value turns {@code exp} into a no-op.
     */
    private final int clockSkewSeconds;

    /**
     * How long the fetched JWKS is cached, so verification costs no outbound call per request. Not a
     * token value. An unknown {@code kid} triggers a rate-limited refresh regardless, which is how
     * Entra's key rotation is absorbed without a redeploy.
     */
    private final long jwksCacheTtlSeconds;

    /** Primary first, then CP when configured. */
    private final List<TrustedIssuer> trustedIssuers;

    public EntraAuthProperties(
            @Value("${auth.mode:ENFORCE}") final AuthMode mode,
            @Value("${auth.tenant-id:}") final String tenantId,
            @Value("${auth.audience:}") final String audience,
            @Value("${auth.issuer:}") final String issuer,
            @Value("${auth.jwks-uri:}") final String jwksUri,
            @Value("${auth.cp-tenant-id:}") final String cpTenantId,
            @Value("${auth.cp-audience:}") final String cpAudience,
            @Value("${auth.cp-issuer:}") final String cpIssuer,
            @Value("${auth.cp-jwks-uri:}") final String cpJwksUri,
            @Value("${auth.clock-skew-seconds:60}") final int clockSkewSeconds,
            @Value("${auth.jwks-cache-ttl-seconds:600}") final long jwksCacheTtlSeconds) {

        this.mode = mode;
        this.tenantId = normaliseId(tenantId);
        this.audience = audience.trim();
        this.clockSkewSeconds = Math.min(clockSkewSeconds, MAX_CLOCK_SKEW_SECONDS);
        this.jwksCacheTtlSeconds = jwksCacheTtlSeconds;
        this.issuer = issuer.isBlank() ? defaultIssuer(this.tenantId) : issuer.trim();
        this.jwksUri = jwksUri.isBlank() ? defaultJwksUri(this.tenantId) : jwksUri.trim();

        this.cpTenantId = normaliseId(cpTenantId);
        this.cpAudience = cpAudience.trim();
        this.cpIssuer = derivedOrBlank(cpIssuer, this.cpTenantId, true);
        this.cpJwksUri = derivedOrBlank(cpJwksUri, this.cpTenantId, false);

        validate();
        this.trustedIssuers = buildTrustedIssuers();

        log.info("Entra auth initialised mode:{} clockSkewSeconds:{} trustedIssuers:{}",
                this.mode, this.clockSkewSeconds, describe(this.trustedIssuers));
        if (this.mode != AuthMode.ENFORCE) {
            log.warn("SECURITY: Entra token enforcement is {} — client identity is taken from an "
                    + "UNVERIFIED token. This must not be used in a deployed environment.", this.mode);
        }
    }

    public final boolean isCpIssuerEnabled() {
        return !cpTenantId.isEmpty();
    }

    private void validate() {
        validateCpPair();
        if (mode == AuthMode.OFF) {
            return;
        }
        if (tenantId.isBlank()) {
            throw new IllegalStateException("auth.tenant-id must be set when auth.mode=" + mode);
        }
        if (audience.isBlank()) {
            throw new IllegalStateException(
                    "auth.audience must be set when auth.mode=" + mode
                            + ". A blank audience would accept tokens minted for any resource, "
                            + "including Microsoft Graph.");
        }
        if (sharesPrimaryIssuer() && !matchesPrimaryTenantAndKeys()) {
            throw new IllegalStateException(
                    "auth.cp-issuer equals auth.issuer but the tenant or JWKS URI differs.");
        }
    }

    /** All-or-nothing: a half-set CP pair is a misconfiguration, never silently ignored. */
    private void validateCpPair() {
        if (isCpAudienceMissing()) {
            throw new IllegalStateException("auth.cp-audience must be set when auth.cp-tenant-id is set.");
        }
        if (hasCpValuesWithoutTenant()) {
            throw new IllegalStateException("auth.cp-tenant-id must be set when any other auth.cp-* value is set.");
        }
    }

    private boolean isCpAudienceMissing() {
        return isCpIssuerEnabled() && cpAudience.isEmpty();
    }

    private boolean hasCpValuesWithoutTenant() {
        return !isCpIssuerEnabled() && (!cpAudience.isEmpty() || !cpIssuer.isEmpty() || !cpJwksUri.isEmpty());
    }

    /** CP configured in the same tenant as the primary issuer, so both produce the same {@code iss}. */
    private boolean sharesPrimaryIssuer() {
        return isCpIssuerEnabled() && cpIssuer.equalsIgnoreCase(issuer);
    }

    private boolean matchesPrimaryTenantAndKeys() {
        return cpTenantId.equals(tenantId) && cpJwksUri.equalsIgnoreCase(jwksUri);
    }

    /** Same tenant for both means one {@code iss}, so it becomes one issuer accepting both audiences. */
    private List<TrustedIssuer> buildTrustedIssuers() {
        final TrustedIssuer primary =
                new TrustedIssuer(PRIMARY_ISSUER_NAME, issuer, tenantId, Set.of(audience), jwksUri);
        final List<TrustedIssuer> issuers;
        if (sharesPrimaryIssuer()) {
            issuers = List.of(new TrustedIssuer(PRIMARY_ISSUER_NAME + "+" + CP_ISSUER_NAME,
                    issuer, tenantId, Set.copyOf(List.of(audience, cpAudience)), jwksUri));
        } else if (isCpIssuerEnabled()) {
            issuers = List.of(primary,
                    new TrustedIssuer(CP_ISSUER_NAME, cpIssuer, cpTenantId, Set.of(cpAudience), cpJwksUri));
        } else {
            issuers = List.of(primary);
        }
        return issuers;
    }

    private static String describe(final List<TrustedIssuer> issuers) {
        return issuers.stream()
                .map(trusted -> trusted.name() + "{issuer:" + trusted.issuer() + " audience:" + trusted.audiences() + "}")
                .toList()
                .toString();
    }

    /** An explicit value wins; otherwise derived from the tenant, or blank when no tenant is set. */
    private static String derivedOrBlank(final String explicit, final String tenant, final boolean issuer) {
        final String value;
        if (explicit.isBlank() && tenant.isEmpty()) {
            value = "";
        } else if (explicit.isBlank()) {
            value = issuer ? defaultIssuer(tenant) : defaultJwksUri(tenant);
        } else {
            value = explicit.trim();
        }
        return value;
    }

    /** Tenant ids are GUIDs: trimmed and lower-cased, as Entra emits {@code tid}. */
    private static String normaliseId(final String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static String defaultIssuer(final String tenantId) {
        return "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    private static String defaultJwksUri(final String tenantId) {
        return "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
    }
}
