package com.example.investigator.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.service.TraceIdExtractor;
import com.example.investigator.storage.MessageFileSink;
import com.example.investigator.storage.RecentEventStore;
import com.example.investigator.stream.EventHub;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
public class RealMqttObserver implements MqttObserver {

    private final EventHub eventHub;
    private final RecentEventStore recentEventStore;
    private final MessageFileSink messageFileSink;
    private final TraceIdExtractor traceIdExtractor;

    private final Set<String> observedTopics = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> persistByTopicFilter = new ConcurrentHashMap<>();

    private final String defaultHost;
    private final int defaultPort;
    private final String defaultClientIdPrefix;
    private final String initialTopicFilter;
    private final boolean persistByDefault;

    private volatile String activeHost;
    private volatile int activePort;
    private volatile String activeClientIdPrefix;

    private volatile MqttClient client;

    public RealMqttObserver(EventHub eventHub,
                            RecentEventStore recentEventStore,
                            MessageFileSink messageFileSink,
                            TraceIdExtractor traceIdExtractor,
                            @Value("${mqtt.host:localhost}") String defaultHost,
                            @Value("${mqtt.port:1883}") int defaultPort,
                            @Value("${mqtt.client-id-prefix:system-flow-investigator}") String defaultClientIdPrefix,
                            @Value("${mqtt.topic-filter:#}") String initialTopicFilter,
                            @Value("${mqtt.persist-by-default:false}") boolean persistByDefault) {
        this.eventHub = eventHub;
        this.recentEventStore = recentEventStore;
        this.messageFileSink = messageFileSink;
        this.traceIdExtractor = traceIdExtractor;
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
        this.defaultClientIdPrefix = defaultClientIdPrefix;
        this.initialTopicFilter = initialTopicFilter;
        this.persistByDefault = persistByDefault;

        this.activeHost = defaultHost;
        this.activePort = defaultPort;
        this.activeClientIdPrefix = defaultClientIdPrefix;
    }

    @PostConstruct
    public void start() {
        connect(new ConnectMqttRequest(
                "flow-debugger",
                defaultHost,
                defaultPort,
                defaultClientIdPrefix,
                null,
                null
        ));
        subscribe(new SubscribeMqttRequest("flow-debugger", initialTopicFilter, persistByDefault));
    }

    @Override
    public synchronized void connect(ConnectMqttRequest request) {
        if (request != null) {
            if (request.host() != null && !request.host().isBlank()) {
                this.activeHost = request.host();
            }
            if (request.port() > 0) {
                this.activePort = request.port();
            }
            if (request.clientId() != null && !request.clientId().isBlank()) {
                this.activeClientIdPrefix = request.clientId();
            }
        }

        if (client != null && client.isConnected()) {
            return;
        }

        try {
            String brokerUrl = "tcp://" + activeHost + ":" + activePort;
            String clientId = "flow-debugger";//activeClientIdPrefix + "-" + UUID.randomUUID();
            client = new MqttClient(brokerUrl, clientId);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Investigator MQTT connection lost: " +
                            (cause == null ? "unknown" : cause.getMessage()));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleIncomingMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            if (request != null) {
                if (request.username() != null && !request.username().isBlank()) {
                    options.setUserName(request.username());
                }
                if (request.password() != null) {
                    options.setPassword(request.password().toCharArray());
                }
            }

            client.connect(options);
            System.out.println("Investigator connected to " + brokerUrl);
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to connect investigator to MQTT broker", e);
        }
    }

    @Override
    public synchronized void subscribe(SubscribeMqttRequest request) {
        connect(null);

        try {
            client.subscribe(request.topicFilter(), 1);
            persistByTopicFilter.put(request.topicFilter(), request.persistToFile());
            System.out.println("Investigator subscribed to " + request.topicFilter() +
                    " (persist=" + request.persistToFile() + ")");
        } catch (MqttException e) {
            throw new IllegalStateException("Failed subscribing to topic filter " + request.topicFilter(), e);
        }
    }

    @Override
    public Set<String> observedTopics() {
        return observedTopics;
    }

    private void handleIncomingMessage(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        observedTopics.add(topic);

        ObservedEvent event = new ObservedEvent(
                "MQTT",
                activeHost + ":" + activePort,
                topic,
                Instant.now(),
                payload,
                Map.of(
                        "qos", String.valueOf(message.getQos()),
                        "retained", String.valueOf(message.isRetained())
                ),
                traceIdExtractor.extract(payload)
        );

        recentEventStore.add(event);
        eventHub.publish(event);

        if (shouldPersist(topic)) {
            messageFileSink.append(event);
        }
    }

    private boolean shouldPersist(String topic) {
        if (persistByDefault) {
            return true;
        }
        return persistByTopicFilter.entrySet().stream()
                .anyMatch(entry -> entry.getValue() && matches(entry.getKey(), topic));
    }

    private boolean matches(String filter, String topic) {
        String[] filterParts = filter.split("/");
        String[] topicParts = topic.split("/");

        int i = 0;
        for (; i < filterParts.length; i++) {
            String part = filterParts[i];
            if ("#".equals(part)) {
                return true;
            }
            if (i >= topicParts.length) {
                return false;
            }
            if (!"+".equals(part) && !part.equals(topicParts[i])) {
                return false;
            }
        }
        return i == topicParts.length;
    }

    @PreDestroy
    public synchronized void stop() {
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (MqttException ignored) {
            }
        }
    }
}