package uk.gov.hmcts.cp.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.openapi.api.CourtScheduleApi;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschema;
import uk.gov.hmcts.cp.services.OpenApiService;

@RestController
@RequiredArgsConstructor
public class OpenApiController implements CourtScheduleApi {

    private final OpenApiService openApiService;

    @Override
    public ResponseEntity<CourtScheduleschema> getCourtScheduleByCaseUrn(String caseUrn) {
        CourtScheduleschema courtScheduleschema = openApiService.getCourtScheduleschema(caseUrn);
        return new ResponseEntity<>(courtScheduleschema, HttpStatus.OK);
    }

}
