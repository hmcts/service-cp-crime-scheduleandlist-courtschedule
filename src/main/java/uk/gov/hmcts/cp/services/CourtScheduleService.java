package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.clients.CourtScheduleClient;
import uk.gov.hmcts.cp.mappers.HearingsMapper;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;


@Service
@RequiredArgsConstructor
@Slf4j
public class CourtScheduleService {
    private final CourtScheduleClient courtScheduleClient;
    private final HearingsMapper hearingsMapper;

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) throws ResponseStatusException {
        if (ObjectUtils.isEmpty(caseId)) {
            log.error("No case Id provided");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "caseId is required");
        }
        return hearingsMapper.mapCommonPlatformResponse(courtScheduleClient.getHearingResponse(caseId));
    }

}