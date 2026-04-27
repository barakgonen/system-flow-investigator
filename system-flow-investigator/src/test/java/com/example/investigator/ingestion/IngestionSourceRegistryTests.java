package com.example.investigator.ingestion;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class IngestionSourceRegistryTests {

    @Test
    void shouldRegisterAndReturnSourcesSortedById() {
        IngestionSourceRegistry registry = new IngestionSourceRegistry();

        registry.register(source("websocket", "WS"));
        registry.register(source("mqtt", "MQTT"));

        assertThat(registry.sources())
                .extracting(IngestionSource::id)
                .containsExactly("mqtt", "websocket");
    }

    @Test
    void shouldReturnSourceById() {
        IngestionSourceRegistry registry = new IngestionSourceRegistry();
        IngestionSource<?, ?> mqtt = source("mqtt", "MQTT");

        registry.register(mqtt);

        assertThat(registry.get("mqtt")).isSameAs(mqtt);
    }

    @Test
    void shouldThrowWhenSourceDoesNotExist() {
        IngestionSourceRegistry registry = new IngestionSourceRegistry();

        assertThatThrownBy(() -> registry.get("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown ingestion source: missing");
    }

    @Test
    void shouldReturnStatuses() {
        IngestionSourceRegistry registry = new IngestionSourceRegistry();

        registry.register(source("mqtt", "MQTT"));
        registry.register(source("websocket", "WS"));

        assertThat(registry.statuses())
                .extracting(IngestionSourceStatus::id)
                .containsExactly("mqtt", "websocket");
    }

    @Test
    void shouldReplaceSourceWithSameId() {
        IngestionSourceRegistry registry = new IngestionSourceRegistry();

        IngestionSource<?, ?> first = source("mqtt", "MQTT");
        IngestionSource<?, ?> replacement = source("mqtt", "MQTT-NEW");

        registry.register(first);
        registry.register(replacement);

        assertThat(registry.sources()).hasSize(1);
        assertThat(registry.get("mqtt")).isSameAs(replacement);
        assertThat(registry.statuses().get(0).protocol()).isEqualTo("MQTT-NEW");
    }

    private IngestionSource<String, String> source(String id, String protocol) {
        return new IngestionSource<>() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String displayName() {
                return id + " source";
            }

            @Override
            public String protocol() {
                return protocol;
            }

            @Override
            public void connect(String connectRequest) {
            }

            @Override
            public void subscribe(String subscribeRequest) {
            }

            @Override
            public void disconnect() {
            }

            @Override
            public IngestionSourceStatus status() {
                return new IngestionSourceStatus(
                        id,
                        displayName(),
                        protocol,
                        true,
                        List.of("channel-1"),
                        Instant.parse("2026-04-27T10:00:00Z"),
                        "ok"
                );
            }

            @Override
            public List<String> observedChannels() {
                return List.of("channel-1");
            }
        };
    }
}