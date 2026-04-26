package com.example.investigator.api;

import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.domain.config.FlowValidationResult;
import com.example.investigator.service.CorrelationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationControllerTests {

    @Test
    void shouldDelegateTraceLookupToServiceWithoutFlowId() {
        CorrelationService service = mock(CorrelationService.class);

        FlowValidationResult validation = null;

        TraceTimelineResponse response = new TraceTimelineResponse(
                "trace-1",
                0,
                null,
                null,
                null,
                null,
                List.of(),
                validation
        );

        when(service.trace("trace-1", null)).thenReturn(response);

        CorrelationController controller = new CorrelationController(service);

        TraceTimelineResponse result = controller.trace("trace-1", null);

        assertThat(result).isSameAs(response);
        verify(service).trace("trace-1", null);
    }

    @Test
    void shouldDelegateTraceLookupToServiceWithFlowId() {
        CorrelationService service = mock(CorrelationService.class);

        FlowValidationResult validation = new FlowValidationResult(
                "COMPLETE",
                "Flow completed successfully.",
                List.of(),
                List.of(),
                List.of(),
                null
        );

        TraceTimelineResponse response = new TraceTimelineResponse(
                "trace-1",
                0,
                null,
                null,
                null,
                null,
                List.of(),
                validation
        );

        when(service.trace("trace-1", "main-lab-flow")).thenReturn(response);

        CorrelationController controller = new CorrelationController(service);

        TraceTimelineResponse result = controller.trace("trace-1", "main-lab-flow");

        assertThat(result).isSameAs(response);
        verify(service).trace("trace-1", "main-lab-flow");
    }
}