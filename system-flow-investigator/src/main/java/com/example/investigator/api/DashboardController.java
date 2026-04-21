package com.example.investigator.api;

import com.example.investigator.domain.DashboardSummary;
import com.example.investigator.service.InvestigatorFacade;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final InvestigatorFacade facade;

    public DashboardController(InvestigatorFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/summary")
    public DashboardSummary summary() {
        return facade.getDashboardSummary();
    }
}