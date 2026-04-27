package com.example.investigator.ingestion;

import java.util.List;

public interface IngestionSource<C, S> {

    String id();

    String displayName();

    String protocol();

    void connect(C connectRequest);

    void subscribe(S subscribeRequest);

    void disconnect();

    IngestionSourceStatus status();

    List<String> observedChannels();
}