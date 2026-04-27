package com.example.investigator.ingestion.ws;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.ingestion.IngestionSource;
import com.example.investigator.ingestion.IngestionSourceRegistry;
import com.example.investigator.ingestion.IngestionSourceStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketIngestionSource implements IngestionSource<ConnectWebSocketRequest, SubscribeWebSocketRequest> {

    private final WebSocketObserver webSocketObserver;
    private final IngestionSourceRegistry registry;

    public WebSocketIngestionSource(WebSocketObserver webSocketObserver,
                                    IngestionSourceRegistry registry) {
        this.webSocketObserver = webSocketObserver;
        this.registry = registry;
    }

    @PostConstruct
    void register() {
        registry.register(this);
    }

    @Override
    public String id() {
        return "websocket";
    }

    @Override
    public String displayName() {
        return "WebSocket";
    }

    @Override
    public String protocol() {
        return "WS";
    }

    @Override
    public void connect(ConnectWebSocketRequest connectRequest) {
        webSocketObserver.connect(connectRequest);
    }

    @Override
    public void subscribe(SubscribeWebSocketRequest subscribeRequest) {
        webSocketObserver.subscribe(subscribeRequest);
    }

    @Override
    public void disconnect() {
        // Not supported by current WebSocketObserver contract yet.
    }

    @Override
    public IngestionSourceStatus status() {
        return new IngestionSourceStatus(
                id(),
                displayName(),
                protocol(),
                !observedChannels().isEmpty(),
                observedChannels(),
                null,
                observedChannels().isEmpty() ? "No observed channels yet" : "Active / observed channels"
        );
    }

    @Override
    public List<String> observedChannels() {
        return webSocketObserver.observedChannels().stream().toList();
    }
}