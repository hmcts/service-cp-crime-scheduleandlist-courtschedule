package uk.gov.hmcts.cp.services;

import static java.util.UUID.randomUUID;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschema;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschemaCourtScheduleInner;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschemaCourtScheduleInnerHearingsInner;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleschemaCourtScheduleInnerHearingsInnerCourtSittingsInner;

import java.time.OffsetDateTime;

import java.util.List;

@Service
public class CourtScheduleService {

    public CourtScheduleschema getCourtScheduleschema(String caseUrn) {

        CourtScheduleschemaCourtScheduleInnerHearingsInnerCourtSittingsInner courtSittingsItem =
            new CourtScheduleschemaCourtScheduleInnerHearingsInnerCourtSittingsInner();
        courtSittingsItem.courtHouse(randomUUID().toString());
        courtSittingsItem.sittingStart(OffsetDateTime.now());
        courtSittingsItem.setSittingEnd(OffsetDateTime.now().plusMinutes(30));
        courtSittingsItem.judiciaryId(randomUUID().toString());

        CourtScheduleschemaCourtScheduleInnerHearingsInner hearingsItem =
            new CourtScheduleschemaCourtScheduleInnerHearingsInner();
        hearingsItem.hearingId(randomUUID().toString());
        hearingsItem.listNote("Requires interpreter");
        hearingsItem.hearingDescription("Sentencing for theft case");
        hearingsItem.hearingType("Trial");
        hearingsItem.courtSittings(List.of(courtSittingsItem));

        CourtScheduleschemaCourtScheduleInner courtScheduleInner = new CourtScheduleschemaCourtScheduleInner();
        courtScheduleInner.addHearingsItem(hearingsItem);

        CourtScheduleschema courtScheduleschema = new CourtScheduleschema();
        courtScheduleschema.courtSchedule(List.of(courtScheduleInner));

        return courtScheduleschema;
    }
}
