package pe.unsa.sd.coordinator.controller;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.unsa.sd.coordinator.dto.SagaTransactionDTO;
import pe.unsa.sd.coordinator.dto.TransferRequest;
import pe.unsa.sd.coordinator.service.SagaOrchestrationService;

@RestController
@RequestMapping("/api/v1/orchestrator")
@CrossOrigin(origins = "http://localhost:5173")
public class TransferController {

    private static final Set<String> VALID_BANKS = Set.of("BANK_A", "BANK_B", "BANK_C");

    private final SagaOrchestrationService sagaOrchestrationService;

    public TransferController(SagaOrchestrationService sagaOrchestrationService) {
        this.sagaOrchestrationService = sagaOrchestrationService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest request) {
        if (!VALID_BANKS.contains(request.getOriginBankId()) || !VALID_BANKS.contains(request.getDestinationBankId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_REQUEST", "message", "originBankId o destinationBankId inválido"));
        }

        try {
            SagaTransactionDTO result = sagaOrchestrationService.executeSaga(request);
            if ("COMMITTED".equals(result.getStatus())) {
                return ResponseEntity.ok(result);
            }
            if ("ABORTED".equals(result.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "INVALID_REQUEST", "message", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", exception.getMessage()));
        }
    }
}
