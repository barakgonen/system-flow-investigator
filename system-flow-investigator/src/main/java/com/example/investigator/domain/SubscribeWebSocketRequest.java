package com.example.investigator.domain;

public record SubscribeWebSocketRequest(
        String connectionName,
        String logicalChannel,
        boolean persistToFile
) {
}