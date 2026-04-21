package com.example.investigator.api;

import com.example.investigator.service.SseStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final SseStreamService sseStreamService;

    public StreamController(SseStreamService sseStreamService) {
        this.sseStreamService = sseStreamService;
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(
            @RequestParam(name = "channel", required = false) List<String> channel,
            @RequestParam(name = "textContains", required = false) String textContains,
            @RequestParam(name = "traceId", required = false) String traceId
    ) {
        Set<String> channels = channel == null ? Set.of() : new LinkedHashSet<>(channel);

        return sseStreamService.openStream(channels, textContains, traceId);
    }
}