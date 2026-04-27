package com.example.investigator.pipeline;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.Mockito.*;

class ObservedEventPipelineTests {

    @Test
    void shouldStoreAndPublishEvent() {
        RecentEventStore store = mock(RecentEventStore.class);
        EventHub eventHub = mock(EventHub.class);

        ObservedEventPipeline pipeline = new ObservedEventPipeline(store, eventHub);

        ObservedEvent event = new ObservedEvent(
                "MQTT",
                "localhost:1883",
                "lab/flow/in",
                Instant.parse("2026-04-27T10:00:00Z"),
                "{\"traceId\":\"trace-1\"}",
                Map.of("qos", "1"),
                "trace-1",
                Instant.parse("2026-04-27T09:59:59Z")
        );

        pipeline.publish(event);

        verify(store).add(event);
        verify(eventHub).publish(event);
    }
}