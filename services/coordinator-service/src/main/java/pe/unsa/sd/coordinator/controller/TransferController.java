package pe.unsa.sd.coordinator.controller;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.coordinator.dto.SagaTransactionDTO;
import pe.unsa.sd.coordinator.dto.TransferRequest;
import pe.unsa.sd.coordinator.service.SagaOrchestrationService;
import pe.unsa.sd.coordinator.store.SagaTransactionStore;

@RestController
@RequestMapping("/api/v1/orchestrator")
@CrossOrigin(origins = "http://localhost:5173")
public class TransferController {

    private static final Logger log = LoggerFactory.getLogger(TransferController.class);
    private static final Set<String> VALID_BANKS = Set.of("BANK_A", "BANK_B", "BANK_C");

    private final SagaOrchestrationService sagaOrchestrationService;
    private final SagaTransactionStore transactionStore;

    public TransferController(SagaOrchestrationService sagaOrchestrationService,
                              SagaTransactionStore transactionStore) {
        this.sagaOrchestrationService = sagaOrchestrationService;
        this.transactionStore = transactionStore;
    }

    // Inicia una transferencia asincrona. Retorna 202 Accepted con transactionId.
    // El frontend debe hacer polling a GET /api/v1/orchestrator/transfers/{transactionId}
    @PostMapping("/transfers")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest request) {
        if (!VALID_BANKS.contains(request.getOriginBankId()) || !VALID_BANKS.contains(request.getDestinationBankId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_REQUEST", "message", "originBankId o destinationBankId invalido"));
        }

        String transactionId = UUID.randomUUID().toString();

        SagaTransactionDTO pending = new SagaTransactionDTO();
        pending.setTransactionId(transactionId);
        pending.setStatus("PENDING");
        pending.setMessage("Validando solicitud de transferencia...");
        pending.setSourceAccountId(request.getOriginAccountId());
        pending.setSourceBankId(request.getOriginBankId());
        pending.setDestinationAccountId(request.getDestinationAccountId());
        pending.setDestinationBankId(request.getDestinationBankId());
        pending.setAmount(request.getAmount());
        pending.setCurrentStep(0);
        pending.setTotalSteps(2);
        pending.setInitiatedAt(java.time.OffsetDateTime.now());
        transactionStore.put(transactionId, pending);

        // Ejecutar saga en background para no bloquear al frontend
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Iniciando ejecucion asincrona de saga txId={}", transactionId);
                SagaTransactionDTO result = sagaOrchestrationService.executeSaga(request, transactionId);
                transactionStore.put(transactionId, result);
                log.info("Saga completada txId={} status={}", transactionId, result.getStatus());
            } catch (Exception exception) {
                log.error("Error en ejecucion asincrona de saga txId={}", transactionId, exception);
                SagaTransactionDTO error = new SagaTransactionDTO();
                error.setTransactionId(transactionId);
                error.setStatus("ABORTED");
                error.setError(exception.getMessage());
                error.setMessage("Error interno en la transferencia");
                transactionStore.put(transactionId, error);
            }
        });

        return ResponseEntity.accepted().body(Map.of(
                "transactionId", transactionId,
                "status", "PENDING",
                "message", "Transferencia iniciada, consulte estado en GET /api/v1/orchestrator/transfers/" + transactionId
        ));
    }

    // Polling endpoint para consultar el estado de una transferencia en tiempo real
    @GetMapping("/transfers/{transactionId}")
    public ResponseEntity<?> getTransferStatus(@PathVariable String transactionId) {
        SagaTransactionDTO result = transactionStore.get(transactionId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        if ("COMMITTED".equals(result.getStatus())) {
            return ResponseEntity.ok(result);
        }
        if ("ABORTED".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
