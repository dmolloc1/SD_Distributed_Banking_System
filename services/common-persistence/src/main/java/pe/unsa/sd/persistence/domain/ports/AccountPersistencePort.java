package pe.unsa.sd.persistence.domain.ports;

import java.math.BigDecimal;
import pe.unsa.sd.persistence.domain.model.Account;
import pe.unsa.sd.persistence.exception.StorageException;

public interface AccountPersistencePort {
    
    /**
     * Actualiza el saldo de una cuenta de forma atómica, asegurando el bloqueo del archivo,
     * el registro en el journal y la escritura atómica.
     * 
     * @param accountId     El ID de la cuenta a actualizar.
     * @param amount        El monto a agregar (positivo) o restar (negativo).
     * @param operationType El tipo de operación (ej. DEBIT, CREDIT) para auditoría.
     * @param transactionId El ID de la transacción para auditoría.
     * @return El objeto Account actualizado.
     * @throws StorageException Si ocurre un error de I/O o la cuenta está bloqueada.
     */
    Account updateBalanceAtomic(String accountId, BigDecimal amount, String operationType, String transactionId) throws StorageException;
}