package uk.gov.hmcts.cp.repositories;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component("inMemoryCourtScheduleClientImpl")
public class InMemoryCourtScheduleClientImpl implements CourtScheduleClient {

    public CourtScheduleResponse getCourtScheduleByCaseId(final String caseId) {

        final OffsetDateTime sittingStartTime = OffsetDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS);

        final Hearing hearing = Hearing.builder()
                .hearingId(UUID.randomUUID().toString())
                .listNote("Requires interpreter")
                .hearingDescription("Sentencing for theft case")
                .hearingType("Trial")
                .courtSittings(List.of(
                        CourtSitting.builder()
                                .courtHouse("Central Criminal Court")
                                .sittingStart(sittingStartTime)
                                .sittingEnd(sittingStartTime.plusMinutes(60))
                                .judiciaryId(UUID.randomUUID().toString())
                                .build())
                ).build();

        return CourtScheduleResponse.builder()
                .courtSchedule(List.of(
                                CourtSchedule.builder()
                                        .hearings(List.of(hearing)
                                        ).build()
                        )
                ).build();
    }
}
