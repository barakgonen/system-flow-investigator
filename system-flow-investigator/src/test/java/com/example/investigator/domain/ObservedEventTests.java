package com.example.investigator.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ObservedEventTests {

    @Test
    void shouldExposeAllFields() {
        Instant observedAt = Instant.parse("2026-04-24T10:00:01Z");
        Instant sourceSentAt = Instant.parse("2026-04-24T10:00:00Z");

        ObservedEvent event = new ObservedEvent(
                "MQTT",
                "broker-1",
                "lab/flow/in",
                observedAt,
                "{\"traceId\":\"trace-1\"}",
                Map.of("qos", "1"),
                "trace-1",
                sourceSentAt
        );

        assertThat(event.protocol()).isEqualTo("MQTT");
        assertThat(event.source()).isEqualTo("broker-1");
        assertThat(event.channel()).isEqualTo("lab/flow/in");
        assertThat(event.observedAt()).isEqualTo(observedAt);
        assertThat(event.payload()).contains("trace-1");
        assertThat(event.metadata()).containsEntry("qos", "1");
        assertThat(event.traceId()).isEqualTo("trace-1");
        assertThat(event.sourceSentAt()).isEqualTo(sourceSentAt);
    }

    @Test
    void shouldSupportNullFields() {
        ObservedEvent event = new ObservedEvent(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(event.protocol()).isNull();
        assertThat(event.source()).isNull();
        assertThat(event.channel()).isNull();
        assertThat(event.observedAt()).isNull();
        assertThat(event.payload()).isNull();
        assertThat(event.metadata()).isNull();
        assertThat(event.traceId()).isNull();
        assertThat(event.sourceSentAt()).isNull();
    }

    @Test
    void shouldSupportEqualityHashCodeAndToString() {
        Instant observedAt = Instant.parse("2026-04-24T10:00:01Z");
        Instant sourceSentAt = Instant.parse("2026-04-24T10:00:00Z");

        ObservedEvent one = new ObservedEvent(
                "MQTT",
                "broker-1",
                "lab/flow/in",
                observedAt,
                "payload",
                Map.of(),
                "trace-1",
                sourceSentAt
        );

        ObservedEvent two = new ObservedEvent(
                "MQTT",
                "broker-1",
                "lab/flow/in",
                observedAt,
                "payload",
                Map.of(),
                "trace-1",
                sourceSentAt
        );

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one.toString()).contains("MQTT", "broker-1", "lab/flow/in", "trace-1");
    }
}