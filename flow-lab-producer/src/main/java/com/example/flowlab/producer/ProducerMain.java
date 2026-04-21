package com.example.flowlab.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ProducerMain {

    private static final String TOPIC = "lab/flow/in";

    public static void main(String[] args) throws Exception {
        String host = getenv("MQTT_HOST", "localhost");
        String port = getenv("MQTT_PORT", "1883");
        String brokerUrl = "tcp://" + host + ":" + port;

        ObjectMapper mapper = new ObjectMapper();
        MqttClient client = new MqttClient(brokerUrl, "flow-lab-producer-" + UUID.randomUUID());

        client.connect();
        System.out.println("Producer connected to " + brokerUrl);

        long sequence = 0L;

        while (true) {
            sequence++;

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("traceId", UUID.randomUUID().toString());
            payload.put("sequence", sequence);
            payload.put("step", "producer");
            payload.put("timestamp", Instant.now().toString());
            payload.put("message", "hello from producer");

            byte[] bytes = mapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);
            client.publish(TOPIC, new MqttMessage(bytes));

            System.out.println("published topic=" + TOPIC + " seq=" + sequence);
            Thread.sleep(1000);
        }
    }

    private static String getenv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
