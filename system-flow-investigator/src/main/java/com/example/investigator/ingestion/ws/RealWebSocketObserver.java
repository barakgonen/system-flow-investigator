package com.example.investigator.ingestion.ws;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.ingestion.infra.AbstractIngestionSource;
import com.example.investigator.ingestion.infra.ObservedEventPipeline;
import com.example.investigator.service.SourceTimestampExtractor;
import com.example.investigator.service.TraceIdExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
public class RealWebSocketObserver extends AbstractIngestionSource<ConnectWebSocketRequest, SubscribeWebSocketRequest>
        implements WebSocketObserver {

    private final String defaultHost;
    private final int defaultPort;

    private final ObservedEventPipeline pipeline;
    private final TraceIdExtractor traceIdExtractor;
    private final SourceTimestampExtractor sourceTimestampExtractor;
    private final WebSocketClientFactory webSocketClientFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessionsByConnection = new ConcurrentHashMap<>();

    public RealWebSocketObserver(ObservedEventPipeline pipeline,
                                 TraceIdExtractor traceIdExtractor,
                                 SourceTimestampExtractor sourceTimestampExtractor,
                                 WebSocketClientFactory webSocketClientFactory,
                                 @Value("${ws.host:localhost}") String defaultHost,
                                 @Value("${ws.port:8090}") int defaultPort) {
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
        this.pipeline = pipeline;
        this.traceIdExtractor = traceIdExtractor;
        this.sourceTimestampExtractor = sourceTimestampExtractor;
        this.webSocketClientFactory = webSocketClientFactory;
    }

    @PostConstruct
    public void start() {
        connect(new ConnectWebSocketRequest(
                "web",
                "ws://" + defaultHost + ":" + defaultPort + "/ws/live"
        ));

        subscribe(new SubscribeWebSocketRequest(
                "web",
                "ws/live/out",
                false
        ));
    }

    @Override
    public String sourceType() {
        return "WS";
    }

    @Override
    public void connect(ConnectWebSocketRequest request) {
        try {
            WebSocketClient client = webSocketClientFactory.create();
            WebSocketHandler handler = createHandler(request);

            client.execute(handler, request.url()).get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed connecting to WebSocket " + request.url(), e);
        }
    }

    WebSocketHandler createHandler(ConnectWebSocketRequest request) {
        return new TextWebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                sessionsByConnection.put(request.connectionName(), session);
                System.out.println("WS connected: " + request.connectionName() + " -> " + request.url());
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                handleIncomingMessage(request.connectionName(), message.getPayload());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                sessionsByConnection.remove(request.connectionName());
                System.out.println("WS disconnected: " + request.connectionName() + " status=" + status);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                System.err.println("WS transport error [" + request.connectionName() + "]: " + exception.getMessage());
            }
        };
    }

    @Override
    public void subscribe(SubscribeWebSocketRequest request) {
        registerPersistence(request.logicalChannel(), request.persistToFile());
        markObserved(request.logicalChannel());
        System.out.println("WS logical subscription registered: " + request.logicalChannel());
    }

    private void handleIncomingMessage(String connectionName, String payload) {
        String channel = resolveLogicalChannel(connectionName, payload);
        markObserved(channel);

        ObservedEvent event = new ObservedEvent(
                "WS",
                connectionName,
                channel,
                Instant.now(),
                payload,
                Map.of(),
                traceIdExtractor.extract(payload),
                sourceTimestampExtractor.extract(payload)
        );

        pipeline.accept(event, shouldPersist(channel));
    }

    private String resolveLogicalChannel(String connectionName, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode channelNode = root.get("channel");

            if (channelNode != null && !channelNode.isNull() && !channelNode.asText().isBlank()) {
                return channelNode.asText();
            }
        } catch (Exception ignored) {
        }

        return "WS::" + connectionName;
    }
}