package com.example.investigator.ingestion;

import java.util.Set;

public interface IngestionSource<C, S> {

    String sourceType();

    void connect(C connectRequest);

    void subscribe(S subscribeRequest);

    Set<String> observedChannels();
}