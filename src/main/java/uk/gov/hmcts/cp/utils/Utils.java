package uk.gov.hmcts.cp.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.security.*;
import java.security.cert.X509Certificate;

@Slf4j
public class Utils {
    public static String sanitizeString(final String urn) {
        if (urn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "caseUrn is required");
        }
        return StringEscapeUtils.escapeHtml4(urn);
    }

    public static void ignoreCertificates() {
        final TrustManager[] trustAllCerts = {new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (GeneralSecurityException e) {
            log.error("Error while ignoring SSL certificate", e);
        }
    }

    public static HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        final TrustManager[] trustAllCerts = {
                new X509TrustManager() {
                    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

}
