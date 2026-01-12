package uk.gov.hmcts.cp;

import ch.qos.logback.classic.AsyncAppender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import uk.gov.hmcts.cp.config.IntegrationTestConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"management.tracing.enabled=true"})
@Import(IntegrationTestConfig.class)
@Slf4j
class SpringLoggingIntegrationTest {

    private PrintStream originalStdOut = System.out;

    @AfterEach
    void afterEach() {
        System.setOut(originalStdOut);
    }

    @Test
    void springbootTestShouldLogCorrectFields() throws IOException {
        MDC.put("any-mdc-field", "1234-1234");
        final ByteArrayOutputStream capturedStdOut = captureStdOut();
        log.info("spring boot test message");

        AsyncAppender asyncAppender = (AsyncAppender) ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("ROOT"))
                .getAppender("ASYNC_JSON");
        asyncAppender.stop();

        Map<String, Object> capturedFields = new ObjectMapper().readValue(capturedStdOut.toString(), new TypeReference<>() {
        });

        assertThat(capturedFields.get("any-mdc-field")).isEqualTo("1234-1234");
        assertThat((String) capturedFields.get("message")).contains("spring boot test message");
        assertThat(capturedFields.get("logger_name")).isEqualTo(this.getClass().getName());
        assertThat(capturedFields.get("thread_name")).isNotNull();
        assertThat(capturedFields.get("level")).isEqualTo("INFO");
        assertThat(capturedFields.get("timestamp")).isNotNull();
    }

    private ByteArrayOutputStream captureStdOut() {
        final ByteArrayOutputStream capturedStdOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedStdOut));
        return capturedStdOut;
    }
}
