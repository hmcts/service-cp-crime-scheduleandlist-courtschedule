package uk.gov.hmcts.cp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class AppPropertiesBackend {

    private final String caseMapperUrl;
    private final String caseMapperPath;
    private final String hearingsUrl;
    private final String hearingsPath;
    private final String hearingsCjscppuid;

    public AppPropertiesBackend(
            @Value("${case-mapper-client.url}") final String caseMapperUrl,
            @Value("${case-mapper-client.path}") final String caseMapperPath,
            @Value("${court-schedule-client.url}") final String hearingsUrl,
            @Value("${court-schedule-client.path}") final String hearingsPath,
            @Value("${court-schedule-client.cjscppuid}") final String hearingsCjscppuid) {
        this.caseMapperUrl = caseMapperUrl;
        this.caseMapperPath = caseMapperPath;
        this.hearingsUrl = hearingsUrl;
        this.hearingsPath = hearingsPath;
        this.hearingsCjscppuid = hearingsCjscppuid;
    }
}
