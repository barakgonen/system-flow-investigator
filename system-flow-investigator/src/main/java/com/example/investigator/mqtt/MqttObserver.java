package com.example.investigator.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.SubscribeMqttRequest;

import java.util.Set;

public interface MqttObserver {

    void connect(ConnectMqttRequest request);

    void subscribe(SubscribeMqttRequest request);

    Set<String> observedTopics();
}