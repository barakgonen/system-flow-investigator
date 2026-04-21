package com.example.investigator.domain;

public record ConnectMqttRequest(
        String connectionName,
        String host,
        int port,
        String clientId,
        String username,
        String password
) {
}