package uk.gov.hmcts.cp.pact.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.CourtSchedule;
import uk.gov.hmcts.cp.openapi.model.Hearing;
import uk.gov.hmcts.cp.openapi.model.CourtSitting;
import uk.gov.hmcts.cp.repositories.CourtScheduleRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("test")  // important: use the in-memory repo!
@ExtendWith({SpringExtension.class, PactVerificationInvocationContextProvider.class})
@Provider("VPCourtSchedulePactProvider")
@PactBroker(
        scheme = "https",
        host = "${pact.broker.host}",
        authentication = @PactBrokerAuth(token = "${pact.broker.token}")
)
@Tag("pact")
public class CourtScheduleProviderPactTest {

    @Autowired
    private CourtScheduleRepository courtScheduleRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setupTarget(PactVerificationContext context) {
        System.out.println("Running test on port: " + port);
        context.setTarget(new HttpTestTarget("localhost", port));
        System.out.println("pact.verifier.publishResults: " + System.getProperty("pact.verifier.publishResults"));
    }

    @State("court schedule for case 456789 exists")
    public void setupCourtSchedule() {
        courtScheduleRepository.clearAll();
        var courtSitting = CourtSitting.builder()
                .courtHouse("Central Criminal Court")
                .sittingStart(OffsetDateTime.now())
                .sittingEnd(OffsetDateTime.now().plusMinutes(60))
                .judiciaryId(UUID.randomUUID().toString())
                .build();
        var hearing = Hearing.builder()
                .hearingId(UUID.randomUUID().toString())
                .listNote("Requires interpreter")
                .hearingDescription("Sentencing for theft case")
                .hearingType("Trial")
                .courtSittings(List.of(courtSitting))
                .build();

        var schedule = CourtSchedule.builder()
                .hearings(List.of(hearing))
                .build();

        var response = CourtScheduleResponse.builder()
                .courtSchedule(List.of(schedule))
                .build();

        courtScheduleRepository.saveCourtSchedule("456789", response);
    }

    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
