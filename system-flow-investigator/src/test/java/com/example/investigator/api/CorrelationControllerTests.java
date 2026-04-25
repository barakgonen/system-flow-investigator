package com.example.investigator.api;

import com.example.investigator.domain.TraceTimelineResponse;
import com.example.investigator.service.CorrelationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationControllerTests {

    @Test
    void shouldDelegateTraceLookupToService() {
        CorrelationService service = mock(CorrelationService.class);
        TraceTimelineResponse response = new TraceTimelineResponse(
                "trace-1",
                0,
                null,
                null,
                null,
                null,
                List.of()
        );

        when(service.trace("trace-1")).thenReturn(response);

        CorrelationController controller = new CorrelationController(service);

        TraceTimelineResponse result = controller.trace("trace-1");

        assertThat(result).isSameAs(response);
        verify(service).trace("trace-1");
    }
}