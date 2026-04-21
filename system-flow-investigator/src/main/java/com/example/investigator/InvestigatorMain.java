package com.example.investigator;

import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class InvestigatorMain {

    public static void main(String[] args) throws Exception {
        String host = getenv("MQTT_HOST", "localhost");
        String port = getenv("MQTT_PORT", "1883");
        String topicFilter = getenv("MQTT_TOPIC_FILTER", "lab/flow/#");
        String brokerUrl = "tcp://" + host + ":" + port;

        MqttClient client = new MqttClient(brokerUrl, "system-flow-investigator-" + UUID.randomUUID());

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("Investigator connection lost: " + (cause == null ? "unknown" : cause.getMessage()));
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                System.out.println("observed topic=" + topic + " payload=" + payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        client.connect();
        client.subscribe(topicFilter);

        System.out.println("Investigator connected to " + brokerUrl + " and subscribed to " + topicFilter);

        while (true) {
            Thread.sleep(60_000);
        }
    }

    private static String getenv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
