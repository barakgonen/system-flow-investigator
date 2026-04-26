package com.example.investigator.service;

import com.example.investigator.domain.config.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class InvestigationConfigService {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path configPath = Path.of("data", "investigation-config.json");

    public InvestigationConfig getConfig() {
        try {
            if (!Files.exists(configPath)) {
                InvestigationConfig defaultConfig = defaultConfig();
                saveConfig(defaultConfig);
                return defaultConfig;
            }

            return objectMapper.readValue(configPath.toFile(), InvestigationConfig.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed loading investigation config", e);
        }
    }

    public InvestigationConfig saveConfig(InvestigationConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), config);
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("Failed saving investigation config", e);
        }
    }

    private InvestigationConfig defaultConfig() {
        return new InvestigationConfig(
                "Default Investigation",
                "Default validation flows",
                List.of(
                        new FlowDefinition(
                                "main-lab-flow",
                                "Main Lab Flow",
                                "Producer to consumer-producer to websocket delivery",
                                List.of(
                                        new ExpectedFlowStep(1, "MQTT", "lab/flow/in", "Producer published"),
                                        new ExpectedFlowStep(2, "MQTT", "lab/flow/out", "Consumer-producer forwarded"),
                                        new ExpectedFlowStep(3, "WS", "ws/live/out", "WebSocket delivered")
                                )
                        )
                ),
                new FlowValidationRules(50, true)
        );
    }
}