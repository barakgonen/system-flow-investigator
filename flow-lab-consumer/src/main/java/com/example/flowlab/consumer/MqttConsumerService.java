package com.example.flowlab.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MqttConsumerService {

    private final LiveEventsBroadcaster broadcaster;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${mqtt.host:localhost}")
    private String host;

    @Value("${mqtt.port:1883}")
    private int port;

    @Value("${mqtt.in-topic:lab/flow/out}")
    private String inTopic;

    @Value("${app.ws-channel:ws/live/out}")
    private String wsChannel;

    private MqttClient client;

    public MqttConsumerService(LiveEventsBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @PostConstruct
    public void start() {
        try {
            String brokerUrl = "tcp://" + host + ":" + port;
            client = new MqttClient(brokerUrl, "flow-lab-consumer-" + UUID.randomUUID());

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("Consumer connection lost: " +
                            (cause == null ? "unknown" : cause.getMessage()));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Map<String, Object> input = mapper.readValue(
                            new String(message.getPayload(), StandardCharsets.UTF_8),
                            new TypeReference<>() {}
                    );

                    System.out.println(
                            "consumed traceId=" + input.get("traceId") +
                                    " seq=" + input.get("sequence") +
                                    " step=" + input.get("step")
                    );

                    Map<String, Object> wsPayload = new LinkedHashMap<>();
                    wsPayload.put("channel", wsChannel);
                    wsPayload.put("traceId", input.get("traceId"));
                    wsPayload.put("sequence", input.get("sequence"));
                    wsPayload.put("step", "consumer-ws");
                    wsPayload.put("timestamp", Instant.now().toString());
                    wsPayload.put("sourceTopic", topic);
                    wsPayload.put("message", "broadcast from consumer websocket");
                    wsPayload.put("original", input);

                    broadcaster.broadcast(mapper.writeValueAsString(wsPayload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect();
            client.subscribe(inTopic);

            System.out.println("Consumer connected to " + brokerUrl + " and subscribed to " + inTopic);
            System.out.println("WebSocket endpoint ready at /ws/live");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start consumer MQTT service", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}