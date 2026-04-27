package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.storage.RecentEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventExportServiceTests {

    private RecentEventStore store;
    private EventExportService service;

    @BeforeEach
    void setUp() {
        store = new RecentEventStore();
        service = new EventExportService(store);

        store.add(event(
                "MQTT",
                "localhost:1883",
                "lab/flow/in",
                "trace-1",
                "2026-04-27T04:49:32Z"
        ));

        store.add(event(
                "MQTT",
                "localhost:1883",
                "lab/flow/out",
                "trace-1",
                "2026-04-27T04:49:33Z"
        ));

        store.add(event(
                "WS",
                "web",
                "ws/live/out",
                "trace-2",
                "2026-04-27T04:49:34Z"
        ));
    }

    @Test
    void shouldExportAllEventsAsNdjson() {
        String ndjson = service.exportNdjson(emptyQuery());

        List<String> lines = ndjson.lines().toList();

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).contains("\"protocol\":\"MQTT\"");
        assertThat(lines.get(1)).contains("\"channel\":\"lab/flow/out\"");
        assertThat(lines.get(2)).contains("\"protocol\":\"WS\"");
    }

    @Test
    void shouldEndEachJsonObjectWithNewLine() {
        String ndjson = service.exportNdjson(emptyQuery());

        assertThat(ndjson).endsWith("\n");
    }

    @Test
    void shouldReturnEmptyStringWhenNoEventsMatch() {
        String ndjson = service.exportNdjson(new EventExportQuery(
                "missing/topic",
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(ndjson).isEmpty();
    }

    @Test
    void shouldFilterByTraceId() {
        List<ObservedEvent> events = service.exportEvents(new EventExportQuery(
                null,
                "trace-1",
                null,
                null,
                null,
                null
        ));

        assertThat(events).hasSize(2);
        assertThat(events).allMatch(event -> "trace-1".equals(event.traceId()));
    }

    @Test
    void shouldFilterByChannel() {
        List<ObservedEvent> events = service.exportEvents(new EventExportQuery(
                "lab/flow/out",
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).channel()).isEqualTo("lab/flow/out");
    }

    @Test
    void shouldFilterByProtocolIgnoringCase() {
        List<ObservedEvent> events = service.exportEvents(new EventExportQuery(
                null,
                null,
                "mqtt",
                null,
                null,
                null
        ));

        assertThat(events).hasSize(2);
        assertThat(events).allMatch(event -> "MQTT".equals(event.protocol()));
    }

    @Test
    void shouldFilterBySource() {
        List<ObservedEvent> events = service.exportEvents(new EventExportQuery(
                null,
                null,
                null,
                "web",
                null,
                null
        ));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).source()).isEqualTo("web");
    }

    @Test
    void shouldFilterByFromTimeInclusive() {
        List<ObservedEvent> events = service.exportEvents(new EventExportQuery(
                null,
                null,
                null,
                null,
                Instant.parse("2026-04-27T04:49:33Z"),
                null
        ));

        assertThat(events).hasSize(2);
        assertThat(events).extracting(ObservedEvent::channel)
                .containsExactly("lab/flow/out", "ws/live/out");
    }

    @Test
    void shouldFilterByToTimeInclusive() {
        List<ObservedEvent> events = service.exportEvents(new EventExportQuery(
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-04-27T04:49:33Z")
        ));

        assertThat(events).hasSize(2);
        assertThat(events).extracting(ObservedEvent::channel)
                .containsExactly("lab/flow/in", "lab/flow/out");
    }

    @Test
    void shouldFilterByTimeRangeInclusive() {
        List<ObservedEvent> events = service.exportEvents(new EventExportQuery(
                null,
                null,
                null,
                null,
                Instant.parse("2026-04-27T04:49:33Z"),
                Instant.parse("2026-04-27T04:49:34Z")
        ));

        assertThat(events).hasSize(2);
        assertThat(events).extracting(ObservedEvent::channel)
                .containsExactly("lab/flow/out", "ws/live/out");
    }

    @Test
    void shouldSupportNullQueryAsExportAll() {
        List<ObservedEvent> events = service.exportEvents(null);

        assertThat(events).hasSize(3);
    }

    @Test
    void shouldExportIsoInstantValues() {
        String ndjson = service.exportNdjson(new EventExportQuery(
                "lab/flow/in",
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(ndjson).contains("\"observedAt\":\"2026-04-27T04:49:32Z\"");
        assertThat(ndjson).contains("\"sourceSentAt\":\"2026-04-27T04:49:31Z\"");
    }

    private EventExportQuery emptyQuery() {
        return new EventExportQuery(
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private ObservedEvent event(String protocol,
                                String source,
                                String channel,
                                String traceId,
                                String observedAt) {
        Instant observed = Instant.parse(observedAt);
        Instant sourceSent = observed.minusSeconds(1);

        return new ObservedEvent(
                protocol,
                source,
                channel,
                observed,
                "{\"traceId\":\"%s\",\"message\":\"hello\"}".formatted(traceId),
                Map.of("test", "true"),
                traceId,
                sourceSent
        );
    }
}