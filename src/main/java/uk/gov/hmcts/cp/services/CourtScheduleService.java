package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.repositories.CourtScheduleClient;

import static uk.gov.hmcts.cp.utils.Utils.sanitizeString;

@Service
@RequiredArgsConstructor
public class CourtScheduleService {

    private static final Logger LOG = LoggerFactory.getLogger(CourtScheduleService.class);
    private final CourtScheduleClient courtScheduleClient;

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) throws ResponseStatusException {
        if (StringUtils.isEmpty(caseId)) {
            LOG.atWarn().log("No case Id provided");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "caseId is required");
        }
        LOG.atWarn().log("NOTE: System configured to return stubbed Court Schedule details. Ignoring provided caseId : {}", sanitizeString(caseId));
        final CourtScheduleResponse stubbedCourtScheduleResponse = courtScheduleClient.getCourtScheduleByCaseId(caseId);
        LOG.atDebug().log("Court Schedule response: {}", stubbedCourtScheduleResponse);
        return stubbedCourtScheduleResponse;
    }

}
