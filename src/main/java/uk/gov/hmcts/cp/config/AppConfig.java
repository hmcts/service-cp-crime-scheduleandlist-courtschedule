package uk.gov.hmcts.cp.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.auth.EntraAuthProperties;
import uk.gov.hmcts.cp.auth.EntraTokenValidator;
import uk.gov.hmcts.cp.services.ClockService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class AppConfig {

    private static final long JWKS_REFRESH_TIMEOUT_MS = Duration.ofSeconds(15).toMillis();
    private static final long JWKS_MIN_REFRESH_INTERVAL_MS = Duration.ofSeconds(30).toMillis();
    private static final long JWKS_OUTAGE_TTL_MS = Duration.ofHours(24).toMillis();

    @Value("${spring.application.name:service-cp-crime-scheduleandlist-courtschedule}")
    private String applicationName;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    public ClockService clockService() {
        return new ClockService(Clock.systemDefaultZone());
    }

    /**
     * Entra's published signing keys.
     *
     * <p>Built once at startup so there is no outbound call per request. Nimbus handles the three
     * things that matter operationally: the cache, a rate-limited refresh when an unknown {@code kid}
     * appears (Entra rotates keys without notice, and a service that cannot pick up a new key without
     * a redeploy will have an outage), and serving stale keys through a bounded Entra outage.
     */
    @Bean
    public JWKSource<SecurityContext> entraJwkSource(final EntraAuthProperties authProperties)
            throws MalformedURLException {
        return JWKSourceBuilder
                .create(URI.create(authProperties.getJwksUri()).toURL())
                .cache(Duration.ofSeconds(authProperties.getJwksCacheTtlSeconds()).toMillis(),
                        JWKS_REFRESH_TIMEOUT_MS)
                .rateLimited(JWKS_MIN_REFRESH_INTERVAL_MS)
                .outageTolerant(JWKS_OUTAGE_TTL_MS)
                .build();
    }

    @Bean
    public EntraTokenValidator entraTokenValidator(final EntraAuthProperties authProperties,
                                                   final JWKSource<SecurityContext> entraJwkSource) {
        return new EntraTokenValidator(authProperties, entraJwkSource);
    }
}
