package uk.gov.hmcts.cp.controllers;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.api.CourtScheduleApi;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.Hearing;
import uk.gov.hmcts.cp.services.CaseUrnMapperService;
import uk.gov.hmcts.cp.services.CourtScheduleService;

import java.util.stream.Collectors;

import static uk.gov.hmcts.cp.utils.Utils.sanitizeString;

@RestController
@Slf4j
public class CourtScheduleController implements CourtScheduleApi {
    private final CourtScheduleService courtScheduleService;
    private final CaseUrnMapperService caseUrnMapperService;

    public CourtScheduleController(final CourtScheduleService courtScheduleService,
                                   final CaseUrnMapperService caseUrnMapperService) {
        this.courtScheduleService = courtScheduleService;
        this.caseUrnMapperService = caseUrnMapperService;
    }

    @Override
    @NonNull
    public ResponseEntity<CourtScheduleResponse> getCourtScheduleByCaseUrn(final String caseUrn) {
        final String sanitizedCaseUrn;
        final CourtScheduleResponse courtScheduleResponse;
        try {
            sanitizedCaseUrn = sanitizeString(caseUrn);
            log.info("Received request to get court schedule for caseUrn: {}", sanitizedCaseUrn);
            final String caseId = caseUrnMapperService.getCaseId(sanitizedCaseUrn);
            courtScheduleResponse = courtScheduleService.getCourtScheduleByCaseId(caseId);
            log.info("caseUrn : {} -> Court Schedule Hearing Ids : {}", caseUrn, courtScheduleResponse.getCourtSchedule().stream()
                    .flatMap(a -> a.getHearings().stream().map(Hearing::getHearingId)).collect(Collectors.joining(",")));
        } catch (ResponseStatusException e) {
            log.error(e.getMessage());
            throw e;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(courtScheduleResponse);
    }
}
