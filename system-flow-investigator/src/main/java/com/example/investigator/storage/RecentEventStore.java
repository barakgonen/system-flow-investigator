package com.example.investigator.storage;

import com.example.investigator.domain.ObservedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RecentEventStore {

    private final ConcurrentMap<String, Deque<ObservedEvent>> store = new ConcurrentHashMap<>();
    private final int maxPerChannel = 10_000;

    public void add(ObservedEvent event) {
        store.compute(event.channel(), (key, queue) -> {
            Deque<ObservedEvent> deque = (queue == null) ? new ArrayDeque<>() : queue;
            if (deque.size() >= maxPerChannel) {
                deque.removeFirst();
            }
            deque.addLast(event);
            return deque;
        });
    }

    public List<ObservedEvent> getRecent(String channel) {
        Deque<ObservedEvent> deque = store.get(channel);
        return deque == null ? List.of() : new ArrayList<>(deque);
    }

    public Set<String> channels() {
        return store.keySet();
    }

    public List<ObservedEvent> getAllRecent() {
        List<ObservedEvent> result = new ArrayList<>();
        for (Deque<ObservedEvent> deque : store.values()) {
            result.addAll(deque);
        }
        result.sort(Comparator.comparing(ObservedEvent::observedAt));
        return result;
    }
}