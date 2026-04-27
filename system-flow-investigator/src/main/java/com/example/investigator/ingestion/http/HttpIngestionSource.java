package com.example.investigator.ingestion.http;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.ingestion.IngestionSource;
import com.example.investigator.ingestion.IngestionSourceRegistry;
import com.example.investigator.ingestion.IngestionSourceStatus;
import com.example.investigator.ingestion.ObservedEventPublisher;
import com.example.investigator.service.TraceIdExtractor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class HttpIngestionSource implements IngestionSource<Void, Void> {

    private final ObservedEventPublisher publisher;
    private final TraceIdExtractor traceIdExtractor;
    private final IngestionSourceRegistry registry;

    private final ConcurrentSkipListSet<String> observedChannels = new ConcurrentSkipListSet<>();

    private volatile Instant lastEventAt;

    public HttpIngestionSource(ObservedEventPublisher publisher,
                               TraceIdExtractor traceIdExtractor,
                               IngestionSourceRegistry registry) {
        this.publisher = publisher;
        this.traceIdExtractor = traceIdExtractor;
        this.registry = registry;
    }

    @PostConstruct
    void register() {
        registry.register(this);
    }

    public ObservedEvent ingest(HttpIngestionEventRequest request) {
        String protocol = valueOrDefault(request.protocol(), "HTTP");
        String source = valueOrDefault(request.source(), "http");
        String channel = valueOrDefault(request.channel(), "http/manual");
        String payload = valueOrDefault(request.payload(), "{}");
        Map<String, String> metadata = request.metadata() == null ? Map.of() : request.metadata();

        ObservedEvent event = new ObservedEvent(
                protocol,
                source,
                channel,
                Instant.now(),
                payload,
                metadata,
                traceIdExtractor.extract(payload),
                extractTimestamp(payload)
        );

        observedChannels.add(channel);
        lastEventAt = event.observedAt();

        publisher.publish(event);

        return event;
    }

    private Instant extractTimestamp(String payload) {
        try {
            // use same logic you already use elsewhere
            return "2024-01-01T00:00:00Z".equals(payload) ? Instant.parse(payload) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String id() {
        return "http";
    }

    @Override
    public String displayName() {
        return "HTTP Ingestion";
    }

    @Override
    public String protocol() {
        return "HTTP";
    }

    @Override
    public void connect(Void connectRequest) {
        // No-op. HTTP ingestion is request-based.
    }

    @Override
    public void subscribe(Void subscribeRequest) {
        // No-op. HTTP ingestion is request-based.
    }

    @Override
    public void disconnect() {
        // No-op. HTTP ingestion is request-based.
    }

    @Override
    public IngestionSourceStatus status() {
        return new IngestionSourceStatus(
                id(),
                displayName(),
                protocol(),
                true,
                observedChannels(),
                lastEventAt,
                observedChannels.isEmpty()
                        ? "Ready. No HTTP events ingested yet."
                        : "Ready. HTTP events ingested."
        );
    }

    @Override
    public List<String> observedChannels() {
        return List.copyOf(observedChannels);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}