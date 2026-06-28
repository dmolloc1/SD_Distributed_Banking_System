# [TASK] Integrante 2: Implementar Transferencias Distribuidas (Saga Orquestado con Compensación)

## Objetivo
Implementar la orquestación de transacciones distribuidas mediante el **Patrón Saga Orquestado** en el Global Transaction Orchestrator (GTO). Este patrón divide operaciones interbancarias en pasos locales secuenciales con transacciones compensatorias automáticas ante fallos.

**Justificación Arquitectónica:**
- **Sin bloqueos distribuidos**: Cada banco ejecuta localmente sin mantener locks globales
- **Consistencia eventual**: Se garantiza mediante compensación, no bloqueos
- **Alta resiliencia**: Fallos en un nodo no bloquean indefinidamente al orquestador
- **Procesos de larga duración**: Apropiado para transferencias con múltiples fases

## Distinción vs 2PC (Two-Phase Commit)
❌ NO implementar 2PC porque:
- Bloquea indefinidamente si el coordinador falla
- Incumple RNF-02 (timeout por fase)
- Incompatible con autonomía de datos

✅ Implementar Saga porque:
- Cada paso tiene timeout independiente
- Incluye compensación (rollback lógico) explícito
- Mantiene autonomía de cada Bank Core Node

---

## Responsabilidades

### 1. Domain Models (pe/unsa/sd/coordinator/domain/model/)

**SagaTransaction.java** (modelo de transacción distribuida):
- `transactionId`: UUID único global (generado automáticamente)
- `sourceAccountId`: Cuenta origen
- `sourceBankId`: ID banco origen (BANK_A, BANK_B, BANK_C)
- `destinationAccountId`: Cuenta destino
- `destinationBankId`: ID banco destino
- `amount`: Monto a transferir (BigDecimal)
- `status`: Estados de la Saga:
  - `PENDING`: Recibida, no iniciada
  - `EXECUTING_STEP_1`: Ejecutando step 1 (débito origen)
  - `EXECUTING_STEP_2`: Ejecutando step 2 (crédito destino)
  - `COMMITTED`: Ambos steps completados exitosamente
  - `COMPENSATING`: Ejecutando transacciones compensatorias
  - `ABORTED`: Saga fallida permanentemente
- `initiatedAt`: Timestamp de inicio
- `completedAt`: Timestamp de finalización (null si aún en progreso)
- `steps`: List\<SagaStep\> historial completo

**SagaStep.java** (un paso individual dentro de la saga):
- `stepId`: ID único del paso dentro de la saga
- `stepNumber`: Orden secuencial (1, 2, 3...)
- `bankId`: Banco donde se ejecuta
- `accountId`: Cuenta afectada
- `operationType`: Tipo de operación local ("DEBIT" o "CREDIT")
- `amount`: Monto del paso
- `stepStatus`: Estado del paso:
  - `PENDING`: No ejecutado aún
  - `EXECUTING`: En progreso
  - `SUCCESS`: Completado exitosamente
  - `FAILED`: Falló, requiere compensación
  - `COMPENSATED`: Transacción compensatoria ejecutada
- `executedAt`: Timestamp de ejecución
- `errorMessage`: Descripción del error (si aplica)
- `compensationApplied`: Boolean indicando si se ejecutó compensación
- `compensationTimestamp`: Timestamp de compensación

---

### 2. Domain Service (pe/unsa/sd/coordinator/domain/service/)

**SagaOrchestrationService.java** con métodos:

#### **executeSaga(SagaTransaction saga) → SagaResult**
Orquesta la ejecución completa de una saga:
1. Persiste saga inicial con estado PENDING
2. Itera sobre cada step de forma secuencial
3. Para cada step:
   - Llama `executeStep()`
   - Si SUCCESS: continúa al siguiente
   - Si FAILED: ejecuta `executeCompensation()` en pasos anteriores
4. Retorna resultado final

```java
public SagaResult executeSaga(SagaTransaction saga) {
  // 1. Validar saga (montos > 0, bancos válidos)
  // 2. Persister estado inicial
  
  for (SagaStep step : saga.getSteps()) {
    try {
      saga.setStatus("EXECUTING_STEP_" + step.getStepNumber());
      executeStep(step);
      step.setStepStatus("SUCCESS");
    } catch (Exception e) {
      step.setStepStatus("FAILED");
      step.setErrorMessage(e.getMessage());
      
      // Ejecutar compensación en pasos anteriores
      executeCompensation(saga, step.getStepNumber());
      saga.setStatus("ABORTED");
      return new SagaResult(false, saga);
    }
  }
  
  saga.setStatus("COMMITTED");
  saga.setCompletedAt(LocalDateTime.now());
  return new SagaResult(true, saga);
}
```

