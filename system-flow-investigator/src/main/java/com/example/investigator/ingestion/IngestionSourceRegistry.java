package com.example.investigator.ingestion;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IngestionSourceRegistry {

    private final Map<String, IngestionSource<?, ?>> sources = new ConcurrentHashMap<>();

    public void register(IngestionSource<?, ?> source) {
        sources.put(source.id(), source);
    }

    public List<IngestionSource<?, ?>> sources() {
        return sources.values()
                .stream()
                .sorted(Comparator.comparing(IngestionSource::id))
                .toList();
    }

    public IngestionSource<?, ?> get(String sourceId) {
        IngestionSource<?, ?> source = sources.get(sourceId);

        if (source == null) {
            throw new IllegalArgumentException("Unknown ingestion source: " + sourceId);
        }

        return source;
    }

    public List<IngestionSourceStatus> statuses() {
        return sources()
                .stream()
                .map(IngestionSource::status)
                .toList();
    }
}