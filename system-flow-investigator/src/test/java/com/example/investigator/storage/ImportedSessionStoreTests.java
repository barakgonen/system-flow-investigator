package com.example.investigator.storage;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.config.FlowValidationRules;
import com.example.investigator.domain.config.InvestigationConfig;
import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.domain.export.SessionExportMetadata;
import com.example.investigator.domain.session.ImportedSession;
import com.example.investigator.domain.session.ImportedSessionSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ImportedSessionStoreTests {

    @Test
    void shouldSaveAndGetSession() {
        ImportedSessionStore store = new ImportedSessionStore();

        ImportedSession session = session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                1
        );

        ImportedSession saved = store.save(session);

        assertThat(saved).isSameAs(session);

        Optional<ImportedSession> loaded = store.get("session-1");

        assertThat(loaded).isPresent();
        assertThat(loaded.get()).isSameAs(session);
    }

    @Test
    void shouldReturnEmptyWhenSessionDoesNotExist() {
        ImportedSessionStore store = new ImportedSessionStore();

        assertThat(store.get("missing")).isEmpty();
    }

    @Test
    void shouldReturnSummariesSortedByImportedAtDescending() {
        ImportedSessionStore store = new ImportedSessionStore();

        store.save(session(
                "old",
                "Old Session",
                Instant.parse("2026-04-27T05:00:00Z"),
                1
        ));

        store.save(session(
                "new",
                "New Session",
                Instant.parse("2026-04-27T06:00:00Z"),
                2
        ));

        List<ImportedSessionSummary> summaries = store.summaries();

        assertThat(summaries).hasSize(2);
        assertThat(summaries).extracting(ImportedSessionSummary::sessionId)
                .containsExactly("new", "old");
        assertThat(summaries).extracting(ImportedSessionSummary::eventCount)
                .containsExactly(2, 1);
    }

    @Test
    void shouldReturnAllSessions() {
        ImportedSessionStore store = new ImportedSessionStore();

        ImportedSession one = session(
                "session-1",
                "Session One",
                Instant.parse("2026-04-27T05:00:00Z"),
                1
        );

        ImportedSession two = session(
                "session-2",
                "Session Two",
                Instant.parse("2026-04-27T06:00:00Z"),
                2
        );

        store.save(one);
        store.save(two);

        assertThat(store.all()).containsExactlyInAnyOrder(one, two);
    }

    @Test
    void shouldReplaceSessionWithSameId() {
        ImportedSessionStore store = new ImportedSessionStore();

        ImportedSession first = session(
                "session-1",
                "First",
                Instant.parse("2026-04-27T05:00:00Z"),
                1
        );

        ImportedSession replacement = session(
                "session-1",
                "Replacement",
                Instant.parse("2026-04-27T06:00:00Z"),
                3
        );

        store.save(first);
        store.save(replacement);

        assertThat(store.get("session-1")).containsSame(replacement);
        assertThat(store.all()).hasSize(1);
        assertThat(store.summaries().get(0).name()).isEqualTo("Replacement");
        assertThat(store.summaries().get(0).eventCount()).isEqualTo(3);
    }

    private ImportedSession session(String sessionId,
                                    String name,
                                    Instant importedAt,
                                    int eventCount) {
        List<ObservedEvent> events = java.util.stream.IntStream.range(0, eventCount)
                .mapToObj(index -> event("trace-" + index))
                .toList();

        return new ImportedSession(
                sessionId,
                name,
                importedAt,
                metadata(eventCount),
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

    private ObservedEvent event(String traceId) {
        return new ObservedEvent(
                "MQTT",
                "localhost:1883",
                "lab/flow/in",
                Instant.parse("2026-04-27T04:49:32Z"),
                "{\"traceId\":\"%s\"}".formatted(traceId),
                Map.of("qos", "1"),
                traceId,
                Instant.parse("2026-04-27T04:49:31Z")
        );
    }
}