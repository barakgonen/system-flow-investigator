package com.example.investigator.api;

import com.example.investigator.domain.DashboardSummary;
import com.example.investigator.service.InvestigatorFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTests {

    @Mock
    private InvestigatorFacade facade;
    private DashboardController dashboardController;

    @BeforeEach
    public void setUp() {
        dashboardController = new DashboardController(facade);
    }

    @Test
    public void testSummaryEndpoint() {
        // == Arrange
        DashboardSummary dashboardSummary = Mockito.mock(DashboardSummary.class);
        Mockito.when(facade.getDashboardSummary()).thenReturn(dashboardSummary);

        // == Act
        dashboardController.summary();

        // == Assert
        Mockito.verify(facade).getDashboardSummary();
    }
}