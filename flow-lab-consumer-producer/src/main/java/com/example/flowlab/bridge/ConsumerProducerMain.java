package com.example.flowlab.bridge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ConsumerProducerMain {

    public static void main(String[] args) throws Exception {
        String host = getenv("MQTT_HOST", "localhost");
        String port = getenv("MQTT_PORT", "1883");
        String inTopic = getenv("IN_TOPIC", "lab/flow/in");
        String outTopic = getenv("OUT_TOPIC", "lab/flow/out");
        int dropEveryN = Integer.parseInt(getenv("DROP_EVERY_N", "0"));

        String brokerUrl = "tcp://" + host + ":" + port;
        ObjectMapper mapper = new ObjectMapper();
        MqttClient client = new MqttClient(brokerUrl, "flow-lab-consumer-producer-" + UUID.randomUUID());

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("Consumer-producer connection lost: " + (cause == null ? "unknown" : cause.getMessage()));
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Map<String, Object> input = mapper.readValue(
                        new String(message.getPayload(), StandardCharsets.UTF_8),
                        new TypeReference<>() {}
                );

                long sequence = asLong(input.get("sequence"));
                String traceId = String.valueOf(input.get("traceId"));

                if (dropEveryN > 0 && sequence % dropEveryN == 0) {
                    System.out.println("dropped traceId=" + traceId + " seq=" + sequence);
                    return;
                }

                Map<String, Object> output = new LinkedHashMap<>();
                output.put("traceId", traceId);
                output.put("sequence", sequence);
                output.put("step", "consumer-producer");
                output.put("timestamp", Instant.now().toString());
                output.put("message", "forwarded by consumer-producer");

                byte[] bytes = mapper.writeValueAsString(output).getBytes(StandardCharsets.UTF_8);
                client.publish(outTopic, new MqttMessage(bytes));

                System.out.println("forwarded traceId=" + traceId + " seq=" + sequence);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        client.connect();
        client.subscribe(inTopic);

        System.out.println("Consumer-producer connected to " + brokerUrl + " and subscribed to " + inTopic);

        while (true) {
            Thread.sleep(60_000);
        }
    }

    private static String getenv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
