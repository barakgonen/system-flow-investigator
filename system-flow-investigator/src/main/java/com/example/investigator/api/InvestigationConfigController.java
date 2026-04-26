package com.example.investigator.api;

import com.example.investigator.domain.config.InvestigationConfig;
import com.example.investigator.service.InvestigationConfigService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investigation/config")
public class InvestigationConfigController {

    private final InvestigationConfigService configService;

    public InvestigationConfigController(InvestigationConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public InvestigationConfig getConfig() {
        return configService.getConfig();
    }

    @PutMapping
    public InvestigationConfig saveConfig(@RequestBody InvestigationConfig config) {
        return configService.saveConfig(config);
    }
}