# [TASK][TEAM-Logic] Integrante 2: Transferencias Distribuidas con Saga Orquestado (con Compensación)

## Estado real del proyecto (verificado en este repo)

### Ya implementado
- `coordinator-service`:
  - `GET /distributed/health`
  - `GET /distributed/accounts/{clientId}`
  - cliente HTTP con `RestTemplate` (`BankServiceClient`)
- `bank-a-service`, `bank-b-service`, `bank-c-service`:
  - `GET /clients/{clientId}/accounts`
  - lectura de cuentas desde JSON local

### Aún NO implementado
- No existe `POST /api/v1/orchestrator/transfers`
- No existen modelos `SagaTransaction` ni `SagaStep`
- No existen endpoints en bancos:
  - `POST /api/v1/bank/accounts/{accountId}/debit`
  - `POST /api/v1/bank/accounts/{accountId}/credit`
- No hay lógica Saga ni compensación en coordinator

---

## Objetivo de la Issue 9
Implementar en `coordinator-service` la orquestación de transferencias interbancarias usando **Saga Orquestado (NO 2PC)**:

1. Step 1: `DEBIT` en banco origen  
2. Step 2: `CREDIT` en banco destino  
3. Si falla step 2: compensación automática (`CREDIT` inverso en origen)

---

## Estructura y archivos correctos (alineado al repo actual)

> En este repo hoy se usan paquetes `controller`, `service`, `dto`, `client`, `model`.
> Para mantener consistencia, se recomienda esta estructura.

### Coordinator Service

Crear en:

```text
services/coordinator-service/src/main/java/pe/unsa/sd/coordinator/
  controller/
    TransferController.java
  service/
    SagaOrchestrationService.java
  model/
    SagaTransaction.java
    SagaStep.java
    SagaStatus.java
    SagaStepStatus.java
    OperationType.java
    BankId.java
  dto/
    TransferRequest.java
    BankOperationRequest.java
    CompensationRequest.java
    SagaTransactionDTO.java
```

> Si tu equipo decide mantener `domain/model` y `domain/service`, también es válido, pero **no está usado actualmente** en este repo.

### Bank Core Nodes (A/B/C)

Crear/actualizar en cada banco:

```text
services/bank-a-service/src/main/java/pe/unsa/sd/banka/controller/AccountController.java
services/bank-a-service/src/main/java/pe/unsa/sd/banka/service/FileAccountService.java

services/bank-b-service/src/main/java/pe/unsa/sd/bankb/controller/AccountController.java
services/bank-b-service/src/main/java/pe/unsa/sd/bankb/service/FileAccountService.java

services/bank-c-service/src/main/java/pe/unsa/sd/bankc/controller/AccountController.java
services/bank-c-service/src/main/java/pe/unsa/sd/bankc/service/FileAccountService.java
```

Nuevos endpoints requeridos por banco:
- `POST /api/v1/bank/accounts/{accountId}/debit`
- `POST /api/v1/bank/accounts/{accountId}/credit`

---

## Contrato API Coordinator

### Endpoint
`POST /api/v1/orchestrator/transfers`

### CORS
`@CrossOrigin(origins = "http://localhost:5173")`

### Request (`TransferRequest`)
```json
{
  "originAccountId": "ACC-A001",
  "originBankId": "BANK_A",
  "destinationAccountId": "ACC-B001",
  "destinationBankId": "BANK_B",
  "amount": 100.00,
  "currency": "USD"
}
```

### Responses esperadas
- `200 OK` si `status = COMMITTED`
- `409 Conflict` si `status = ABORTED` (incluye steps y compensación)
- `400 Bad Request` si request inválido
- `500 Internal Server Error` si error no controlado

---

## Modelos mínimos

### `SagaTransaction`
- `transactionId` (UUID)
- `sourceAccountId`
- `sourceBankId` (`BANK_A`, `BANK_B`, `BANK_C`)
- `destinationAccountId`
- `destinationBankId`
- `amount` (`BigDecimal`)
- `status`: `PENDING`, `EXECUTING_STEP_1`, `EXECUTING_STEP_2`, `COMMITTED`, `COMPENSATING`, `ABORTED`
- `initiatedAt`
- `completedAt` (nullable)
- `steps` (`List<SagaStep>`)

### `SagaStep`
- `stepId`
- `stepNumber`
- `bankId`
- `accountId`
- `operationType` (`DEBIT`, `CREDIT`)
- `amount`
- `stepStatus` (`PENDING`, `EXECUTING`, `SUCCESS`, `FAILED`, `COMPENSATED`)
- `executedAt`
- `errorMessage` (nullable)
- `compensationApplied` (boolean)
- `compensationTimestamp` (nullable)

---

## Servicio de Orquestación

`SagaOrchestrationService.executeSaga(SagaTransaction saga)`:

1. Validar entrada (`amount > 0`, bancos válidos)
2. Ejecutar step 1 (`DEBIT` origen)
3. Ejecutar step 2 (`CREDIT` destino)
4. Si ambos OK -> `COMMITTED` y `completedAt=now`
5. Si falla step 2 -> `COMPENSATING` -> `CREDIT` compensatorio en origen -> `ABORTED`

Métodos auxiliares:
- `executeStep(SagaStep step)`
- `executeCompensation(SagaTransaction saga, int failedAtStep)`

Regla:
- Compensar en orden inverso de steps exitosos
- Si falla una compensación: registrar error crítico y dejar trazabilidad

---

## Mapeo de bancos (MVP)
- `BANK_A -> http://localhost:8081`
- `BANK_B -> http://localhost:8082`
- `BANK_C -> http://localhost:8083`

---

## Logging mínimo requerido

```text
=== SAGA INICIADA ===
[STEP 1/2] Ejecutando DEBIT en BANK_A account=ACC-A001 amount=100.00
✓ STEP 1 SUCCESS
[STEP 2/2] Ejecutando CREDIT en BANK_B account=ACC-B001 amount=100.00
✗ STEP 2 FAILED: ...
⚠ Iniciando compensación
[COMPENSACIÓN] CREDIT en BANK_A account=ACC-A001 amount=100.00
=== SAGA COMMITTED ===
o
=== SAGA ABORTED ===
```

---

## Definition of Done
- [ ] `POST /api/v1/orchestrator/transfers` operativo
- [ ] Saga secuencial (sin 2PC)
- [ ] Step 1 (`DEBIT` origen) correcto
- [ ] Step 2 (`CREDIT` destino) correcto en éxito
- [ ] Si falla step 2, compensación ejecutada (`CREDIT` origen)
- [ ] Steps con `stepNumber`, `stepStatus`, `executedAt`
- [ ] JSON de respuesta incluye historial completo de steps
- [ ] Logs trazables por `transactionId`
- [ ] Prueba manual de éxito y fallo con compensación

---

## Prueba manual (Coordinator)

```bash
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
```

```bash
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

## Correcciones aplicadas respecto a la versión anterior

1. Se eliminó referencia a DTO `SagaStepRequest` y se reemplazó por `BankOperationRequest` (según issue original).
2. Se corrigió el alcance para incluir explícitamente `BANK_C`.
3. Se eliminó mezcla conceptual con 2PC (`reserve/confirm/rollback`).
4. Se alineó estructura de paquetes con el estilo actual del repo (`model/service/controller/dto`).
5. Se dejó claro que los endpoints `debit/credit` en bank nodes son dependencia obligatoria.
