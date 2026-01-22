package uk.gov.hmcts.cp.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.cp.httpclients.CourtScheduleClientImpl;
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

@ExtendWith(MockitoExtension.class)
class CourtScheduleClientImplTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private final String mockUrl = "http://mock-server";
    private final String mockPath = "/some/mapper";
    private final String cjscppuid = "mock-cjscppuid";

    private CourtScheduleClientImpl createClient() {
        return new TestCourtScheduleClientImpl(mockHttpClient, mockUrl, mockPath, cjscppuid);
    }

    private static class TestCourtScheduleClientImpl extends CourtScheduleClientImpl {
        private final String testUrl;
        private final String testPath;
        private final String testCjscppuid;

        TestCourtScheduleClientImpl(HttpClient httpClient, String url, String path, String cjscppuid) {
            super(httpClient);
            this.testUrl = url;
            this.testPath = path;
            this.testCjscppuid = cjscppuid;
        }

        @Override
        public String getCourtScheduleClientUrl() {
            return testUrl;
        }

        @Override
        public String getCourtScheduleClientPath() {
            return testPath;
        }

        @Override
        public String getCjscppuid() {
            return testCjscppuid;
        }
    }

    private void setupMockHttpClient() throws Exception {
        when(mockHttpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(mockResponse);
    }

    @Test
    void getCourtScheduleByCaseId_shouldReturnValidResponse() throws Exception {
        setupMockHttpClient();
        CourtScheduleClientImpl client = createClient();

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
        setupMockHttpClient();
        CourtScheduleClientImpl client = createClient();

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