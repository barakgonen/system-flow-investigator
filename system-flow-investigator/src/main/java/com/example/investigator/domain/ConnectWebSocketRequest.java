package com.example.investigator.domain;

public record ConnectWebSocketRequest(
        String connectionName,
        String url
) {
}