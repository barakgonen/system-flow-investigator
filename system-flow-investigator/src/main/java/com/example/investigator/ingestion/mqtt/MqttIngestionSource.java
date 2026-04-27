package com.example.investigator.ingestion.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.ingestion.IngestionSource;
import com.example.investigator.ingestion.IngestionSourceRegistry;
import com.example.investigator.ingestion.IngestionSourceStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MqttIngestionSource implements IngestionSource<ConnectMqttRequest, SubscribeMqttRequest> {

    private final MqttObserver mqttObserver;
    private final IngestionSourceRegistry registry;

    public MqttIngestionSource(MqttObserver mqttObserver,
                               IngestionSourceRegistry registry) {
        this.mqttObserver = mqttObserver;
        this.registry = registry;
    }

    @PostConstruct
    void register() {
        registry.register(this);
    }

    @Override
    public String id() {
        return "mqtt";
    }

    @Override
    public String displayName() {
        return "MQTT Broker";
    }

    @Override
    public String protocol() {
        return "MQTT";
    }

    @Override
    public void connect(ConnectMqttRequest connectRequest) {
        mqttObserver.connect(connectRequest);
    }

    @Override
    public void subscribe(SubscribeMqttRequest subscribeRequest) {
        mqttObserver.subscribe(subscribeRequest);
    }

    @Override
    public void disconnect() {
        // Not supported by current MqttObserver contract yet.
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
                observedChannels().isEmpty() ? "No observed topics yet" : "Active / observed topics"
        );
    }

    @Override
    public List<String> observedChannels() {
        return mqttObserver.observedChannels().stream().toList();
    }
}