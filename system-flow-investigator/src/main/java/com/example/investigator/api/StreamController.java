package com.example.investigator.api;

import com.example.investigator.domain.ObservedEvent;
import com.example.investigator.service.InvestigatorFacade;
import com.example.investigator.service.SseStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final SseStreamService sseStreamService;

    public StreamController(SseStreamService sseStreamService) {
        this.sseStreamService = sseStreamService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @RequestParam(name = "protocol", required = false) String protocol,
            @RequestParam(name = "channelContains", required = false) String channelContains,
            @RequestParam(name = "textContains", required = false) String textContains,
            @RequestParam(name = "traceId", required = false) String traceId
    ) {
        return sseStreamService.openStream(protocol, channelContains, textContains, traceId);
    }
}