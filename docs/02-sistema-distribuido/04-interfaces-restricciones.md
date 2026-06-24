## 10. Interfaces de Acceso para la Lógica de Negocio

Para evitar el acoplamiento directo entre la lógica financiera y el sistema de archivos, el Equipo 2 (Lógica de Negocio) deberá interactuar exclusivamente a través de la interfaz `AccountPersistencePort`. Esta abstracción oculta la complejidad del manejo de *Locks*, archivos temporales y sincronización de disco.

### 10.1. Interfaz Principal: `AccountPersistencePort`

```java
public interface AccountPersistencePort {
    
    /**
     * Recupera una cuenta de forma segura.
     * Implementa un Shared Lock interno para lectura consistente.
     */
    Optional<AccountDto> findAccountById(String accountId) throws StorageException;

    /**
     * Realiza una actualización de saldo atómica.
     * Orquesta automáticamente: Exclusive Lock -> Journaling -> Shadow Update -> Unlock.
     * @param transaction Datos de la operación (Monto, TX_ID, Tipo)
     * @return El nuevo estado de la cuenta tras el commit.
     */
    AccountDto updateBalanceAtomic(TransactionRequest transaction) throws StorageException, ConcurrencyException;

    /**
     * Obtiene el historial de movimientos desde el Ledger inmutable.
     */
    List<TransactionLogEntry> getTransactionHistory(String accountId);
}
```

### 10.2. Modelo de Respuesta Transaccional
Cada operación sobre la persistencia retornará un objeto de estado que debe ser validado por la lógica de negocio antes de responder al Orquestador Saga.

| Componente | Tipo | Descripción |
| :--- | :--- | :--- |
| `status` | Enum | `SUCCESS`, `LOCKED_BY_OTHER`, `FILE_CORRUPTED`, `INSUFFICIENT_FUNDS`. |
| `data` | Object | La entidad `AccountDto` resultante. |
| `journalReference` | String | El ID del registro generado en el archivo `.log`. |

---

## 11. Buenas Prácticas, Restricciones y Lineamientos

El éxito de una arquitectura distribuida basada en archivos depende de la disciplina de los desarrolladores. El incumplimiento de estas normas puede resultar en la pérdida de integridad de los datos.

### 11.1. Restricciones Mandatorias (Prohibiciones)
1.  **Acceso Directo Prohibido:** Queda estrictamente prohibido que cualquier clase ajena al paquete de persistencia utilice `java.io` o `java.nio` para leer o escribir en los directorios de datos.
2.  **No Bypass del Orquestador:** Ningún banco debe modificar sus archivos de forma autónoma sin una transacción iniciada por el `Coordinator Service`, salvo para procesos de recuperación de desastres.
3.  **Gestión de Excepciones:** No se deben capturar excepciones de persistencia (`IOException`, `LockAcquisitionException`) de forma genérica. Cada error debe ser escalado para que el Orquestador decida si ejecutar una compensación (Rollback).

### 11.2. Lineamientos de Desarrollo (Buenas Prácticas)
1.  **Idempotencia de Operaciones:**
    La capa de persistencia validará el `TX_ID` antes de escribir. Si el Equipo 2 envía una transacción que ya existe en el `transactions.log`, el sistema retornará la respuesta previa sin ejecutar una doble operación (Prevención de cobros dobles).
    
2.  **Minimización del Tiempo de Bloqueo (Lock Hold Time):**
    Los desarrolladores de lógica de negocio deben realizar todos los cálculos, validaciones de identidad y reglas de fraude **antes** de solicitar el `updateBalanceAtomic`. El tiempo transcurrido con el archivo bloqueado debe limitarse estrictamente a la escritura física en disco.

3.  **Uso de BigDecimals:**
    Para evitar errores de redondeo en archivos JSON, todos los montos deben manejarse como `BigDecimal` con escala 2 y modo de redondeo `HALF_EVEN`. **Nunca usar `float` o `double`** para saldos financieros.

### 11.3. Matriz de Decisiones ante Fallos de Persistencia

| Situación | Acción del Desarrollador (Equipo 2/3) | Impacto |
| :--- | :--- | :--- |
| **Archivo Bloqueado (`423 Locked`)** | Implementar Reintento Exponencial (3 veces). | Retraso leve en UI. |
| **Error de Integridad (Checksum mismatch)** | Notificar error crítico y detener el nodo bancario. | Requiere intervención manual. |
| **Timeout de Escritura** | Ejecutar compensación en el banco contraparte. | Transacción fallida, dinero seguro. |

