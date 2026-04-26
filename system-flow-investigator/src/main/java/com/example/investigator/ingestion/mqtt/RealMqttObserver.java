package com.example.investigator.ingestion.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.ingestion.infra.AbstractIngestionSource;
import com.example.investigator.ingestion.infra.ObservedEventPipeline;
import com.example.investigator.service.SourceTimestampExtractor;
import com.example.investigator.service.TraceIdExtractor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Primary
public class RealMqttObserver extends AbstractIngestionSource<ConnectMqttRequest, SubscribeMqttRequest>
        implements MqttObserver {

    private final ObservedEventPipeline pipeline;
    private final TraceIdExtractor traceIdExtractor;
    private final SourceTimestampExtractor sourceTimestampExtractor;
    private final MqttClientFactory mqttClientFactory;

    private final String defaultHost;
    private final int defaultPort;
    private final String defaultClientIdPrefix;
    private final String initialTopicFilter;
    private final boolean persistByDefault;

    private volatile String activeHost;
    private volatile int activePort;
    private volatile String activeClientIdPrefix;

    private volatile MqttClient client;

    public RealMqttObserver(ObservedEventPipeline pipeline,
                            TraceIdExtractor traceIdExtractor,
                            SourceTimestampExtractor sourceTimestampExtractor,
                            MqttClientFactory mqttClientFactory,
                            @Value("${mqtt.host:localhost}") String defaultHost,
                            @Value("${mqtt.port:1883}") int defaultPort,
                            @Value("${mqtt.client-id-prefix:system-flow-investigator}") String defaultClientIdPrefix,
                            @Value("${mqtt.topic-filter:#}") String initialTopicFilter,
                            @Value("${mqtt.persist-by-default:false}") boolean persistByDefault) {
        this.pipeline = pipeline;
        this.traceIdExtractor = traceIdExtractor;
        this.sourceTimestampExtractor = sourceTimestampExtractor;
        this.mqttClientFactory = mqttClientFactory;
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
        this.defaultClientIdPrefix = defaultClientIdPrefix;
        this.initialTopicFilter = initialTopicFilter;
        this.persistByDefault = persistByDefault;

        this.activeHost = defaultHost;
        this.activePort = defaultPort;
        this.activeClientIdPrefix = defaultClientIdPrefix;
    }

    @Override
    public String sourceType() {
        return "MQTT";
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

        subscribe(new SubscribeMqttRequest(
                "flow-debugger",
                initialTopicFilter,
                persistByDefault
        ));
    }

    @Override
    public synchronized void connect(ConnectMqttRequest request) {
        if (request != null) {
            if (request.host() != null && !request.host().isBlank()) {
                activeHost = request.host();
            }
            if (request.port() > 0) {
                activePort = request.port();
            }
            if (request.clientId() != null && !request.clientId().isBlank()) {
                activeClientIdPrefix = request.clientId();
            }
        }

        if (client != null && client.isConnected()) {
            return;
        }

        try {
            String brokerUrl = "tcp://" + activeHost + ":" + activePort;
            String clientId = activeClientIdPrefix + "-" + UUID.randomUUID();

            client = mqttClientFactory.create(brokerUrl, clientId);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT connection lost: " +
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
            System.out.println("MQTT connected to " + brokerUrl);
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to connect MQTT", e);
        }
    }

    @Override
    public synchronized void subscribe(SubscribeMqttRequest request) {
        connect(null);

        try {
            client.subscribe(request.topicFilter(), 1);
            registerPersistence(request.topicFilter(), request.persistToFile());
            markObserved(request.topicFilter());
            System.out.println("MQTT subscribed to " + request.topicFilter());
        } catch (MqttException e) {
            throw new IllegalStateException("Failed subscribing MQTT to " + request.topicFilter(), e);
        }
    }

    private void handleIncomingMessage(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        markObserved(topic);

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
                traceIdExtractor.extract(payload),
                sourceTimestampExtractor.extract(payload)
        );

        pipeline.accept(event, shouldPersist(topic));
    }

    @PreDestroy
    public synchronized void stop() {
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}