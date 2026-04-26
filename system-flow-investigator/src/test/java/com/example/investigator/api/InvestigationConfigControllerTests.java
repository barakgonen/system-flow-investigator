package com.example.investigator.api;

import com.example.investigator.domain.config.*;
import com.example.investigator.service.InvestigationConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvestigationConfigControllerTests {

    @Test
    void shouldGetConfigFromService() {
        InvestigationConfigService service = mock(InvestigationConfigService.class);
        InvestigationConfig config = config();

        when(service.getConfig()).thenReturn(config);

        InvestigationConfigController controller = new InvestigationConfigController(service);

        assertThat(controller.getConfig()).isSameAs(config);
        verify(service).getConfig();
    }

    @Test
    void shouldSaveConfigUsingService() {
        InvestigationConfigService service = mock(InvestigationConfigService.class);
        InvestigationConfig config = config();

        when(service.saveConfig(config)).thenReturn(config);

        InvestigationConfigController controller = new InvestigationConfigController(service);

        assertThat(controller.saveConfig(config)).isSameAs(config);
        verify(service).saveConfig(config);
    }

    private InvestigationConfig config() {
        return new InvestigationConfig(
                "Main Investigation",
                "desc",
                List.of(
                        new FlowDefinition(
                                "main-lab-flow",
                                "Main Lab Flow",
                                "Producer to websocket flow",
                                List.of(
                                        new ExpectedFlowStep(
                                                1,
                                                "MQTT",
                                                "lab/flow/in",
                                                "Producer"
                                        )
                                )
                        )
                ),
                new FlowValidationRules(50, true)
        );
    }
}