#### **executeStep(SagaStep step) → void**
Ejecuta un paso individual (débito o crédito):
- Llama al Bank Core Node correspondiente
- Retorna exitosamente o lanza excepción
- **NO espera confirmación final** (la compensación es el mecanismo de deshace)

```java
public void executeStep(SagaStep step) {
  String bankUrl = getBankUrl(step.getBankId());
  String operation = step.getOperationType(); // "DEBIT" o "CREDIT"
  
  try {
    if ("DEBIT".equals(operation)) {
      // Débito: quitar fondos de cuenta origen
      // POST {bankUrl}/api/v1/bank/accounts/{accountId}/debit
      bankServiceClient.debit(bankUrl, step.getAccountId(), step.getAmount());
    } else {
      // Crédito: agregar fondos a cuenta destino
      // POST {bankUrl}/api/v1/bank/accounts/{accountId}/credit
      bankServiceClient.credit(bankUrl, step.getAccountId(), step.getAmount());
    }
  } catch (RestClientException e) {
    throw new SagaStepException("Fallo en step " + step.getStepNumber(), e);
  }
}
```

#### **executeCompensation(SagaTransaction saga, int failedAtStep) → void**
Ejecuta transacciones compensatorias para deshacer steps anteriores:

```java
public void executeCompensation(SagaTransaction saga, int failedAtStep) {
  saga.setStatus("COMPENSATING");
  
  // Iterar HACIA ATRÁS desde (failedAtStep - 1) hasta 1
  for (int i = failedAtStep - 1; i >= 1; i--) {
    SagaStep step = saga.getSteps().get(i - 1);
    try {
      // Invertir la operación
      String inverseOp = "DEBIT".equals(step.getOperationType()) ? "CREDIT" : "DEBIT";
      
      String bankUrl = getBankUrl(step.getBankId());
      if ("CREDIT".equals(inverseOp)) {
        // Si fue débito, hacer crédito de compensación
        bankServiceClient.credit(bankUrl, step.getAccountId(), step.getAmount());
      } else {
        // Si fue crédito, hacer débito de compensación
        bankServiceClient.debit(bankUrl, step.getAccountId(), step.getAmount());
      }
      
      step.setCompensationApplied(true);
      step.setCompensationTimestamp(LocalDateTime.now());
      
    } catch (RestClientException e) {
      // CRÍTICO: si compensación falla, registro de error para reconciliación manual
      log.error("CRÍTICO: Compensación falló en Step {}. Reconciliación manual necesaria", i, e);
      step.setErrorMessage("Compensación falló: " + e.getMessage());
    }
  }
}
```

---

### 3. DTOs (pe/unsa/sd/coordinator/dto/)

- **TransferRequest.java**:
  - `originAccountId`, `destinationAccountId`
  - `originBankId`, `destinationBankId`
  - `amount`, `currency`

- **SagaStepRequest.java** (solicitud para ejecutar un paso):
  - `sagaId`: ID de la saga a la que pertenece
  - `stepNumber`: Orden del paso
  - `bankId`: Banco
  - `accountId`: Cuenta
  - `operationType`: "DEBIT" o "CREDIT"
  - `amount`: Monto

- **CompensationRequest.java** (solicitud de compensación):
  - `sagaId`: ID de la saga
  - `originalOperation`: Operación original ("DEBIT" o "CREDIT")
  - `bankId`, `accountId`, `amount`
  - `reason`: Motivo de compensación

- **SagaTransactionDTO.java**:
  - Serialización completa de SagaTransaction
  - Incluye steps con estado actual
  - Timestamps de ejecución

---

### 4. Controller (pe/unsa/sd/coordinator/controller/)

**TransferController.java**:

```
POST /api/v1/orchestrator/transfers
  Body: {
    "originAccountId": "ACC-A001",
    "originBankId": "BANK_A",
    "destinationAccountId": "ACC-B001",
    "destinationBankId": "BANK_B",
    "amount": 100.00,
    "currency": "USD"
  }

  Response (200 OK - COMMITTED):
  {
    "transactionId": "tx-uuid",
    "status": "COMMITTED",
    "amount": 100.00,
    "initiatedAt": "2024-01-15T10:30:00Z",
    "completedAt": "2024-01-15T10:30:05Z",
    "steps": [
      {
        "stepNumber": 1,
        "operation": "DEBIT",
        "bank": "BANK_A",
        "account": "ACC-A001",
        "amount": 100.00,
        "status": "SUCCESS",
        "executedAt": "2024-01-15T10:30:01Z"
      },
      {
        "stepNumber": 2,
        "operation": "CREDIT",
        "bank": "BANK_B",
        "account": "ACC-B001",
        "amount": 100.00,
        "status": "SUCCESS",
        "executedAt": "2024-01-15T10:30:02Z"
      }
    ]
  }

  Response (409 Conflict - ABORTED con compensación):
  {
    "transactionId": "tx-uuid",
    "status": "ABORTED",
    "error": "INSUFFICIENT_FUNDS",
    "steps": [
      {
        "stepNumber": 1,
        "operation": "DEBIT",
        "status": "SUCCESS",
        "executedAt": "2024-01-15T10:30:01Z"
      },
      {
        "stepNumber": 2,
        "operation": "CREDIT",
        "status": "FAILED",
        "error": "INSUFFICIENT_FUNDS",
        "failedAt": "2024-01-15T10:30:02Z"
      },
      {
        "stepNumber": 1,
        "operation": "CREDIT (COMPENSATION)",
        "status": "COMPENSATED",
        "compensatedAt": "2024-01-15T10:30:03Z"
      }
    ]
  }

  Response (400 Bad Request):
  {
    "error": "INVALID_REQUEST",
    "message": "Monto debe ser > 0"
  }
```

