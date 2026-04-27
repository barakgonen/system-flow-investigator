package com.example.investigator.api;

import com.example.investigator.ingestion.IngestionSourceRegistry;
import com.example.investigator.ingestion.IngestionSourceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IngestionSourceControllerTests {

    @Test
    void shouldReturnAllSourceStatuses() {
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        List<IngestionSourceStatus> statuses = List.of(
                new IngestionSourceStatus(
                        "mqtt",
                        "MQTT Broker",
                        "MQTT",
                        true,
                        List.of("lab/flow/in"),
                        null,
                        "Active / observed topics"
                )
        );

        when(registry.statuses()).thenReturn(statuses);

        IngestionSourceController controller = new IngestionSourceController(registry);

        assertThat(controller.sources()).isSameAs(statuses);
        verify(registry).statuses();
    }

    @Test
    void shouldReturnSingleSourceStatus() {
        IngestionSourceRegistry registry = mock(IngestionSourceRegistry.class);

        var source = mock(com.example.investigator.ingestion.IngestionSource.class);

        IngestionSourceStatus status = new IngestionSourceStatus(
                "websocket",
                "WebSocket",
                "WS",
                true,
                List.of("ws/live/out"),
                null,
                "Active / observed channels"
        );

        when(registry.get("websocket")).thenReturn(source);
        when(source.status()).thenReturn(status);

        IngestionSourceController controller = new IngestionSourceController(registry);

        assertThat(controller.source("websocket")).isSameAs(status);

        verify(registry).get("websocket");
        verify(source).status();
    }
}