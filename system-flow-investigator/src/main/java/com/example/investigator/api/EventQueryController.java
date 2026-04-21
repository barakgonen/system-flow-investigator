package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.service.InvestigatorFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(name = "channel", required = false) String channel
    ) {
        return facade.getRecentEvents(channel);
    }

    @GetMapping("/mqtt/topics")
    public Set<String> mqttTopics() {
        return facade.observedTopics();
    }

    @GetMapping("/ws/channels")
    public Set<String> wsChannels() {
        return facade.observedWebSocketChannels();
    }
}