---

### 5. Logging Detallado

```
=== SAGA INICIADA ===
SagaId=tx-abc123, Origen=BANK_A:ACC-A001, Destino=BANK_B:ACC-B001, Monto=100.00

[STEP 1/2] Ejecutando DEBIT en BANK_A
✓ DEBIT exitoso. Nuevo saldo: 4900.00

[STEP 2/2] Ejecutando CREDIT en BANK_B
✗ CREDIT falló: INSUFFICIENT_FUNDS_BANK_B
⚠ Iniciando compensación...

[COMPENSACIÓN STEP 1/1] Revirtiendo DEBIT en BANK_A
✓ CREDIT (compensación) exitoso. Saldo restaurado: 5000.00

=== SAGA ABORTADA ===
SagaId=tx-abc123, Razón=Fallo en Step 2, CompensacionEstado=COMPLETA
```

---

## Criterios de Aceptación

- [ ] Endpoint `/api/v1/orchestrator/transfers` recibe solicitud
- [ ] Saga ejecuta steps de forma **secuencial** (no paralela)
- [ ] Step 1 (DEBIT origen) se ejecuta correctamente
- [ ] Step 2 (CREDIT destino) se ejecuta correctamente
- [ ] Si Step 2 falla: compensación automática ejecuta CREDIT inverso en origen
- [ ] Cada step registrado con `stepNumber`, `status`, `executedAt`
- [ ] Compensación solo se ejecuta si hay fallos
- [ ] Logs muestran el flujo completo de ejecución y compensación
- [ ] Respuesta JSON incluye historial de steps **en orden de ejecución**
- [ ] Tests manuales demuestran: éxito, fallo con compensación
- [ ] Manejo robusto de errores de red

---

## Prueba Manual

```bash
# Transfer exitosa (A → B)
curl -X POST http://localhost:8080/api/v1/orchestrator/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "originAccountId": "ACC-A001",
    "originBankId": "BANK_A",
    "destinationAccountId": "ACC-B001",
    "destinationBankId": "BANK_B",
    "amount": 50.00,
    "currency": "USD"
  }'

# Transfer con fondos insuficientes en destino (falla y compensa)
curl -X POST http://localhost:8080/api/v1/orchestrator/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "originAccountId": "ACC-A001",
    "originBankId": "BANK_A",
    "destinationAccountId": "ACC-B001",
    "destinationBankId": "BANK_B",
    "amount": 50000.00,
    "currency": "USD"
  }'
```

---

## Notas Técnicas

- **SIN bloqueos distribuidos**: Cada banco maneja concurrencia localmente
- **Transacciones compensatorias**: Diseñar compensación inversa para cada step
- **Timeouts por step**: Cada call a un banco tiene timeout configurable
- **Persistencia de Saga**: Guardar estado de saga en archivo JSON para recuperación
- **Idempotencia**: Implementar Idempotency Keys en headers HTTP
- **URLs de bancos**: BANK_A = http://localhost:8081, BANK_B = http://localhost:8082
- **Logging**: Usar SLF4J con niveles INFO para flujo normal, ERROR para fallos

---

## Diferencia vs 2PC

| Aspecto | 2PC | Saga Orquestado |
|--------|-----|-----------------|
| Bloqueos | Distribuidos durante 2 fases | Solo locales en cada banco |
| Fallo del coordinador | Bloquea indefinidamente | Puede recuperarse con compensación |
| Latencia | Alta (3 round-trips) | Media (steps secuenciales) |
| Compensación | Rollback atómico | Lógica explícita por step |
| Escalabilidad | Limitada | Alta (sin bloqueos globales) |

---

## Dependencias

Requiere que **Integrante 1** haya implementado en Bank Core Nodes:
- POST `/api/v1/bank/accounts/{accountId}/debit`
- POST `/api/v1/bank/accounts/{accountId}/credit`

(NO `/reserve`, `/confirm`, `/rollback` que son 2PC)
