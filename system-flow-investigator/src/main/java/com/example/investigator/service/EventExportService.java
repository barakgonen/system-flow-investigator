package com.example.investigator.service;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.export.EventExportQuery;
import com.example.investigator.storage.RecentEventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class EventExportService {

    private final RecentEventStore recentEventStore;
    private final ObjectMapper objectMapper;

    public EventExportService(RecentEventStore recentEventStore) {
        this.recentEventStore = recentEventStore;
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String exportNdjson(EventExportQuery query) {
        return recentEventStore.getAllRecent()
                .stream()
                .filter(event -> matches(event, query))
                .map(this::toJsonLine)
                .reduce("", (a, b) -> a + b + "\n");
    }

    public List<ObservedEvent> exportEvents(EventExportQuery query) {
        return recentEventStore.getAllRecent()
                .stream()
                .filter(event -> matches(event, query))
                .toList();
    }

    private boolean matches(ObservedEvent event, EventExportQuery query) {
        if (query == null) {
            return true;
        }

        if (query.channel() != null && !query.channel().isBlank()
                && !Objects.equals(query.channel(), event.channel())) {
            return false;
        }

        if (query.traceId() != null && !query.traceId().isBlank()
                && !Objects.equals(query.traceId(), event.traceId())) {
            return false;
        }

        if (query.protocol() != null && !query.protocol().isBlank()
                && !query.protocol().equalsIgnoreCase(event.protocol())) {
            return false;
        }

        if (query.source() != null && !query.source().isBlank()
                && !Objects.equals(query.source(), event.source())) {
            return false;
        }

        if (query.from() != null && event.observedAt().isBefore(query.from())) {
            return false;
        }

        if (query.to() != null && event.observedAt().isAfter(query.to())) {
            return false;
        }

        return true;
    }

    private String toJsonLine(ObservedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed serializing event to NDJSON", e);
        }
    }
}