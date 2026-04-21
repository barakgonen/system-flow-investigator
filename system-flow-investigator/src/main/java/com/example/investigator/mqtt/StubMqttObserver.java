package com.example.investigator.mqtt;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.SubscribeMqttRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("stub")
public class StubMqttObserver implements MqttObserver {

    private final Set<String> topics = ConcurrentHashMap.newKeySet();

    @Override
    public void connect(ConnectMqttRequest request) {
        // phase 1 stub
    }

    @Override
    public void subscribe(SubscribeMqttRequest request) {
        topics.add(request.topicFilter());
    }

    @Override
    public Set<String> observedTopics() {
        return topics;
    }
}