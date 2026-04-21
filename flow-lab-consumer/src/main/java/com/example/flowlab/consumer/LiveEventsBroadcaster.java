package com.example.flowlab.consumer;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveEventsBroadcaster {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public void broadcast(String payload) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                } else {
                    sessions.remove(session);
                }
            } catch (Exception e) {
                sessions.remove(session);
                try {
                    session.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public int sessionCount() {
        return sessions.size();
    }
}