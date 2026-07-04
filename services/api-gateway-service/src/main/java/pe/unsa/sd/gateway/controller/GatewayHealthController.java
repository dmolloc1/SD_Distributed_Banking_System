package pe.unsa.sd.gateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayHealthController {

    @Value("${spring.application.name}")
    private String service;

    @Value("${server.port}")
    private int port;

    @GetMapping("/api/gateway/health")
    public Map<String, Object> health() {
        return Map.of(
                "service", service,
                "status", "UP",
                "port", port,
                "timestamp", Instant.now().toString());
    }
}