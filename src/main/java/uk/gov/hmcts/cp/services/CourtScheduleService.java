package uk.gov.hmcts.cp.services;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponseCourtScheduleInner;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponseCourtScheduleInnerHearingsInner;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponseCourtScheduleInnerHearingsInnerCourtSittingsInner;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CourtScheduleService {

    private static final Logger log = LoggerFactory.getLogger(CourtScheduleService.class);

    public CourtScheduleResponse getCourtScheduleResponse(String caseUrn) throws ResponseStatusException {
        if (StringUtils.isEmpty(caseUrn)) {
            log.warn("No case urn provided");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "caseUrn is required");
        }
        log.warn("NOTE: System configured to return stubbed Court Schedule details. Ignoring provided caseUrn : {}", caseUrn);
        CourtScheduleResponse stubbedCourtScheduleResponse = CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                                CourtScheduleResponseCourtScheduleInner.builder()
                                        .hearings(List.of(
                                                        CourtScheduleResponseCourtScheduleInnerHearingsInner.builder()
                                                                .hearingId(UUID.randomUUID().toString())
                                                                .listNote("Requires interpreter")
                                                                .hearingDescription("Sentencing for theft case")
                                                                .hearingType("Trial")
                                                                .courtSittings(List.of(
                                                                        CourtScheduleResponseCourtScheduleInnerHearingsInnerCourtSittingsInner.builder()
                                                                                .courtHouse("Central Criminal Court")
                                                                                .sittingStart(OffsetDateTime.now())
                                                                                .sittingEnd(OffsetDateTime.now().plusMinutes(60))
                                                                                .judiciaryId(UUID.randomUUID().toString())
                                                                                .build())
                                                                ).build()
                                                )
                                        ).build()
                        )
                ).build();
        log.debug("Court Schedule response: {}", stubbedCourtScheduleResponse);
        return stubbedCourtScheduleResponse;
    }
}
