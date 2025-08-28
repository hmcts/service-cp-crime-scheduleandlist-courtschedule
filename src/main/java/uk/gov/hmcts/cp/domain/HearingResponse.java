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

    private List<HearingResult> hearings;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class HearingResult implements Serializable {
        private String id;
        private Type type;
        private List<Judiciary> judiciary;
        private List<HearingDay> hearingDays;

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        public static class Type {
            private String description;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        public static class Judiciary {
            private String judicialId;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        public static class HearingDay {
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
