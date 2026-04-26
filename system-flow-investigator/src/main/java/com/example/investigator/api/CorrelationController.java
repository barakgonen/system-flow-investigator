package com.example.investigator.api;

import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.service.CorrelationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/correlation")
public class CorrelationController {

    private final CorrelationService correlationService;

    public CorrelationController(CorrelationService correlationService) {
        this.correlationService = correlationService;
    }

    @GetMapping("/trace/{traceId}")
    public TraceTimelineResponse trace(@PathVariable("traceId") String traceId,
                                       @RequestParam(required = false) String flowId) {
        return correlationService.trace(traceId, flowId);
    }
}