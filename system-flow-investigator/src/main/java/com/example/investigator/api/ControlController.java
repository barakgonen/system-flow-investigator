package com.example.investigator.api;

import com.example.investigator.domain.ConnectMqttRequest;
import com.example.investigator.domain.ConnectWebSocketRequest;
import com.example.investigator.domain.SubscribeMqttRequest;
import com.example.investigator.domain.SubscribeWebSocketRequest;
import com.example.investigator.service.InvestigatorFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/control")
public class ControlController {

    private final InvestigatorFacade facade;

    public ControlController(InvestigatorFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/mqtt/connect")
    public ResponseEntity<Void> connectMqtt(@RequestBody ConnectMqttRequest request) {
        facade.connectMqtt(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mqtt/subscribe")
    public ResponseEntity<Void> subscribeMqtt(@RequestBody SubscribeMqttRequest request) {
        facade.subscribeMqtt(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ws/connect")
    public ResponseEntity<Void> connectWebSocket(@RequestBody ConnectWebSocketRequest request) {
        facade.connectWebSocket(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ws/subscribe")
    public ResponseEntity<Void> subscribeWebSocket(@RequestBody SubscribeWebSocketRequest request) {
        facade.subscribeWebSocket(request);
        return ResponseEntity.ok().build();
    }
}