package pe.unsa.sd.persistence.exception;

/**
 * Lanzada cuando una cuenta no tiene saldo suficiente para realizar un débito o retiro.
 */
public class InsufficientFundsException extends StorageException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}