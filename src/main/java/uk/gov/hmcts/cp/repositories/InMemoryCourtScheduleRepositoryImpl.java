package uk.gov.hmcts.cp.repositories;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class InMemoryCourtScheduleRepositoryImpl implements CourtScheduleRepository {

    public CourtScheduleResponse getCourtScheduleByCaseUrn(final String caseUrn) {
        final Hearing courtScheduleHearing = Hearing.builder()
                .hearingId(UUID.randomUUID().toString())
                .listNote("Requires interpreter")
                .hearingDescription("Sentencing for theft case")
                .hearingType("Trial")
                .courtSittings(List.of(
                        CourtSitting.builder()
                                .courtHouse("Central Criminal Court")
                                .sittingStart(OffsetDateTime.now())
                                .sittingEnd(OffsetDateTime.now().plusMinutes(60))
                                .judiciaryId(UUID.randomUUID().toString())
                                .build())
                ).build();

        return CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                                CourtSchedule.builder()
                                        .hearings(List.of(courtScheduleHearing)
                                        ).build()
                        )
                ).build();
    }

}
