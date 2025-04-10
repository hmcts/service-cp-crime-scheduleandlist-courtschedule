package uk.gov.hmcts.cp.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschema;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschemaCourtScheduleInner;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschemaCourtScheduleInnerHearingsInner;

import java.util.List;

@Service
public class OpenApiService {

    public CourtScheduleschema getCourtScheduleschema(String caseUrn) {
        CourtScheduleschema courtScheduleschema = new CourtScheduleschema();
        CourtScheduleschemaCourtScheduleInner courtScheduleInner = new CourtScheduleschemaCourtScheduleInner();
        CourtScheduleschemaCourtScheduleInnerHearingsInner hearingsItem =
            new CourtScheduleschemaCourtScheduleInnerHearingsInner();
        hearingsItem.hearingId(caseUrn);
        courtScheduleInner.addHearingsItem(hearingsItem);
        courtScheduleschema.courtSchedule(List.of(courtScheduleInner));
        return courtScheduleschema;
    }
}
