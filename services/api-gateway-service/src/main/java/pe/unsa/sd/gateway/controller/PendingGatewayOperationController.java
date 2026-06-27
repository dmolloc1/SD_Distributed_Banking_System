package pe.unsa.sd.gateway.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PendingGatewayOperationController {

    @PostMapping("/api/operations/deposit")
    public ResponseEntity<Map<String, Object>> depositPlaceholder() {
        return pending("deposit");
    }

    @PostMapping("/api/operations/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawPlaceholder() {
        return pending("withdraw");
    }

    @PostMapping("/api/transfers")
    public ResponseEntity<Map<String, Object>> transferPlaceholder() {
        return pending("transfer");
    }

    @GetMapping("/api/transactions/{transactionId}")
    public ResponseEntity<Map<String, Object>> transactionStatusPlaceholder(@PathVariable String transactionId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "status", "PENDING",
                        "operation", "transaction-status",
                        "transactionId", transactionId,
                        "message", "Endpoint prepared in API Gateway. Coordinator implementation pending.",
                        "timestamp", Instant.now().toString()));
    }

    private ResponseEntity<Map<String, Object>> pending(String operation) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "status", "PENDING",
                        "operation", operation,
                        "message", "Endpoint prepared in API Gateway. Coordinator implementation pending.",
                        "timestamp", Instant.now().toString()));
    }
}
