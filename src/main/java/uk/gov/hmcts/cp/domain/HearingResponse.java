package uk.gov.hmcts.cp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HearingResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<HearingSchedule> hearings;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class HearingSchedule implements Serializable {
        private static final long serialVersionUID = 2L;

        private String id;
        private HearingType type;
        private boolean allocated;
        private List<Judiciary> judiciary;
        private List<HearingDay> hearingDays;
        private int weekCommencingDurationInWeeks;

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        public static class HearingType implements Serializable {
            private static final long serialVersionUID = 3L;

            private String description;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        public static class Judiciary implements Serializable {
            private static final long serialVersionUID = 4L;

            private String judicialId;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        public static class HearingDay implements Serializable {
            private static final long serialVersionUID = 5L;

            private String endTime;
            private int sequence;
            private String startTime;
            private String courtRoomId;
            private String hearingDate;
            private String courtCentreId;
            private int durationMinutes;
        }
    }

}
