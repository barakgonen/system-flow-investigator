//package com.example.flowlab.consumer;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.eclipse.paho.client.mqttv3.*;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Map;
//import java.util.UUID;
//
//public class ConsumerMain {
//
//    public static void main(String[] args) throws Exception {
//        String host = getenv("MQTT_HOST", "localhost");
//        String port = getenv("MQTT_PORT", "1883");
//        String inTopic = getenv("IN_TOPIC", "lab/flow/out");
//
//        String brokerUrl = "tcp://" + host + ":" + port;
//        ObjectMapper mapper = new ObjectMapper();
//        MqttClient client = new MqttClient(brokerUrl, "flow-lab-consumer-" + UUID.randomUUID());
//
//        client.setCallback(new MqttCallback() {
//            @Override
//            public void connectionLost(Throwable cause) {
//                System.err.println("Consumer connection lost: " + (cause == null ? "unknown" : cause.getMessage()));
//            }
//
//            @Override
//            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                Map<String, Object> input = mapper.readValue(
//                        new String(message.getPayload(), StandardCharsets.UTF_8),
//                        new TypeReference<>() {}
//                );
//
//                System.out.println(
//                        "consumed traceId=" + input.get("traceId") +
//                        " seq=" + input.get("sequence") +
//                        " step=" + input.get("step")
//                );
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken token) {
//            }
//        });
//
//        client.connect();
//        client.subscribe(inTopic);
//
//        System.out.println("Consumer connected to " + brokerUrl + " and subscribed to " + inTopic);
//
//        while (true) {
//            Thread.sleep(60_000);
//        }
//    }
//
//    private static String getenv(String key, String defaultValue) {
//        String value = System.getenv(key);
//        return value == null || value.isBlank() ? defaultValue : value;
//    }
//}
