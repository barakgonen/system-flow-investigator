package com.example.investigator.service;

import com.example.investigator.domain.CorrelatedEvent;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.domain.config.FlowValidationResult;
import com.example.investigator.domain.session.ImportedSession;
import com.example.investigator.domain.session.ImportedSessionSummary;
import com.example.investigator.storage.ImportedSessionStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ImportedSessionService {

    private final ImportedSessionStore importedSessionStore;
    private final FlowValidationService flowValidationService;

    public ImportedSessionService(ImportedSessionStore importedSessionStore,
                                  FlowValidationService flowValidationService) {
        this.importedSessionStore = importedSessionStore;
        this.flowValidationService = flowValidationService;
    }

    public List<ImportedSessionSummary> listImportedSessions() {
        return importedSessionStore.summaries();
    }

    public List<ObservedEvent> events(String sessionId) {
        return session(sessionId).events();
    }

    public TraceTimelineResponse trace(String sessionId, String traceId, String flowId) {
        List<ObservedEvent> matchingEvents = session(sessionId).events()
                .stream()
                .filter(event -> Objects.equals(traceId, event.traceId()))
                .sorted(eventComparator())
                .toList();

        if (matchingEvents.isEmpty()) {
            FlowValidationResult validation = flowValidationService.validate(List.of(), flowId);

            return new TraceTimelineResponse(
                    traceId,
                    0,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    validation
            );
        }

        AtomicInteger index = new AtomicInteger(1);
        List<CorrelatedEvent> correlatedEvents = buildCorrelatedEvents(matchingEvents, index);

        Instant startedAt = firstNonNullSourceOrObserved(matchingEvents.get(0));

        Instant lastObservedAt = matchingEvents.stream()
                .map(ObservedEvent::observedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        Long totalSourceDurationMs = calculateTotalSourceDuration(matchingEvents);
        Long totalObservedDurationMs = calculateTotalObservedDuration(matchingEvents);

        FlowValidationResult validation = flowValidationService.validate(correlatedEvents, flowId);

        return new TraceTimelineResponse(
                traceId,
                correlatedEvents.size(),
                startedAt,
                lastObservedAt,
                totalSourceDurationMs,
                totalObservedDurationMs,
                correlatedEvents,
                validation
        );
    }

    private ImportedSession session(String sessionId) {
        return importedSessionStore.get(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Imported session not found: " + sessionId));
    }

    private List<CorrelatedEvent> buildCorrelatedEvents(List<ObservedEvent> events, AtomicInteger index) {
        ObservedEvent[] previous = new ObservedEvent[1];

        return events.stream()
                .map(event -> {
                    ObservedEvent prev = previous[0];

                    Long deltaSource = null;
                    Long deltaObserved = null;

                    if (prev != null) {
                        deltaSource = durationMs(effectiveSourceTime(prev), effectiveSourceTime(event));
                        deltaObserved = durationMs(prev.observedAt(), event.observedAt());
                    }

                    previous[0] = event;

                    return new CorrelatedEvent(
                            index.getAndIncrement(),
                            event.protocol(),
                            event.source(),
                            event.channel(),
                            event.sourceSentAt(),
                            event.observedAt(),
                            deltaSource,
                            deltaObserved,
                            event.payload(),
                            event.metadata()
                    );
                })
                .toList();
    }

    private Comparator<ObservedEvent> eventComparator() {
        return Comparator
                .comparing(this::effectiveSortTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ObservedEvent::observedAt, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Instant effectiveSortTime(ObservedEvent event) {
        return event.sourceSentAt() != null ? event.sourceSentAt() : event.observedAt();
    }

    private Instant effectiveSourceTime(ObservedEvent event) {
        return event.sourceSentAt() != null ? event.sourceSentAt() : event.observedAt();
    }

    private Instant firstNonNullSourceOrObserved(ObservedEvent event) {
        return event.sourceSentAt() != null ? event.sourceSentAt() : event.observedAt();
    }

    private Long calculateTotalSourceDuration(List<ObservedEvent> events) {
        Instant first = events.stream()
                .map(this::effectiveSourceTime)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Instant last = events.stream()
                .map(this::effectiveSourceTime)
                .filter(Objects::nonNull)
                .reduce((a, b) -> b)
                .orElse(null);

        return durationMs(first, last);
    }

    private Long calculateTotalObservedDuration(List<ObservedEvent> events) {
        Instant first = events.stream()
                .map(ObservedEvent::observedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Instant last = events.stream()
                .map(ObservedEvent::observedAt)
                .filter(Objects::nonNull)
                .reduce((a, b) -> b)
                .orElse(null);

        return durationMs(first, last);
    }

    private Long durationMs(Instant from, Instant to) {
        if (from == null || to == null) {
            return null;
        }

        return Duration.between(from, to).toMillis();
    }
}