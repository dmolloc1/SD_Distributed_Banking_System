package pe.unsa.sd.gateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GatewayOperationControllerTest {

    private HttpServer coordinatorServer;
    private HttpServer bankBServer;
    private GatewayOperationController controller;
    private AtomicReference<String> coordinatorRequestBody;
    private AtomicReference<String> bankBRequestBody;

    @BeforeEach
    void setUp() throws IOException {
        coordinatorRequestBody = new AtomicReference<>();
        bankBRequestBody = new AtomicReference<>();

        coordinatorServer = startServer(exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, "{\"transactionId\":\"tx-1\",\"status\":\"COMMITTED\"}");
                return;
            }
            coordinatorRequestBody.set(readBody(exchange));
            respond(exchange, 202, "{\"transactionId\":\"tx-1\",\"status\":\"PENDING\"}");
        });

        bankBServer = startServer(exchange -> {
            bankBRequestBody.set(readBody(exchange));
            respond(exchange, 200, "{\"status\":\"SUCCESS\",\"accountId\":\"B-2001\"}");
        });

        controller = new GatewayOperationController(
                "http://127.0.0.1:1",
                "http://127.0.0.1:" + bankBServer.getAddress().getPort(),
                "http://127.0.0.1:2",
                "http://127.0.0.1:" + coordinatorServer.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        coordinatorServer.stop(0);
        bankBServer.stop(0);
    }

    @Test
    void transferDerivesOriginAndDestinationBanksFromAccountPrefixes() {
        Map<String, Object> payload = Map.of(
                "accessBank", "BANK_A",
                "sourceAccountId", "B-2001",
                "targetAccountId", "C-3001",
                "amount", "25.00");

        ResponseEntity<Map<String, Object>> response = controller.transfer(payload).block();

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        String body = coordinatorRequestBody.get();
        assertTrue(body.contains("\"originBankId\":\"BANK_B\""));
        assertTrue(body.contains("\"destinationBankId\":\"BANK_C\""));
        assertTrue(body.contains("\"originAccountId\":\"B-2001\""));
    }

    @Test
    void getTransactionStatusForwardsRequestToCoordinator() {
        ResponseEntity<Map<String, Object>> response = controller.getTransactionStatus("tx-1").block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("COMMITTED", response.getBody().get("status"));
    }

    @Test
    void withdrawRoutesToBankSelectedByAccountPrefix() {
        Map<String, Object> payload = Map.of(
                "sourceAccountId", "B-2001",
                "amount", "10.00");

        ResponseEntity<Map<String, Object>> response = controller.withdraw(payload).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(bankBRequestBody.get().contains("\"amount\":10.00"));
    }

    @Test
    void depositRejectsUnknownAccountPrefix() {
        ResponseEntity<Map<String, Object>> response = controller.deposit(Map.of(
                "targetAccountId", "X-9001",
                "amount", "10.00")).block();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_ACCOUNT", response.getBody().get("error"));
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler::handle);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        return server;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
