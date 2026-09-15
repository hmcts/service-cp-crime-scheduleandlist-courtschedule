package uk.gov.hmcts.cp.auth;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Decides which requests need a token, and that the caller holds a recognised role.
 *
 * <p><b>Deny by default:</b> every request is validated unless its path is listed as exempt below.
 * Exemptions are enumerated, never inferred from a prefix — a prefix rule silently exempts endpoints
 * added later.
 */
@Service
public class AuthorizationPolicy {

    /** The only application role Entra issues for this API. */
    public static final String ROLE_READ = "app.read";

    private static final Set<String> KNOWN_ROLES = Set.of(ROLE_READ);

    /** Infrastructure endpoints, carrying no case data. */
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of("/", "", "/error");
    private static final List<String> PUBLIC_PATH_ROOTS = List.of("/actuator");

    /** True when the path is reachable without a token — see the two lists above. */
    public boolean isExemptFromValidation(final String requestUri) {
        final String path = stripTrailingSlash(requestUri);
        return PUBLIC_EXACT_PATHS.contains(path)
                || PUBLIC_PATH_ROOTS.stream().anyMatch(root -> path.equals(root) || path.startsWith(root + "/"));
    }

    /** The roles this API recognises; a caller needs at least one of them. */
    public Set<String> knownRoles() {
        return KNOWN_ROLES;
    }

    /**
     * @throws TokenValidationException with {@code INSUFFICIENT_ROLE} when the caller holds no role
     *         this API recognises
     */
    public void assertAuthorized(final ValidatedCaller caller) throws TokenValidationException {
        if (KNOWN_ROLES.stream().noneMatch(caller::hasRole)) {
            throw new TokenValidationException(TokenValidationException.Reason.INSUFFICIENT_ROLE);
        }
    }

    /**
     * Strips a single trailing slash. Traversal and encoding are already normalised by the servlet
     * container before {@code getRequestURI()} reaches here.
     */
    private static String stripTrailingSlash(final String requestUri) {
        final String trimmed = requestUri == null ? "" : requestUri.trim();
        return trimmed.length() > 1 && trimmed.endsWith("/")
                ? trimmed.substring(0, trimmed.length() - 1)
                : trimmed;
    }
}
