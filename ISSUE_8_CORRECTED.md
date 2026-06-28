## Objetivo
Implementar la lógica de negocio local para cada banco (A, B, C): depósitos, retiros y validaciones de saldo. Este es el componente que maneja las operaciones individuales de cada nodo bancario.

## Responsabilidades

### 1. Domain Models (pe/unsa/sd/bank{a,b,c}/domain/model/)

Crear **AccountOperationRequest.java**:
- `accountId`: ID de la cuenta
- `amount`: Monto de la operación
- `operationType`: "DEPOSIT" o "WITHDRAW"

Crear **Transaction.java**:
- `transactionId`: UUID único
- `accountId`: Cuenta afectada
- `type`: "DEBIT", "CREDIT", "REVERSAL"
- `amount`: Monto
- `balanceBefore`: Saldo anterior
- `balanceAfter`: Saldo posterior
- `timestamp`: Fecha/hora
- `status`: "SUCCESS", "FAILED", "PENDING"

### 2. Domain Service (pe/unsa/sd/bank{a,b,c}/domain/service/)

Crear **AccountValidationService.java** con métodos:
- `validateWithdraw(Account, BigDecimal)`: Verifica saldo suficiente y monto > 0
- `validateDeposit(Account, BigDecimal)`: Verifica monto > 0
- `calculateNewBalance(Account, operationType, amount)`: Calcula nuevo saldo

### 3. Use Cases (pe/unsa/sd/bank{a,b,c}/application/usecase/)

Crear **DepositUseCase.java**:
- Orquesta: obtener cuenta → validar → calcular → actualizar → registrar transacción
- Lanzar `InvalidAmountException` si monto ≤ 0

Crear **WithdrawUseCase.java**:
- Orquesta: obtener cuenta → validar saldo → calcular → actualizar → registrar transacción
- Lanzar `InsufficientFundsException` si no hay fondos

Crear **DebitUseCase.java** (para Saga distribuida):
- Ejecuta débito local sin validación de fondos (validación ocurre en coordinador)
- Registra en journal

Crear **CreditUseCase.java** (para Saga distribuida):
- Ejecuta crédito local sin restricciones
- Registra en journal

### 4. Excepciones Personalizadas (pe/unsa/sd/bank{a,b,c}/exception/)

- `InsufficientFundsException.java`
- `InvalidAmountException.java`

### 5. DTOs (pe/unsa/sd/bank{a,b,c}/dto/)

- **DepositRequest.java**: `amount`
- **WithdrawRequest.java**: `amount`
- **TransactionDTO.java**: Serialización de Transaction
- **DebitRequest.java**: Para Saga (sagaId, amount, accountId)
- **CreditRequest.java**: Para Saga (sagaId, amount, accountId)

### 6. Controller Actualizado (pe/unsa/sd/bank{a,b,c}/controller/)

Agregar endpoints en **AccountController.java**:

```
POST /api/v1/bank/accounts/{accountId}/deposit
  Body: {"amount": 100.00}
  Response: TransactionDTO con status SUCCESS/FAILED

POST /api/v1/bank/accounts/{accountId}/withdraw
  Body: {"amount": 50.00}
  Response: TransactionDTO con status SUCCESS/FAILED

POST /api/v1/bank/accounts/{accountId}/debit
  Body: {"sagaId": "saga-uuid", "amount": 50.00}
  Response: {"status": "SUCCESS", "newBalance": 4950.00}
  Errors: 400 INVALID_AMOUNT, 500 INTERNAL_ERROR

POST /api/v1/bank/accounts/{accountId}/credit
  Body: {"sagaId": "saga-uuid", "amount": 50.00}
  Response: {"status": "SUCCESS", "newBalance": 5050.00}
  Errors: 400 INVALID_AMOUNT, 500 INTERNAL_ERROR
```

## Ejecución

### Bank A (Replicar después a B y C)
1. Crear estructura de carpetas: domain/, application/, exception/, dto/
2. Implementar modelos de dominio
3. Implementar validaciones (AccountValidationService)
4. Implementar use cases (Deposit, Withdraw, Debit, Credit)
5. Crear excepciones
6. Crear DTOs
7. Agregar endpoints al controller
8. Probar localmente:
   ```bash
   curl -X POST http://localhost:8081/api/v1/bank/accounts/ACC-A001/deposit \
     -H "Content-Type: application/json" \
     -d '{"amount": 100.00}'
   ```

### Bank B y C
- Copiar estructura completa de Bank A
- Cambiar package: `banka` → `bankb` / `bankc`
- Cambiar `BANK_A` → `BANK_B` / `BANK_C` en configuraciones
- Puertos: Bank B en 8082, Bank C en 8083

## Criterios de Aceptación

- [ ] Depósito funciona: aumenta saldo correctamente
- [ ] Retiro funciona: disminuye saldo correctamente
- [ ] Retiro rechaza si saldo insuficiente (409 error)
- [ ] Validaciones rechazan montos ≤ 0
- [ ] Endpoints `/debit` y `/credit` responden correctamente
- [ ] Débito disminuye saldo sin validar fondos (Saga lo valida)
- [ ] Crédito aumenta saldo sin restricciones
- [ ] Transacciones se registran con ID único
- [ ] Todos los 3 bancos (A, B, C) funcionan idénticamente
- [ ] Tests manuales con curl exitosos
- [ ] Logs muestran detalles de operaciones

## Notas Técnicas

- Usar `@Service` para use cases
- Inyección de dependencias via constructor
- Usar `BigDecimal` para valores monetarios (no double)
- Logging con SLF4J en use cases
- Manejo de excepciones con try-catch en controller
- DTOs para serialización JSON
- Endpoints `/debit` y `/credit` NO validan fondos (es responsabilidad del orquestador)
- Incluir `sagaId` en requests para rastrabilidad distribuida
