package pe.unsa.sd.coordinator.store;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.unsa.sd.coordinator.dto.SagaTransactionDTO;

// Almacenamiento en memoria de transacciones en progreso.
// Permite al frontend consultar el estado de una transferencia via polling (GET).
// Las transacciones completadas se limpian automaticamente cada 5 minutos.
@Component
public class SagaTransactionStore {

    private static final Logger log = LoggerFactory.getLogger(SagaTransactionStore.class);

    private final Map<String, SagaTransactionDTO> transactions = new ConcurrentHashMap<>();

    private static final int RETENTION_MINUTES = 30;

    public void put(String transactionId, SagaTransactionDTO dto) {
        transactions.put(transactionId, dto);
    }

    public SagaTransactionDTO get(String transactionId) {
        return transactions.get(transactionId);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanExpiredTransactions() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(RETENTION_MINUTES);
        transactions.entrySet().removeIf(entry -> {
            SagaTransactionDTO dto = entry.getValue();
            if (dto == null) return true;
            if ("COMMITTED".equals(dto.getStatus()) || "ABORTED".equals(dto.getStatus())) {
                if (dto.getCompletedAt() != null && dto.getCompletedAt().isBefore(cutoff)) {
                    log.debug("Eliminando transaccion expirada: {}", entry.getKey());
                    return true;
                }
            }
            return false;
        });
    }
}
