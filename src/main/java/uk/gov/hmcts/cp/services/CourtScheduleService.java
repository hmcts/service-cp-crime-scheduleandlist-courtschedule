package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.clients.CourtScheduleClient;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.filters.HearingResponseFilter;
import uk.gov.hmcts.cp.mappers.HearingsMapper;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;


@Service
@RequiredArgsConstructor
@Slf4j
public class CourtScheduleService {
    private final CourtScheduleClient courtScheduleClient;
    private final HearingsMapper hearingsMapper;
    private final HearingResponseFilter hearingResponseFilter;

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) throws ResponseStatusException {
        validateOrThrowError(caseId, HttpStatus.BAD_REQUEST, "caseId is mandatory");
        final HearingResponse hearingResponse = courtScheduleClient.getHearingResponse(caseId);
        validateOrThrowError(hearingResponse.getHearings(), HttpStatus.NOT_FOUND, "hearing response should not be empty");
        return createCourtScheduleResponse(hearingResponse);
    }

    private void validateOrThrowError(final Object obj, final HttpStatus status, final String errorMessage) {
        if (ObjectUtils.isEmpty(obj)) {
            log.error(errorMessage);
            throw new ResponseStatusException(status, errorMessage);
        }
    }

    private CourtScheduleResponse createCourtScheduleResponse(final HearingResponse hearingResponse) {
        final HearingResponse filteredResponse = hearingResponseFilter.filterHearingResponse(hearingResponse);
        return hearingsMapper.mapCommonPlatformResponse(filteredResponse);
    }
}