package com.example.investigator.websocket;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;

import java.util.Set;

public interface WebSocketObserver {

    void connect(ConnectWebSocketRequest request);

    void subscribe(SubscribeWebSocketRequest request);

    Set<String> observedChannels();
}