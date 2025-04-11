package uk.gov.hmcts.cp.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.openapi.api.CourtScheduleApi;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschema;
import uk.gov.hmcts.cp.services.CourtScheduleService;

@RestController
@RequiredArgsConstructor
public class CourtSceduleController implements CourtScheduleApi {

    private final CourtScheduleService courtScheduleService;

    @Override
    public ResponseEntity<CourtScheduleschema> getCourtScheduleByCaseUrn(String caseUrn) {
        CourtScheduleschema courtScheduleschema = courtScheduleService.getCourtScheduleschema(caseUrn);
        return new ResponseEntity<>(courtScheduleschema, HttpStatus.OK);
    }

}
