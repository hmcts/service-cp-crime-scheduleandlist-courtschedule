package uk.gov.hmcts.cp.utils;

import lombok.extern.slf4j.Slf4j;
import lombok.experimental.UtilityClass;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.*;
import java.net.http.HttpClient;
import java.security.*;
import java.security.cert.X509Certificate;

@Slf4j
@UtilityClass
public class Utils {
    public static String sanitizeString(final String urn) {
        if (urn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "caseUrn is required");
        }
        return StringEscapeUtils.escapeHtml4(urn);
    }

}
