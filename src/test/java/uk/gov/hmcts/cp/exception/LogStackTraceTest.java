package uk.gov.hmcts.cp.exception;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class LogStackTraceTest {

    @Test
    void exception_should_appear_with_stacktrace() {
        Logger logger = (Logger) LoggerFactory.getLogger(LogStackTraceTest.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        Exception exception = new RuntimeException("Stack trace error");
        logger.error("Error should display exception when appended.", exception);

        ILoggingEvent event = listAppender.list.get(0);

        assertThat(event.getFormattedMessage()).contains("Error should display exception when appended.");
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(RuntimeException.class.getName());
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("Stack trace error");
        assertThat(event.getThrowableProxy().getStackTraceElementProxyArray()).isNotEmpty();

        // The below one is same as above.
        String messageWithStackTrace =
                event.getFormattedMessage() + "\n" +
                        event.getThrowableProxy().getClassName() + ": " +
                        event.getThrowableProxy().getMessage() + "\n" +
                        java.util.Arrays.stream(
                                        event.getThrowableProxy().getStackTraceElementProxyArray()
                                ).map(Object::toString)
                                .reduce("", (a, b) -> a + "\n" + b);

        assertThat(messageWithStackTrace)
                .contains("Error should display exception when appended.")
                .contains("java.lang.RuntimeException")
                .contains("Stack trace error")
                .contains("at uk.gov.hmcts.cp.exception.LogStackTraceTest");

    }

}
