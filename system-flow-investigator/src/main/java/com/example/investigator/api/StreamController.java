package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.service.InvestigatorFacade;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final InvestigatorFacade facade;

    public StreamController(InvestigatorFacade facade) {
        this.facade = facade;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ObservedEvent> streamEvents(
            @RequestParam(name = "protocol", required = false) String protocol,
            @RequestParam(name = "channelContains", required = false) String channelContains,
            @RequestParam(name = "textContains", required = false) String textContains,
            @RequestParam(name = "traceId", required = false) String traceId
    ) {
        return facade.streamEvents()
                .filter(event -> protocol == null || event.protocol().equalsIgnoreCase(protocol))
                .filter(event -> channelContains == null || event.channel().contains(channelContains))
                .filter(event -> textContains == null || event.payload().contains(textContains))
                .filter(event -> traceId == null || traceId.equals(event.traceId()));
    }
}