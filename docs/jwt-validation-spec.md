# Entra access token validation

How this service authenticates a caller, and — more usefully — **why it does what it does**.

This document deliberately does not restate the rules. The rules are the code and its tests, which
cannot drift from the behaviour:

| Question | Where the answer lives |
|---|---|
| What is validated, and how? | `uk.gov.hmcts.cp.auth.EntraTokenValidator` |
| Which requests need a token? | `uk.gov.hmcts.cp.auth.AuthorizationPolicy` |
| What is rejected, and with what status? | `EntraTokenValidatorTest` — the conformance suite |

What follows is the part none of those can express: the decisions, the traps, and the prerequisites.

---

## 1. Why validation exists here at all

This service shipped with **no** in-application token validation at all: every request reached
`CourtScheduleController` unauthenticated. APIM's `validate-jwt` policy blocks that from the
internet, but not from anything that reaches the pod directly — in-cluster callers, a port-forward,
a misrouted ingress. **That is what in-application validation is for: the paths the gateway does not
see.** It duplicates the gateway deliberately.

---

## 2. Decisions

### Nimbus, not Spring Security

Every hard requirement here — JWKS caching, rate-limited refresh, outage tolerance,
single-algorithm pinning — is a Nimbus feature (`DefaultJWTProcessor`, `JWKSourceBuilder`). Spring
Security would add a framework to reach a library this service can depend on directly, and its
filter chain registers at `spring.security.filter.order` (default `-100`), i.e. **after**
`TracingFilter` and `ClientIdResolutionFilter`. That reordering is not free in a service whose
filter order is load-bearing.

### One algorithm, pinned in code

`alg` appears in **three** places, and only one of them is trustworthy:

| Where | Present? | Trustworthy? |
|---|---|---|
| The token's JOSE header | Yes, always | **No.** It is part of the token the caller hands us |
| The JWKS key entries | **No.** Entra's keys expose `kty, use, kid, n, e, x5c, x5t` and no `alg` | Would be, but absent |
| Our configuration — the key selector | Pinned to RS256 | Yes |

```java
setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource))
```

The selector accepts exactly one algorithm, so a token declaring any other finds no candidate key
and fails before verification is attempted. `alg: none`, algorithm confusion and token-supplied key
material (`jku`/`jwk`/`x5u`) are **structurally impossible** rather than separately defended —
covered by `rejectsUnsignedToken`, `rejectsAlgorithmConfusionAttack` and `ignoresHeaderSuppliedKey`.

### App-only proven by `sub == oid`, never by `idtyp`

`idtyp: app` would be the direct check, but Entra omits it unless explicitly enabled as an optional
claim on the app registration. Requiring it today would reject all legitimate traffic. App-only is
inferred instead from `sub == oid` (an app-only token's subject is the service principal object id)
plus `roles` present and `scp` absent. `acceptsTokenWithoutIdtypClaim` is the regression guard —
nobody should later "harden" this into an outage.

### One role, no read/write separation

Entra issues a single application role, `court-schedule.read`, for this API. `AuthorizationPolicy`
recognises exactly that one role; there is nothing to separate.

### `azp` is the identity, `oid`/`sub` is not

`azp` is the calling application's client id. `oid`/`sub` is the service principal object id for
that application in the tenant — seeding anything with `oid` instead of `azp` produces a
signature-valid token that then fails downstream for reasons that look unrelated to auth.

---

## 3. Endpoint coverage

`AuthorizationPolicy.isExemptFromValidation` is deny-by-default: every path requires a token unless
listed. The exemption list is exact-match only, never a prefix — `AuthorizationPolicyTest` proves
near-miss paths (`/healthx`, `/health/x`) stay protected, and that every path in the OpenAPI contract
(read by reflection from `CourtScheduleApi`, not restated in the test) requires one.

**Exempt** (infrastructure, no case data): `/`, `/health`, `/info`, `/prometheus`, `/error`. Note
`management.endpoints.web.base-path` is `/` in this service, so actuator endpoints are top-level
paths rather than under `/actuator`.

**Protected:** `GET /case/{case_urn}/courtschedule` — the only business endpoint.

This service has no internal producer endpoint that calls in without an `Authorization` header, so
there is no equivalent of an `INTERNAL_UNVALIDATED_PATHS` exemption to carry.

---

## 4. Configuration

| Property | Env var | Purpose | Default |
|---|---|---|---|
| `auth.mode` | `AUTH_MODE` | `OFF` / `OBSERVE` / `ENFORCE` | `ENFORCE` |
| `auth.tenant-id` | `AUTH_TENANT_ID` | Issuing tenant | blank — no default on purpose |
| `auth.audience` | `AUTH_AUDIENCE` | This API's own audience | blank — no default on purpose |
| `auth.issuer` | `AUTH_ISSUER` | Overrides the tenant-derived issuer | derived |
| `auth.jwks-uri` | `AUTH_JWKS_URI` | Overrides the tenant-derived JWKS URI | derived |
| `auth.clock-skew-seconds` | `AUTH_CLOCK_SKEW_SECONDS` | Applied to `exp`/`nbf` | `60` (capped at 300) |
| `auth.jwks-cache-ttl-seconds` | `AUTH_JWKS_CACHE_TTL_SECONDS` | JWKS cache TTL | `600` |

`auth.mode` is trusted as injected by the deployment (Kubernetes) per environment — this service
does not independently gate it against which environment it thinks it is running in.
`EntraAuthProperties` fails startup only if `tenant-id`/`audience` is blank while `mode != OFF`.
Leaving tenant/audience blank is deliberate: a default would let a deployment that forgot to inject
come up healthy against another environment's tenant and reject every token, silently. Run locally
with `AUTH_MODE=OFF`, which skips the check.

---

## 5. Deliberately not checked

In-app TLS enforcement, token size limits, duplicate JSON keys, `iat` max age, `typ`, and replay or
nonce tracking — each lacks a threat model in this context. `tid`/`ver` are checked as exact-match
claims even though `iss` already implies both; kept as defence in depth, not counted as independent
coverage.

---

## 6. Entra prerequisites (not code)

- App registration exposing this API's audience.
- The `court-schedule.read` app role declared **and assigned, with admin consent** — a
  declared-but-unconsented role produces a token that looks correct but silently omits `roles`.
- `requestedAccessTokenVersion` pinned to `2`.
- Per-environment tenant/audience values wired into the deployment config
  (`hmcts/cp-vp-aks-deploy`) — not something this repo can set on its own.
