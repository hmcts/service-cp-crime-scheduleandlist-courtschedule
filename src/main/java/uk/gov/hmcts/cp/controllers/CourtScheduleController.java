package uk.gov.hmcts.cp.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.cp.openapi.api.CourtScheduleApi;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.services.CourtScheduleService;

@RestController
public class CourtScheduleController implements CourtScheduleApi {
    private static final Logger log = LoggerFactory.getLogger(CourtScheduleController.class);
    private final CourtScheduleService courtScheduleService;

    public CourtScheduleController(CourtScheduleService courtScheduleService) {
        this.courtScheduleService = courtScheduleService;
    }

    @Override
    public ResponseEntity<CourtScheduleResponse> getCourtScheduleByCaseUrn(String caseUrn) {
        CourtScheduleResponse courtScheduleResponse;
        try {
            courtScheduleResponse = courtScheduleService.getCourtScheduleResponse(caseUrn);
        } catch (ResponseStatusException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).build();
        }
        log.debug("getCourtScheduleByCaseUrn: {}", caseUrn);
        return new ResponseEntity<>(courtScheduleResponse, HttpStatus.OK);
    }

}
