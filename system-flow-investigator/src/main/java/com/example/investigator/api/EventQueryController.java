package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.service.InvestigatorFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/events")
public class EventQueryController {

    private final InvestigatorFacade facade;

    public EventQueryController(InvestigatorFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/recent")
    public List<ObservedEvent> recent(
            @RequestParam(name = "channel", required = false) List<String> channel,
            @RequestParam(name = "textContains", required = false) String textContains,
            @RequestParam(name = "traceId", required = false) String traceId
    ) {
        Set<String> channels = channel == null ? Set.of() : new LinkedHashSet<>(channel);

        if (channels.size() == 1 && (textContains == null || textContains.isBlank()) && (traceId == null || traceId.isBlank())) {
            return facade.getRecentEvents(channels.iterator().next());
        }

        return facade.getRecentEvents(channels, textContains, traceId);
    }

    @GetMapping("/mqtt/topics")
    public Set<String> mqttTopics() {
        return facade.observedMqttChannels();
    }

    @GetMapping("/ws/channels")
    public Set<String> wsChannels() {
        return facade.observedWebSocketChannels();
    }
}