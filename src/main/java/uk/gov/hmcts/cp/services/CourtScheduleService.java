package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.repositories.CourtScheduleClient;
import org.owasp.encoder.Encode;


@Service
@RequiredArgsConstructor
@Slf4j
public class CourtScheduleService {
    private final CourtScheduleClient courtScheduleClient;

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) throws ResponseStatusException {
        if (StringUtils.isEmpty(caseId)) {
            log.warn("No case Id provided");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "caseId is required");
        }
        log.warn("NOTE: System configured to return stubbed Court Schedule details. Ignoring provided caseId : {}", Encode.forJava(caseId));
        return courtScheduleClient.getCourtScheduleByCaseId(caseId);
    }

}