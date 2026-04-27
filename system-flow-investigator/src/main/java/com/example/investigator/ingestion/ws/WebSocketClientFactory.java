package com.example.investigator.ingestion.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Component
public class WebSocketClientFactory {

    public WebSocketClient create() {
        return new StandardWebSocketClient();
    }
}