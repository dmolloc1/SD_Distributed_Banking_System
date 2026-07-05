package pe.unsa.sd.gateway.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class PendingGatewayOperationController {

    private static final Map<String, String> BANK_URLS = Map.of(
            "BANK_A", "http://localhost:8081",
            "BANK_B", "http://localhost:8082",
            "BANK_C", "http://localhost:8083");

    private static final String COORDINATOR_URL = "http://localhost:8090";

    private final WebClient webClient;

    public PendingGatewayOperationController() {
        this.webClient = WebClient.create();
    }

    @PostMapping("/api/operations/deposit")
    public Mono<ResponseEntity<Map<String, Object>>> deposit(@RequestBody Map<String, Object> payload) {
        String accountId = getString(payload, "targetAccountId");
        String amount = getString(payload, "amount");

        if (accountId == null || amount == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST")));
        }

        String bankId = extractBankFromAccount(accountId);
        String bankUrl = BANK_URLS.get(bankId);
        if (bankUrl == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "INVALID_ACCOUNT")));
        }

        return executeBankOperation(bankUrl + "/api/v1/bank/accounts/" + accountId + "/credit", amount);
    }

    @PostMapping("/api/operations/withdraw")
    public Mono<ResponseEntity<Map<String, Object>>> withdraw(@RequestBody Map<String, Object> payload) {
        String accountId = getString(payload, "sourceAccountId");
        String amount = getString(payload, "amount");

        if (accountId == null || amount == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST")));
        }

        String bankId = extractBankFromAccount(accountId);
        String bankUrl = BANK_URLS.get(bankId);
        if (bankUrl == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "INVALID_ACCOUNT")));
        }

        return executeBankOperation(bankUrl + "/api/v1/bank/accounts/" + accountId + "/debit", amount);
    }

    @PostMapping("/api/transfers")
    public Mono<ResponseEntity<Map<String, Object>>> transfer(@RequestBody Map<String, Object> payload) {
        String sourceAccountId = getString(payload, "sourceAccountId");
        String targetAccountId = getString(payload, "targetAccountId");
        String sourceBankId = getString(payload, "accessBank");
        String amount = getString(payload, "amount");
        String destinationBankId = extractBankFromAccount(targetAccountId);

        if (sourceAccountId == null || targetAccountId == null || sourceBankId == null || amount == null || destinationBankId == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "INVALID_REQUEST")));
        }

        Map<String, Object> transferRequest = Map.of(
                "originAccountId", sourceAccountId,
                "originBankId", sourceBankId,
                "destinationAccountId", targetAccountId,
                "destinationBankId", destinationBankId,
                "amount", new BigDecimal(amount),
                "currency", "USD");

        return webClient.post()
                .uri(URI.create(COORDINATOR_URL + "/api/v1/orchestrator/transfers"))
                .bodyValue(transferRequest)
                .exchangeToMono(response -> response.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {}))
                .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()))));
    }

    private Mono<ResponseEntity<Map<String, Object>>> executeBankOperation(String url, String amount) {
        Map<String, Object> body = Map.of(
                "transactionId", java.util.UUID.randomUUID().toString(),
                "amount", new BigDecimal(amount),
                "currency", "USD");

        return webClient.post()
                .uri(URI.create(url))
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {}))
                .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()))));
    }

    private String extractBankFromAccount(String accountId) {
        if (accountId == null) return null;
        if (accountId.startsWith("A-")) return "BANK_A";
        if (accountId.startsWith("B-")) return "BANK_B";
        if (accountId.startsWith("C-")) return "BANK_C";
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }
}
