package uk.gov.hmcts.cp.repositories;

import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;


@FunctionalInterface
public interface CourtScheduleClient {
    CourtScheduleResponse getCourtScheduleByCaseId(String caseId);
}
