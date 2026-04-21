package com.example.investigator.ingestion;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractIngestionSource<C, S> implements IngestionSource<C, S> {

    protected final Set<String> observedChannels = ConcurrentHashMap.newKeySet();
    protected final Map<String, Boolean> persistByChannel = new ConcurrentHashMap<>();

    @Override
    public Set<String> observedChannels() {
        return observedChannels;
    }

    protected boolean shouldPersist(String channel) {
        return persistByChannel.getOrDefault(channel, false);
    }

    protected void markObserved(String channel) {
        if (channel != null && !channel.isBlank()) {
            observedChannels.add(channel);
        }
    }

    protected void registerPersistence(String channel, boolean persist) {
        if (channel != null && !channel.isBlank()) {
            persistByChannel.put(channel, persist);
        }
    }
}