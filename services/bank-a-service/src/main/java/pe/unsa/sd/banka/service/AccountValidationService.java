package pe.unsa.sd.banka.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import pe.unsa.sd.banka.exception.InsufficientFundsException;
import pe.unsa.sd.banka.exception.InvalidAmountException;
import pe.unsa.sd.banka.model.Account;

@Service
public class AccountValidationService {

    private static final int MONEY_SCALE = 2;

    public void validateDeposit(Account account, BigDecimal amount) {
        requireAccount(account);
        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("El monto del depósito debe ser mayor a cero");
        }
    }

    public void validateWithdraw(Account account, BigDecimal amount) {
        requireAccount(account);
        BigDecimal normalizedAmount = normalizeAmount(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("El monto del retiro debe ser mayor a cero");
        }

        BigDecimal currentBalance = normalizeAmount(account.getBalance());
        if (currentBalance.compareTo(normalizedAmount) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente para realizar el retiro");
        }
    }

    public BigDecimal calculateNewBalance(Account account, String operationType, BigDecimal amount) {
        requireAccount(account);
        BigDecimal currentBalance = normalizeAmount(account.getBalance());
        BigDecimal normalizedAmount = normalizeAmount(amount);

        if ("DEPOSIT".equals(operationType)) {
            return currentBalance.add(normalizedAmount).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        }

        if ("WITHDRAW".equals(operationType)) {
            return currentBalance.subtract(normalizedAmount).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        }

        throw new IllegalArgumentException("Tipo de operación no soportado: " + operationType);
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private void requireAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("La cuenta no puede ser nula");
        }
    }
}
