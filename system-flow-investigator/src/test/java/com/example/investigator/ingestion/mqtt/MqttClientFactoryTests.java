package com.example.investigator.ingestion.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttClientFactoryTests {

    private final MqttClientFactory factory = new MqttClientFactory();

    @Test
    void shouldCreateMqttClientWithGivenBrokerUrlAndClientId() throws Exception {
        MqttClient client = factory.create(
                "tcp://localhost:1883",
                "test-client"
        );

        assertThat(client).isNotNull();
        assertThat(client.getClientId()).isEqualTo("test-client");
        assertThat(client.getServerURI()).isEqualTo("tcp://localhost:1883");

        client.close();
    }
}