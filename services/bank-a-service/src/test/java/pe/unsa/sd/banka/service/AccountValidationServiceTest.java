package pe.unsa.sd.banka.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.unsa.sd.banka.exception.InsufficientFundsException;
import pe.unsa.sd.banka.exception.InvalidAmountException;
import pe.unsa.sd.banka.model.Account;

class AccountValidationServiceTest {

    private AccountValidationService validationService;
    private Account account;

    @BeforeEach
    void setUp() {
        validationService = new AccountValidationService();
        account = new Account("A-1001", "C001", "BANK_A", "AHORROS", "PEN", new BigDecimal("100.00"));
    }

    @Test
    void validateDepositAcceptsPositiveAmount() {
        assertDoesNotThrow(() -> validationService.validateDeposit(account, new BigDecimal("25.00")));
    }

    @Test
    void validateDepositRejectsInvalidAmount() {
        assertThrows(InvalidAmountException.class,
                () -> validationService.validateDeposit(account, BigDecimal.ZERO));
    }

    @Test
    void validateWithdrawAcceptsAvailableFunds() {
        assertDoesNotThrow(() -> validationService.validateWithdraw(account, new BigDecimal("40.00")));
    }

    @Test
    void validateWithdrawRejectsInsufficientFunds() {
        assertThrows(InsufficientFundsException.class,
                () -> validationService.validateWithdraw(account, new BigDecimal("150.00")));
    }

    @Test
    void calculateNewBalanceAppliesDepositAndWithdraw() {
        assertEquals(new BigDecimal("125.00"),
                validationService.calculateNewBalance(account, "DEPOSIT", new BigDecimal("25.00")));
        assertEquals(new BigDecimal("60.00"),
                validationService.calculateNewBalance(account, "WITHDRAW", new BigDecimal("40.00")));
    }
}
