package com.example.investigator.websocket;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.service.TraceIdExtractor;
import com.example.investigator.storage.MessageFileSink;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
public class RealWebSocketObserver implements WebSocketObserver {

    private final EventHub eventHub;
    private final RecentEventStore recentEventStore;
    private final MessageFileSink messageFileSink;
    private final TraceIdExtractor traceIdExtractor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessionsByConnection = new ConcurrentHashMap<>();
    private final Set<String> observedChannels = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> persistByLogicalChannel = new ConcurrentHashMap<>();

    public RealWebSocketObserver(EventHub eventHub,
                                 RecentEventStore recentEventStore,
                                 MessageFileSink messageFileSink,
                                 TraceIdExtractor traceIdExtractor) {
        this.eventHub = eventHub;
        this.recentEventStore = recentEventStore;
        this.messageFileSink = messageFileSink;
        this.traceIdExtractor = traceIdExtractor;
    }

    @Override
    public void connect(ConnectWebSocketRequest request) {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();

            WebSocketHandler handler = new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    sessionsByConnection.put(request.connectionName(), session);
                    System.out.println("Investigator WS connected: " + request.connectionName() + " -> " + request.url());
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    handleIncomingMessage(request.connectionName(), message.getPayload());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    sessionsByConnection.remove(request.connectionName());
                    System.out.println("Investigator WS disconnected: " + request.connectionName());
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    System.err.println("Investigator WS transport error [" + request.connectionName() + "]: " + exception.getMessage());
                }
            };

            client.execute(handler, request.url()).get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed connecting investigator to WebSocket: " + request.url(), e);
        }
    }

    @Override
    public void subscribe(SubscribeWebSocketRequest request) {
        persistByLogicalChannel.put(request.logicalChannel(), request.persistToFile());
        observedChannels.add(request.logicalChannel());
        System.out.println("Investigator WS logical subscription registered: " +
                request.logicalChannel() + " (persist=" + request.persistToFile() + ")");
    }

    @Override
    public Set<String> observedChannels() {
        return observedChannels;
    }

    private void handleIncomingMessage(String connectionName, String payload) {
        String logicalChannel = resolveLogicalChannel(connectionName, payload);
        observedChannels.add(logicalChannel);

        ObservedEvent event = new ObservedEvent(
                "WS",
                connectionName,
                logicalChannel,
                Instant.now(),
                payload,
                Map.of(),
                traceIdExtractor.extract(payload)
        );

        recentEventStore.add(event);
        eventHub.publish(event);

        if (persistByLogicalChannel.getOrDefault(logicalChannel, false)) {
            messageFileSink.append(event);
        }
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