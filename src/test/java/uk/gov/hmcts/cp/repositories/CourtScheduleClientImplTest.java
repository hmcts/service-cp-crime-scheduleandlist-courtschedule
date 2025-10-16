package uk.gov.hmcts.cp.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.cp.openapi.model.CourtScheduleResponse;
import uk.gov.hmcts.cp.openapi.model.Hearing;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


class CourtScheduleClientImplTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private CourtScheduleClientImpl client;

    private final String mockUrl = "http://mock-server";
    private final String mockPath = "/some/mapper";
    private final String cjscppuid = "mock-cjscppuid";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(mockHttpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(mockResponse);

        client = new CourtScheduleClientImpl(mockHttpClient) {

            @Override
            public String getCourtScheduleClientUrl() {
                return mockUrl;
            }

            @Override
            public String getCourtScheduleClientPath() {
                return mockPath;
            }

            @Override
            public String getCjscppuid() {
                return cjscppuid;
            }
        };
    }

    @Test
    void getCourtScheduleByCaseId_shouldReturnValidResponse() throws Exception {

        URL resource = getClass().getClassLoader().getResource("courtSchedule_allocated.json");
        Path path = Path.of(resource.toURI());
        String json = Files.readString(path);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);

        CourtScheduleResponse response = client.getCourtScheduleByCaseId("some-case-id");

        assertNotNull(response);
        assertEquals(1, response.getCourtSchedule().size());

        Hearing hearing = response.getCourtSchedule().get(0).getHearings().get(0);
        assertEquals("hearing-1", hearing.getHearingId());
        assertEquals("First hearing", hearing.getHearingType());
        assertEquals(1, hearing.getCourtSittings().size());
        assertEquals("judge-1", hearing.getCourtSittings().get(0).getJudiciaryId());
        assertEquals("court-1", hearing.getCourtSittings().get(0).getCourtHouse());
    }

    @Test
    void getCourtScheduleByCaseId_shouldReturnEmptyHearingSchedule() throws Exception {

        URL resource = getClass().getClassLoader().getResource("courtSchedule_unallocated.json");
        Path path = Path.of(resource.toURI());
        String json = Files.readString(path);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);

        CourtScheduleResponse response = client.getCourtScheduleByCaseId("some-case-id");
        assertNotNull(response);
        assertEquals(0, response.getCourtSchedule().get(0).getHearings().size());
    }
}