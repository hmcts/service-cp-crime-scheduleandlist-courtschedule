package uk.gov.hmcts.cp;

import ch.qos.logback.classic.AsyncAppender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
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
    void springboot_test_should_log_correct_fields_including_exception() throws IOException {
        MDC.put("any-mdc-field", "1234-1234");
        ByteArrayOutputStream capturedStdOut = captureStdOut();
        log.info("spring boot test message", new RuntimeException("MyException"));
        captureAsyncLogs();

        String logMessage = capturedStdOut.toString();
        AssertionsForClassTypes.assertThat(logMessage).isNotEmpty();
        Map<String, Object> capturedFields = new ObjectMapper().readValue(logMessage, new TypeReference<>() {
        });
        AssertionsForClassTypes.assertThat(capturedFields.get("any-mdc-field")).isEqualTo("1234-1234");
        AssertionsForClassTypes.assertThat(capturedFields.get("timestamp")).isNotNull();
        AssertionsForClassTypes.assertThat(capturedFields.get("logger_name")).isEqualTo("uk.gov.hmcts.cp.SpringLoggingIntegrationTest");
        AssertionsForClassTypes.assertThat(capturedFields.get("thread_name")).isEqualTo("Test worker");
        AssertionsForClassTypes.assertThat(capturedFields.get("level")).isEqualTo("INFO");
        AssertionsForClassTypes.assertThat(capturedFields.get("message").toString()).contains("spring boot test message\njava.lang.RuntimeException: MyException");
        AssertionsForClassTypes.assertThat(capturedFields.get("message").toString()).contains("at uk.gov.hmcts.cp.SpringLoggingIntegrationTest");

    }

    private ByteArrayOutputStream captureStdOut() {
        final ByteArrayOutputStream capturedStdOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedStdOut));
        return capturedStdOut;
    }

    private void captureAsyncLogs() {
        AsyncAppender asyncAppender = (AsyncAppender) ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger("ROOT"))
                .getAppender("ASYNC_JSON");
        asyncAppender.stop();
    }
}
