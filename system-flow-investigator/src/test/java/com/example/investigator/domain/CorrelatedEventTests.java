package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelatedEventTests {

    @Test
    void shouldExposeAllFields() {
        Instant sourceSentAt = Instant.parse("2026-04-24T10:00:00Z");
        Instant observedAt = Instant.parse("2026-04-24T10:00:01Z");

        CorrelatedEvent event = new CorrelatedEvent(
                2,
                "WS",
                "lab-ws",
                "ws/live/out",
                sourceSentAt,
                observedAt,
                100L,
                200L,
                "{\"message\":\"hello\"}",
                Map.of("k", "v")
        );

        assertThat(event.index()).isEqualTo(2);
        assertThat(event.protocol()).isEqualTo("WS");
        assertThat(event.source()).isEqualTo("lab-ws");
        assertThat(event.channel()).isEqualTo("ws/live/out");
        assertThat(event.sourceSentAt()).isEqualTo(sourceSentAt);
        assertThat(event.observedAt()).isEqualTo(observedAt);
        assertThat(event.deltaFromPreviousSourceMs()).isEqualTo(100L);
        assertThat(event.deltaFromPreviousObservedMs()).isEqualTo(200L);
        assertThat(event.payload()).contains("hello");
        assertThat(event.metadata()).containsEntry("k", "v");
    }

    @Test
    void shouldSupportEqualityAndHashCode() {
        CorrelatedEvent one = new CorrelatedEvent(
                1,
                "MQTT",
                "source",
                "lab/flow/in",
                null,
                null,
                null,
                null,
                "payload",
                Map.of()
        );

        CorrelatedEvent two = new CorrelatedEvent(
                1,
                "MQTT",
                "source",
                "lab/flow/in",
                null,
                null,
                null,
                null,
                "payload",
                Map.of()
        );

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one.toString()).contains("MQTT", "lab/flow/in");
    }
}