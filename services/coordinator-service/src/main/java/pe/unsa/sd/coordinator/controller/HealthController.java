package pe.unsa.sd.coordinator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.coordinator.dto.HealthResponse;

@RestController
public class HealthController {

    @Value("${spring.application.name}")
    private String service;

    @Value("${server.port}")
    private int port;

    @GetMapping("/distributed/health")
    public HealthResponse health() {
        return new HealthResponse(service, "UP", port);
    }
}
