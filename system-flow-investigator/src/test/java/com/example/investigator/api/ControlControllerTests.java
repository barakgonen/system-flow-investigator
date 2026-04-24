package com.example.investigator.api;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.service.InvestigatorFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ControlControllerTests {
    @Mock
    private InvestigatorFacade facade;
    private ControlController controlController;

    @BeforeEach
    public void setUp() {
        controlController = new ControlController(facade);
    }

    @Test
    public void testConnectMqttEndpoint() {
        // == Arrange
        ConnectMqttRequest connectMqttRequest = Mockito.mock(ConnectMqttRequest.class);

        // == Act
        controlController.connectMqtt(connectMqttRequest);

        // == Assert
        Mockito.verify(facade).connectMqtt(connectMqttRequest);
    }

    @Test
    public void testSubscribeMqttEndpoint() {
        // == Arrange
        SubscribeMqttRequest subscribeMqttRequest = Mockito.mock(SubscribeMqttRequest.class);

        // == Act
        controlController.subscribeMqtt(subscribeMqttRequest);

        // == Assert
        Mockito.verify(facade).subscribeMqtt(subscribeMqttRequest);
    }

    @Test
    public void testConnectWsEndpoint() {
        // == Arrange
        ConnectWebSocketRequest connectWebSocketRequest = Mockito.mock(ConnectWebSocketRequest.class);

        // == Act
        controlController.connectWebSocket(connectWebSocketRequest);

        // == Assert
        Mockito.verify(facade).connectWebSocket(connectWebSocketRequest);
    }

    @Test
    public void testSubscribeWsEndpoint() {
        // == Arrange
        SubscribeWebSocketRequest subscribeWsRequest = Mockito.mock(SubscribeWebSocketRequest.class);

        // == Act
        controlController.subscribeWebSocket(subscribeWsRequest);

        // == Assert
        Mockito.verify(facade).subscribeWebSocket(subscribeWsRequest);
    }
}
