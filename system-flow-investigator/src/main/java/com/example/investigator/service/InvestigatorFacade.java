package com.example.investigator.service;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.mqtt.MqttObserver;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import com.example.investigator.websocket.WebSocketObserver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    public Set<String> observedTopics() {
        return mqttObserver.observedTopics();
    }

    public Set<String> observedWebSocketChannels() {
        return webSocketObserver.observedChannels();
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
}