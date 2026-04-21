package com.example.investigator.service;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.DashboardSummary;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.ingestion.mqtt.MqttObserver;
import com.example.investigator.ingestion.websocket.WebSocketObserver;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class InvestigatorFacade {

    private final EventHub eventHub;
    private final RecentEventStore recentEventStore;
    private final MqttObserver mqttObserver;
    private final WebSocketObserver webSocketObserver;

    public InvestigatorFacade(EventHub eventHub,
                              RecentEventStore recentEventStore,
                              MqttObserver mqttObserver,
                              WebSocketObserver webSocketObserver) {
        this.eventHub = eventHub;
        this.recentEventStore = recentEventStore;
        this.mqttObserver = mqttObserver;
        this.webSocketObserver = webSocketObserver;
    }

    public Flux<ObservedEvent> streamEvents() {
        return eventHub.stream();
    }

    public List<ObservedEvent> getRecentEvents(String channel) {
        if (channel == null || channel.isBlank()) {
            return recentEventStore.getAllRecent();
        }
        return recentEventStore.getRecent(channel);
    }

    public List<ObservedEvent> getRecentEvents(Set<String> channels,
                                               String textContains,
                                               String traceId) {
        return recentEventStore.getAllRecent().stream()
                .filter(event -> channels == null || channels.isEmpty() || channels.contains(event.channel()))
                .filter(event -> textContains == null || textContains.isBlank() || safe(event.payload()).contains(textContains))
                .filter(event -> traceId == null || traceId.isBlank() || traceId.equals(event.traceId()))
                .toList();
    }

    public Set<String> observedMqttChannels() {
        return mqttObserver.observedChannels();
    }

    public Set<String> observedWebSocketChannels() {
        return webSocketObserver.observedChannels();
    }

    public Set<String> observedAllChannels() {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(mqttObserver.observedChannels());
        result.addAll(webSocketObserver.observedChannels());
        return result;
    }

    public void connectMqtt(ConnectMqttRequest request) {
        mqttObserver.connect(request);
    }

    public void subscribeMqtt(SubscribeMqttRequest request) {
        mqttObserver.subscribe(request);
    }

    public void connectWebSocket(ConnectWebSocketRequest request) {
        webSocketObserver.connect(request);
    }

    public void subscribeWebSocket(SubscribeWebSocketRequest request) {
        webSocketObserver.subscribe(request);
    }

    public DashboardSummary getDashboardSummary() {
        List<ObservedEvent> recentEvents = recentEventStore.getAllRecent();
        Set<String> mqttChannels = mqttObserver.observedChannels();
        Set<String> wsChannels = webSocketObserver.observedChannels();

        List<String> latestTopics = recentEvents.stream()
                .map(ObservedEvent::channel)
                .filter(channel -> channel != null && !channel.isBlank())
                .distinct()
                .limit(10)
                .toList();

        List<String> latestTraceIds = recentEvents.stream()
                .map(ObservedEvent::traceId)
                .filter(traceId -> traceId != null && !traceId.isBlank())
                .distinct()
                .limit(10)
                .toList();

        boolean mqttConnected = true;

        return new DashboardSummary(
                mqttConnected,
                mqttChannels.size(),
                wsChannels.size(),
                recentEvents.size(),
                latestTopics,
                latestTraceIds
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}