package com.example.investigator.ingestion.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Component;

@Component
public class MqttClientFactory {

    public MqttClient create(String brokerUrl, String clientId) throws MqttException {
        return new MqttClient(brokerUrl, clientId);
    }
}