package com.example.investigator.websocket;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("stub")
public class StubWebSocketObserver implements WebSocketObserver {

    private final Set<String> channels = ConcurrentHashMap.newKeySet();

    @Override
    public void connect(ConnectWebSocketRequest request) {
    }

    @Override
    public void subscribe(SubscribeWebSocketRequest request) {
        channels.add(request.logicalChannel());
    }

    @Override
    public Set<String> observedChannels() {
        return channels;
    }
}