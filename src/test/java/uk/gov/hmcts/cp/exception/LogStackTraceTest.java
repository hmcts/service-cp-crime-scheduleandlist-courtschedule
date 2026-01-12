package uk.gov.hmcts.cp.exception;


import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class LogStackTraceTest {
    private final PrintStream originalStdOut = System.out;

    @AfterEach
    void tearDown() {
        System.setOut(originalStdOut);
    }

    @Test
    void exception_should_appear_with_stacktrace() throws Exception {
        ByteArrayOutputStream capturedStdOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedStdOut));

        Exception exception = new RuntimeException("Stack trace error");
        log.error("Error should display exception when appended.", exception);
        flushAsyncAppender();
        Map<String, Object> loggedJson = new ObjectMapper().readValue(
                capturedStdOut.toString(),
                new TypeReference<>() {
                }
        );
        assertThat(loggedJson.get("message").toString())
                .contains("Error should display exception when appended.");
        assertThat(loggedJson.get("message").toString())
                .contains("RuntimeException")
                .contains("Stack trace error");
    }

    private void flushAsyncAppender() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        AsyncAppender asyncAppender =
                (AsyncAppender) root.getAppender("ASYNC_JSON");
        asyncAppender.stop();
    }
}
