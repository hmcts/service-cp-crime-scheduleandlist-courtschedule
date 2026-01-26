package uk.gov.hmcts.cp.httpclients;

import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;


@FunctionalInterface
public interface CourtScheduleClient {
    CourtScheduleResponse getCourtScheduleByCaseId(String caseId);
}
