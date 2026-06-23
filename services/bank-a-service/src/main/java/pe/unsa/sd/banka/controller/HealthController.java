package pe.unsa.sd.banka.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.banka.model.HealthResponse;

@RestController
public class HealthController {

    @Value("${spring.application.name}")
    private String service;

    @Value("${bank.code}")
    private String bankCode;

    @Value("${server.port}")
    private int port;

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(service, bankCode, "UP", port);
    }
}
