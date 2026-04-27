package com.example.investigator.ingestion.ws;

import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.ingestion.infra.IngestionSource;

public interface WebSocketObserver extends IngestionSource<ConnectWebSocketRequest, SubscribeWebSocketRequest> {
}