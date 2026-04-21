package com.example.investigator.domain;

public record SubscribeMqttRequest(
        String connectionName,
        String topicFilter,
        boolean persistToFile
) {
}