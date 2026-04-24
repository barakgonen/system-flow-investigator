package com.example.investigator.ingestion.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.ingestion.infra.IngestionSource;

public interface MqttObserver extends IngestionSource<ConnectMqttRequest, SubscribeMqttRequest> {
}