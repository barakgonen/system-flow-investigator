package com.example.investigator.service;

import com.example.investigator.domain.config.InvestigationConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class InvestigationConfigServiceTests {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateDefaultConfigWhenFileDoesNotExist() throws Exception {
        InvestigationConfigService service = serviceWithPath(tempDir.resolve("investigation-config.json"));

        InvestigationConfig config = service.getConfig();

        assertThat(config.name()).isEqualTo("Main Lab Flow");
        assertThat(config.steps()).hasSize(3);
        assertThat(config.rules().maxStepDurationMs()).isEqualTo(50);
        assertThat(Files.exists(tempDir.resolve("investigation-config.json"))).isTrue();
    }

    @Test
    void shouldSaveAndLoadConfig() throws Exception {
        Path configPath = tempDir.resolve("investigation-config.json");
        InvestigationConfigService service = serviceWithPath(configPath);

        InvestigationConfig saved = service.saveConfig(new InvestigationConfig(
                "Custom Flow",
                "Custom desc",
                java.util.List.of(
                        new com.example.investigator.domain.config.ExpectedFlowStep(
                                1,
                                "MQTT",
                                "custom/topic",
                                "Custom step"
                        )
                ),
                new com.example.investigator.domain.config.FlowValidationRules(100, false)
        ));

        InvestigationConfig loaded = service.getConfig();

        assertThat(saved.name()).isEqualTo("Custom Flow");
        assertThat(loaded.name()).isEqualTo("Custom Flow");
        assertThat(loaded.steps()).hasSize(1);
        assertThat(loaded.rules().allowExtraEvents()).isFalse();
    }

    @Test
    void shouldThrowWhenConfigFileInvalid() throws Exception {
        Path configPath = tempDir.resolve("investigation-config.json");
        Files.writeString(configPath, "not-json");

        InvestigationConfigService service = serviceWithPath(configPath);

        assertThatThrownBy(service::getConfig)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed loading investigation config");
    }

    private InvestigationConfigService serviceWithPath(Path path) throws Exception {
        InvestigationConfigService service = new InvestigationConfigService();

        Field field = InvestigationConfigService.class.getDeclaredField("configPath");
        field.setAccessible(true);
        field.set(service, path);

        return service;
    }
}