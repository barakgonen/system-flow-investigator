package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.storage.RecentEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationServiceTests {

    private RecentEventStore store;
    private CorrelationService service;

    @BeforeEach
    void setUp() {
        store = new RecentEventStore();
        service = new CorrelationService(store);
    }

    @Test
    void shouldReturnEmptyTimelineForUnknownTraceId() {
        TraceTimelineResponse response = service.trace("missing");

        assertThat(response.traceId()).isEqualTo("missing");
        assertThat(response.eventCount()).isZero();
        assertThat(response.events()).isEmpty();
        assertThat(response.startedAt()).isNull();
        assertThat(response.lastObservedAt()).isNull();
        assertThat(response.totalSourceDurationMs()).isNull();
        assertThat(response.totalObservedDurationMs()).isNull();
    }

    @Test
    void shouldReturnOrderedTimelineAndCalculatedDeltas() {
        store.add(event("MQTT", "lab/flow/out", "trace-1",
                "2026-04-24T10:00:06Z",
                "2026-04-24T10:00:08Z"));

        store.add(event("MQTT", "lab/flow/in", "trace-1",
                "2026-04-24T10:00:00Z",
                "2026-04-24T10:00:01Z"));

        store.add(event("WS", "ws/live/out", "trace-1",
                "2026-04-24T10:00:10Z",
                "2026-04-24T10:00:13Z"));

        TraceTimelineResponse response = service.trace("trace-1");

        assertThat(response.traceId()).isEqualTo("trace-1");
        assertThat(response.eventCount()).isEqualTo(3);
        assertThat(response.startedAt()).isEqualTo(Instant.parse("2026-04-24T10:00:00Z"));
        assertThat(response.lastObservedAt()).isEqualTo(Instant.parse("2026-04-24T10:00:13Z"));
        assertThat(response.totalSourceDurationMs()).isEqualTo(10_000);
        assertThat(response.totalObservedDurationMs()).isEqualTo(12_000);

        assertThat(response.events()).extracting("channel")
                .containsExactly("lab/flow/in", "lab/flow/out", "ws/live/out");

        assertThat(response.events().get(0).index()).isEqualTo(1);
        assertThat(response.events().get(0).deltaFromPreviousSourceMs()).isNull();
        assertThat(response.events().get(0).deltaFromPreviousObservedMs()).isNull();

        assertThat(response.events().get(1).deltaFromPreviousSourceMs()).isEqualTo(6_000);
        assertThat(response.events().get(1).deltaFromPreviousObservedMs()).isEqualTo(7_000);

        assertThat(response.events().get(2).deltaFromPreviousSourceMs()).isEqualTo(4_000);
        assertThat(response.events().get(2).deltaFromPreviousObservedMs()).isEqualTo(5_000);
    }

    @Test
    void shouldFallbackToObservedAtWhenSourceSentAtIsMissing() {
        store.add(event("MQTT", "lab/flow/in", "trace-1",
                null,
                "2026-04-24T10:00:01Z"));

        store.add(event("MQTT", "lab/flow/out", "trace-1",
                null,
                "2026-04-24T10:00:04Z"));

        TraceTimelineResponse response = service.trace("trace-1");

        assertThat(response.events()).hasSize(2);
        assertThat(response.events().get(1).deltaFromPreviousSourceMs()).isEqualTo(3_000);
        assertThat(response.events().get(1).deltaFromPreviousObservedMs()).isEqualTo(3_000);
        assertThat(response.totalSourceDurationMs()).isEqualTo(3_000);
        assertThat(response.totalObservedDurationMs()).isEqualTo(3_000);
    }

    @Test
    void shouldReturnOnlyEventsMatchingTraceId() {
        store.add(event("MQTT", "lab/flow/in", "trace-target",
                "2026-04-24T10:00:00Z",
                "2026-04-24T10:00:01Z"));

        store.add(event("MQTT", "lab/flow/out", "trace-other",
                "2026-04-24T10:00:02Z",
                "2026-04-24T10:00:03Z"));

        TraceTimelineResponse response = service.trace("trace-target");

        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).channel()).isEqualTo("lab/flow/in");
    }

    @Test
    void shouldHandleSingleEventTrace() {
        store.add(event("MQTT", "lab/flow/in", "trace-1",
                "2026-04-24T10:00:00Z",
                "2026-04-24T10:00:01Z"));

        TraceTimelineResponse response = service.trace("trace-1");

        assertThat(response.eventCount()).isEqualTo(1);
        assertThat(response.totalSourceDurationMs()).isEqualTo(0);
        assertThat(response.totalObservedDurationMs()).isEqualTo(0);
        assertThat(response.events().get(0).deltaFromPreviousSourceMs()).isNull();
        assertThat(response.events().get(0).deltaFromPreviousObservedMs()).isNull();
    }

    private ObservedEvent event(String protocol,
                                String channel,
                                String traceId,
                                String sourceSentAt,
                                String observedAt) {
        return new ObservedEvent(
                protocol,
                "source",
                channel,
                Instant.parse(observedAt),
                "{\"traceId\":\"%s\"}".formatted(traceId),
                Map.of("test", "true"),
                traceId,
                sourceSentAt == null ? null : Instant.parse(sourceSentAt)
        );
    }
}