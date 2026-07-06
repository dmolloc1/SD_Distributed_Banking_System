package pe.unsa.sd.gateway.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class GatewayOperationController {

    private final Map<String, String> bankUrls;
    private final String coordinatorUrl;
    private final WebClient webClient;

    @Configuration
    static class WebClientConfig {
        @Bean
        @LoadBalanced
        public WebClient.Builder loadBalancedWebClientBuilder() {
            return WebClient.builder();
        }
    }

    public GatewayOperationController(
            WebClient.Builder webClientBuilder,
            @Value("${gateway.services.bank-a-url}") String bankAUrl,
            @Value("${gateway.services.bank-b-url}") String bankBUrl,
            @Value("${gateway.services.bank-c-url}") String bankCUrl,
            @Value("${gateway.services.coordinator-url}") String coordinatorUrl) {
        this.bankUrls = Map.of(
                "BANK_A", bankAUrl,
                "BANK_B", bankBUrl,
                "BANK_C", bankCUrl);
        this.coordinatorUrl = coordinatorUrl;
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/api/customers/{customerId}/accounts")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getCustomerAccounts(@PathVariable String customerId) {
        return Flux.fromIterable(bankUrls.values())
                .flatMap(bankUrl -> webClient.get()
                        .uri(bankUrl + "/clients/" + customerId + "/accounts")
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                        .onErrorReturn(List.of()))
                .flatMap(Flux::fromIterable)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/api/operations/deposit")
    public Mono<ResponseEntity<Map<String, Object>>> deposit(@RequestBody Map<String, Object> payload) {
        String accountId = getString(payload, "targetAccountId");
        String amount = getString(payload, "amount");

        if (accountId == null || amount == null) {
            return badRequest("INVALID_REQUEST");
        }

        String bankId = extractBankFromAccount(accountId);
        String bankUrl = bankId == null ? null : bankUrls.get(bankId);
        if (bankUrl == null) {
            return badRequest("INVALID_ACCOUNT");
        }

        return executeBankOperation(bankUrl + "/api/v1/bank/accounts/" + accountId + "/credit", amount);
    }

    @PostMapping("/api/operations/withdraw")
    public Mono<ResponseEntity<Map<String, Object>>> withdraw(@RequestBody Map<String, Object> payload) {
        String accountId = getString(payload, "sourceAccountId");
        String amount = getString(payload, "amount");

        if (accountId == null || amount == null) {
            return badRequest("INVALID_REQUEST");
        }

        String bankId = extractBankFromAccount(accountId);
        String bankUrl = bankId == null ? null : bankUrls.get(bankId);
        if (bankUrl == null) {
            return badRequest("INVALID_ACCOUNT");
        }

        return executeBankOperation(bankUrl + "/api/v1/bank/accounts/" + accountId + "/debit", amount);
    }

    @PostMapping("/api/transfers")
    public Mono<ResponseEntity<Map<String, Object>>> transfer(@RequestBody Map<String, Object> payload) {
        String sourceAccountId = getString(payload, "sourceAccountId");
        String targetAccountId = getString(payload, "targetAccountId");
        String sourceBankId = extractBankFromAccount(sourceAccountId);
        String destinationBankId = extractBankFromAccount(targetAccountId);
        String amount = getString(payload, "amount");

        if (sourceAccountId == null
                || targetAccountId == null
                || sourceBankId == null
                || destinationBankId == null
                || amount == null) {
            return badRequest("INVALID_REQUEST");
        }

        Map<String, Object> transferRequest = Map.of(
                "originAccountId", sourceAccountId,
                "originBankId", sourceBankId,
                "destinationAccountId", targetAccountId,
                "destinationBankId", destinationBankId,
                "amount", new BigDecimal(amount),
                "currency", "USD");

        return forwardPost(coordinatorUrl + "/api/v1/orchestrator/transfers", transferRequest);
    }

    @GetMapping("/api/transactions/{transactionId}")
    public Mono<ResponseEntity<Map<String, Object>>> getTransactionStatus(@PathVariable String transactionId) {
        return webClient.get()
                .uri(coordinatorUrl + "/api/v1/orchestrator/transfers/" + transactionId)
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

        return forwardPost(url, body);
    }

    private Mono<ResponseEntity<Map<String, Object>>> forwardPost(String url, Map<String, Object> body) {
        return webClient.post()
                .uri(url)
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {}))
                .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", e.getMessage()))));
    }

    private String extractBankFromAccount(String accountId) {
        if (accountId == null) {
            return null;
        }
        if (accountId.startsWith("A-")) {
            return "BANK_A";
        }
        if (accountId.startsWith("B-")) {
            return "BANK_B";
        }
        if (accountId.startsWith("C-")) {
            return "BANK_C";
        }
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private Mono<ResponseEntity<Map<String, Object>>> badRequest(String error) {
        return Mono.just(ResponseEntity.badRequest().body(Map.of("error", error)));
    }
}
