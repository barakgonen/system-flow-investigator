package com.example.investigator.ingestion;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IngestionSourceStatusTests {

    @Test
    void shouldExposeAllFieldsAndSupportEquality() {
        Instant lastEventAt = Instant.parse("2026-04-27T10:00:00Z");

        IngestionSourceStatus one = new IngestionSourceStatus(
                "mqtt",
                "MQTT Broker",
                "MQTT",
                true,
                List.of("lab/flow/in"),
                lastEventAt,
                "Connected"
        );

        IngestionSourceStatus two = new IngestionSourceStatus(
                "mqtt",
                "MQTT Broker",
                "MQTT",
                true,
                List.of("lab/flow/in"),
                lastEventAt,
                "Connected"
        );

        assertThat(one.id()).isEqualTo("mqtt");
        assertThat(one.displayName()).isEqualTo("MQTT Broker");
        assertThat(one.protocol()).isEqualTo("MQTT");
        assertThat(one.connected()).isTrue();
        assertThat(one.observedChannels()).containsExactly("lab/flow/in");
        assertThat(one.lastEventAt()).isEqualTo(lastEventAt);
        assertThat(one.message()).isEqualTo("Connected");

        assertThat(one).isEqualTo(two);
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one.toString()).contains("mqtt", "MQTT Broker");
    }
}