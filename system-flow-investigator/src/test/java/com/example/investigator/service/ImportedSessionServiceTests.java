package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.domain.config.FlowValidationResult;
import com.example.investigator.domain.config.FlowValidationRules;
import com.example.investigator.domain.config.InvestigationConfig;
import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.domain.export.SessionExportMetadata;
import com.example.investigator.domain.session.ImportedSession;
import com.example.investigator.storage.ImportedSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImportedSessionServiceTests {

    private ImportedSessionStore store;
    private FlowValidationService flowValidationService;
    private ImportedSessionService service;

    @BeforeEach
    void setUp() {
        store = new ImportedSessionStore();
        flowValidationService = mock(FlowValidationService.class);

        when(flowValidationService.validate(anyList(), isNull()))
                .thenReturn(null);

        when(flowValidationService.validate(anyList(), anyString()))
                .thenReturn(new FlowValidationResult(
                        "COMPLETE",
                        "Flow completed successfully.",
                        List.of(),
                        List.of(),
                        List.of(),
                        "ws/live/out"
                ));

        service = new ImportedSessionService(store, flowValidationService);
    }

    @Test
    void shouldListImportedSessions() {
        store.save(session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                List.of(event("trace-1", "lab/flow/in", "2026-04-27T04:49:32Z"))
        ));

        assertThat(service.listImportedSessions()).hasSize(1);
        assertThat(service.listImportedSessions().get(0).sessionId()).isEqualTo("session-1");
        assertThat(service.listImportedSessions().get(0).eventCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnEventsForSession() {
        ObservedEvent event = event("trace-1", "lab/flow/in", "2026-04-27T04:49:32Z");

        store.save(session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                List.of(event)
        ));

        assertThat(service.events("session-1")).containsExactly(event);
    }

    @Test
    void shouldThrowWhenEventsRequestedForMissingSession() {
        assertThatThrownBy(() -> service.events("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Imported session not found: missing");
    }

    @Test
    void shouldReturnImportedTraceWithoutValidationWhenFlowIdMissing() {
        store.save(session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                List.of(
                        event("trace-1", "lab/flow/out", "2026-04-27T04:49:34Z"),
                        event("trace-1", "lab/flow/in", "2026-04-27T04:49:32Z")
                )
        ));

        TraceTimelineResponse response = service.trace("session-1", "trace-1", null);

        assertThat(response.traceId()).isEqualTo("trace-1");
        assertThat(response.eventCount()).isEqualTo(2);
        assertThat(response.events()).extracting("channel")
                .containsExactly("lab/flow/in", "lab/flow/out");
        assertThat(response.validation()).isNull();

        verify(flowValidationService).validate(anyList(), isNull());
    }

    @Test
    void shouldReturnImportedTraceWithValidationWhenFlowIdProvided() {
        store.save(session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                List.of(event("trace-1", "lab/flow/in", "2026-04-27T04:49:32Z"))
        ));

        TraceTimelineResponse response = service.trace("session-1", "trace-1", "main-lab-flow");

        assertThat(response.validation()).isNotNull();
        assertThat(response.validation().status()).isEqualTo("COMPLETE");

        verify(flowValidationService).validate(anyList(), eq("main-lab-flow"));
    }

    @Test
    void shouldReturnEmptyTraceForUnknownTraceId() {
        store.save(session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                List.of(event("trace-1", "lab/flow/in", "2026-04-27T04:49:32Z"))
        ));

        TraceTimelineResponse response = service.trace("session-1", "missing-trace", null);

        assertThat(response.traceId()).isEqualTo("missing-trace");
        assertThat(response.eventCount()).isZero();
        assertThat(response.events()).isEmpty();
        assertThat(response.validation()).isNull();
    }

    @Test
    void shouldCalculateImportedTraceTimings() {
        store.save(session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                List.of(
                        event(
                                "trace-1",
                                "lab/flow/in",
                                "2026-04-27T04:49:32Z",
                                "2026-04-27T04:49:31Z"
                        ),
                        event(
                                "trace-1",
                                "lab/flow/out",
                                "2026-04-27T04:49:35Z",
                                "2026-04-27T04:49:34Z"
                        )
                )
        ));

        TraceTimelineResponse response = service.trace("session-1", "trace-1", null);

        assertThat(response.startedAt()).isEqualTo(Instant.parse("2026-04-27T04:49:31Z"));
        assertThat(response.lastObservedAt()).isEqualTo(Instant.parse("2026-04-27T04:49:35Z"));
        assertThat(response.totalSourceDurationMs()).isEqualTo(3_000);
        assertThat(response.totalObservedDurationMs()).isEqualTo(3_000);
        assertThat(response.events().get(1).deltaFromPreviousSourceMs()).isEqualTo(3_000);
        assertThat(response.events().get(1).deltaFromPreviousObservedMs()).isEqualTo(3_000);
    }

    @Test
    void shouldThrowWhenTraceRequestedForMissingSession() {
        assertThatThrownBy(() -> service.trace("missing", "trace-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Imported session not found: missing");
    }

    private ImportedSession session(String sessionId,
                                    String name,
                                    Instant importedAt,
                                    List<ObservedEvent> events) {
        return new ImportedSession(
                sessionId,
                name,
                importedAt,
                metadata(events.size()),
                config(name),
                events
        );
    }

    private SessionExportMetadata metadata(int eventCount) {
        return new SessionExportMetadata(
                "system-flow-investigator-session",
                "1.0",
                Instant.parse("2026-04-27T04:50:00Z"),
                new EventExportQuery(null, null, null, null, null, null),
                eventCount,
                Map.of("events", "events.ndjson")
        );
    }

    private InvestigationConfig config(String name) {
        return new InvestigationConfig(
                name,
                "desc",
                List.of(),
                new FlowValidationRules(50, true)
        );
    }

    private ObservedEvent event(String traceId,
                                String channel,
                                String observedAt) {
        Instant observed = Instant.parse(observedAt);
        return event(traceId, channel, observedAt, observed.minusSeconds(1).toString());
    }

    private ObservedEvent event(String traceId,
                                String channel,
                                String observedAt,
                                String sourceSentAt) {
        return new ObservedEvent(
                channel.startsWith("ws/") ? "WS" : "MQTT",
                channel.startsWith("ws/") ? "web" : "localhost:1883",
                channel,
                Instant.parse(observedAt),
                "{\"traceId\":\"%s\"}".formatted(traceId),
                Map.of("qos", "1"),
                traceId,
                sourceSentAt == null ? null : Instant.parse(sourceSentAt)
        );
    }
}