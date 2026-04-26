package com.example.investigator.domain;

import com.example.investigator.domain.config.FlowValidationResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraceTimelineResponseTests {

    @Test
    void shouldExposeAllFields() {
        CorrelatedEvent event = new CorrelatedEvent(
                1,
                "MQTT",
                "source",
                "lab/flow/in",
                Instant.parse("2026-04-24T10:00:00Z"),
                Instant.parse("2026-04-24T10:00:01Z"),
                null,
                null,
                "payload",
                Map.of()
        );

        FlowValidationResult validation = new FlowValidationResult(
                "COMPLETE",
                "Flow completed successfully.",
                List.of(),
                List.of(),
                List.of(),
                "lab/flow/in"
        );

        TraceTimelineResponse response = new TraceTimelineResponse(
                "trace-1",
                1,
                Instant.parse("2026-04-24T10:00:00Z"),
                Instant.parse("2026-04-24T10:00:01Z"),
                0L,
                0L,
                List.of(event),
                validation
        );

        assertThat(response.traceId()).isEqualTo("trace-1");
        assertThat(response.eventCount()).isEqualTo(1);
        assertThat(response.startedAt()).isEqualTo(Instant.parse("2026-04-24T10:00:00Z"));
        assertThat(response.lastObservedAt()).isEqualTo(Instant.parse("2026-04-24T10:00:01Z"));
        assertThat(response.totalSourceDurationMs()).isZero();
        assertThat(response.totalObservedDurationMs()).isZero();
        assertThat(response.events()).containsExactly(event);
        assertThat(response.validation()).isEqualTo(validation);
        assertThat(response.toString()).contains("trace-1", "COMPLETE");
    }

    @Test
    void shouldSupportEqualityAndHashCode() {
        FlowValidationResult validation = new FlowValidationResult(
                "COMPLETE",
                "Flow completed successfully.",
                List.of(),
                List.of(),
                List.of(),
                null
        );

        TraceTimelineResponse one = new TraceTimelineResponse(
                "trace-1",
                0,
                null,
                null,
                null,
                null,
                List.of(),
                validation
        );

        TraceTimelineResponse two = new TraceTimelineResponse(
                "trace-1",
                0,
                null,
                null,
                null,
                null,
                List.of(),
                validation
        );

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
    }

    @Test
    void shouldSupportNullValidation() {
        TraceTimelineResponse response = new TraceTimelineResponse(
                "trace-1",
                0,
                null,
                null,
                null,
                null,
                List.of(),
                null
        );

        assertThat(response.validation()).isNull();
    }
}