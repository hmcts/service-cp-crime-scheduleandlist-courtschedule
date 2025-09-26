package uk.gov.hmcts.cp.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.api.CourtScheduleApi;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.services.CaseUrnMapperService;
import uk.gov.hmcts.cp.services.CourtScheduleService;

import java.util.stream.Collectors;

import static uk.gov.hmcts.cp.utils.Utils.sanitizeString;

@RestController
public class CourtScheduleController implements CourtScheduleApi {
    private static final Logger LOG = LoggerFactory.getLogger(CourtScheduleController.class);
    private final CourtScheduleService courtScheduleService;
    private final CaseUrnMapperService caseUrnMapperService;

    public CourtScheduleController(final CourtScheduleService courtScheduleService,
                                   final CaseUrnMapperService caseUrnMapperService) {
        this.courtScheduleService = courtScheduleService;
        this.caseUrnMapperService = caseUrnMapperService;
    }

    @Override
    public ResponseEntity<CourtScheduleResponse> getCourtScheduleByCaseUrn(final String caseUrn) {
        final String sanitizedCaseUrn;
        final CourtScheduleResponse courtScheduleResponse;
        try {
            sanitizedCaseUrn = sanitizeString(caseUrn);
            LOG.atInfo().log("Received request to get court schedule for caseUrn: {}", sanitizedCaseUrn);
            String caseId = caseUrnMapperService.getCaseId(sanitizedCaseUrn);
            courtScheduleResponse = courtScheduleService.getCourtScheduleByCaseId(caseId);
            LOG.atInfo().log("caseUrn : {} -> Court Schedule response: {}", caseUrn, courtScheduleResponse.getCourtSchedule().stream()
                    .flatMap(a -> a.getHearings().stream().map(b -> b.getHearingId())).collect(Collectors.joining(",")));
        } catch (ResponseStatusException e) {
            LOG.atError().log(e.getMessage());
            throw e;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(courtScheduleResponse);
    }
}
