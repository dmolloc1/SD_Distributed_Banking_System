# API Gateway

## Rol dentro del sistema distribuido

El API Gateway es el componente encargado de centralizar la comunicacion entre el frontend y los microservicios internos del ecosistema bancario distribuido. En lugar de que la interfaz web consuma directamente a cada banco o al coordinador de transacciones, todas las solicitudes ingresan primero por el gateway, ubicado en el servicio `api-gateway-service`.

Esta decision permite desacoplar la capa de presentacion de la topologia interna del sistema. El usuario no necesita conocer en que banco se encuentra fisicamente una cuenta ni que servicio debe procesar cada operacion. El gateway recibe la solicitud, valida los campos basicos, identifica el banco correspondiente y redirige la operacion al servicio adecuado.

En la arquitectura implementada, el API Gateway cumple tres funciones principales:

- Exponer un punto unico de entrada para el frontend.
- Enrutar consultas y operaciones hacia el coordinador o hacia los bancos.
- Ocultar las rutas internas de los microservicios bancarios.

## Ubicacion en la arquitectura

El flujo general de comunicacion queda organizado de la siguiente manera:

```text
Frontend React
    |
    v
API Gateway Service
    |
    +--> Coordinator Service
    |       |
    |       +--> Bank A Service
    |       +--> Bank B Service
    |       +--> Bank C Service
    |
    +--> Bank A Service
    +--> Bank B Service
    +--> Bank C Service
```

Para consultas distribuidas y transferencias interbancarias, el gateway redirige la solicitud al `coordinator-service`. Para operaciones directas, como depositos y retiros, el gateway identifica el banco propietario de la cuenta y envia la operacion al nodo bancario correspondiente.

## Configuracion del Gateway

El servicio se ejecuta en el puerto `8080`. Las rutas de los bancos y del coordinador no estan escritas directamente como valores fijos dentro de la logica de negocio, sino que se configuran en el archivo `application.yml`.

```yml
server:
  port: 8080

spring:
  application:
    name: api-gateway-service

gateway:
  services:
    coordinator-url: ${COORDINATOR_SERVICE_URL:http://localhost:8090}
    bank-a-url: ${BANK_A_SERVICE_URL:http://localhost:8081}
    bank-b-url: ${BANK_B_SERVICE_URL:http://localhost:8082}
    bank-c-url: ${BANK_C_SERVICE_URL:http://localhost:8083}
```

Esta configuracion permite cambiar las ubicaciones de los servicios mediante variables de entorno, lo cual facilita el despliegue local, en contenedores o en diferentes maquinas.

## Endpoints publicos expuestos

El API Gateway expone los siguientes endpoints principales:

| Endpoint | Metodo | Funcion |
| --- | --- | --- |
| `/api/gateway/health` | GET | Verifica el estado del gateway |
| `/api/coordinator/health` | GET | Verifica el estado del coordinador |
| `/api/bank-a/health` | GET | Verifica el estado del Banco A |
| `/api/bank-b/health` | GET | Verifica el estado del Banco B |
| `/api/bank-c/health` | GET | Verifica el estado del Banco C |
| `/api/customers/{customerId}/accounts` | GET | Consulta las cuentas consolidadas de un cliente |
| `/api/operations/deposit` | POST | Ejecuta un deposito en la cuenta destino |
| `/api/operations/withdraw` | POST | Ejecuta un retiro desde la cuenta origen |
| `/api/transfers` | POST | Inicia una transferencia distribuida |
| `/api/transactions/{transactionId}` | GET | Consulta el estado de una transferencia |

## Identificacion del banco por prefijo de cuenta

Una de las responsabilidades centrales del gateway es determinar a que banco pertenece una cuenta. Para ello se utiliza una convencion de prefijos:

| Prefijo de cuenta | Banco |
| --- | --- |
| `A-` | `BANK_A` |
| `B-` | `BANK_B` |
| `C-` | `BANK_C` |

El siguiente fragmento del controlador muestra esta logica:

```java
private String extractBankFromAccount(String accountId) {
    if (accountId == null) {
        return null;
    }
    if (accountId.startsWith("A-")) {
        return "BANK_A";
    }
    if (accountId.startsWith("B-")) {
        return "BANK_B";
    }
    if (accountId.startsWith("C-")) {
        return "BANK_C";
    }
    return null;
}
```

Gracias a este mecanismo, el usuario puede operar desde cualquier banco sin preocuparse por la ubicacion real de la cuenta. Por ejemplo, una cuenta `B-2001` siempre sera enviada al servicio del Banco B, aunque el usuario este accediendo desde la interfaz del Banco A.

## Operaciones locales: deposito y retiro

Para operaciones simples, el gateway funciona como un router inteligente hacia el banco propietario de la cuenta.

### Deposito

El endpoint publico de deposito es:

```http
POST /api/operations/deposit
```

El frontend envia una solicitud con la cuenta destino y el monto. El gateway extrae el banco desde el prefijo de la cuenta y redirige la operacion al endpoint interno de credito del banco correspondiente.

Fragmento relevante:

```java
@PostMapping("/api/operations/deposit")
public Mono<ResponseEntity<Map<String, Object>>> deposit(@RequestBody Map<String, Object> payload) {
    String accountId = getString(payload, "targetAccountId");
    String amount = getString(payload, "amount");

    if (accountId == null || amount == null) {
        return badRequest("INVALID_REQUEST");
    }

    String bankId = extractBankFromAccount(accountId);
    String bankUrl = bankId == null ? null : bankUrls.get(bankId);
    if (bankUrl == null) {
        return badRequest("INVALID_ACCOUNT");
    }

    return executeBankOperation(bankUrl + "/api/v1/bank/accounts/" + accountId + "/credit", amount);
}
```

### Retiro

El endpoint publico de retiro es:

```http
POST /api/operations/withdraw
```

El gateway recibe la cuenta origen, identifica su banco por prefijo y envia la operacion al endpoint interno de debito:

```java
@PostMapping("/api/operations/withdraw")
public Mono<ResponseEntity<Map<String, Object>>> withdraw(@RequestBody Map<String, Object> payload) {
    String accountId = getString(payload, "sourceAccountId");
    String amount = getString(payload, "amount");

    if (accountId == null || amount == null) {
        return badRequest("INVALID_REQUEST");
    }

    String bankId = extractBankFromAccount(accountId);
    String bankUrl = bankId == null ? null : bankUrls.get(bankId);
    if (bankUrl == null) {
        return badRequest("INVALID_ACCOUNT");
    }

    return executeBankOperation(bankUrl + "/api/v1/bank/accounts/" + accountId + "/debit", amount);
}
```

En ambos casos, el gateway genera un `transactionId` y construye el cuerpo que se enviara al banco:

```java
private Mono<ResponseEntity<Map<String, Object>>> executeBankOperation(String url, String amount) {
    Map<String, Object> body = Map.of(
            "transactionId", java.util.UUID.randomUUID().toString(),
            "amount", new BigDecimal(amount),
            "currency", "USD");

    return forwardPost(url, body);
}
```

## Transferencias distribuidas

Las transferencias son operaciones mas complejas porque pueden involucrar cuentas ubicadas en bancos distintos. En este caso, el gateway no ejecuta directamente el debito ni el credito. Su funcion es transformar la solicitud del frontend en un `TransferRequest` compatible con el coordinador de Sagas.

El endpoint publico es:

```http
POST /api/transfers
```

La logica implementada es la siguiente:

1. Recibe `sourceAccountId`, `targetAccountId` y `amount`.
2. Identifica el banco origen desde el prefijo de la cuenta origen.
3. Identifica el banco destino desde el prefijo de la cuenta destino.
4. Construye el objeto esperado por el `coordinator-service`.
5. Reenvia la solicitud a `/api/v1/orchestrator/transfers`.

Fragmento de codigo:

```java
@PostMapping("/api/transfers")
public Mono<ResponseEntity<Map<String, Object>>> transfer(@RequestBody Map<String, Object> payload) {
    String sourceAccountId = getString(payload, "sourceAccountId");
    String targetAccountId = getString(payload, "targetAccountId");
    String sourceBankId = extractBankFromAccount(sourceAccountId);
    String destinationBankId = extractBankFromAccount(targetAccountId);
    String amount = getString(payload, "amount");

    if (sourceAccountId == null
            || targetAccountId == null
            || sourceBankId == null
            || destinationBankId == null
            || amount == null) {
        return badRequest("INVALID_REQUEST");
    }

    Map<String, Object> transferRequest = Map.of(
            "originAccountId", sourceAccountId,
            "originBankId", sourceBankId,
            "destinationAccountId", targetAccountId,
            "destinationBankId", destinationBankId,
            "amount", new BigDecimal(amount),
            "currency", "USD");

    return forwardPost(coordinatorUrl + "/api/v1/orchestrator/transfers", transferRequest);
}
```

Este flujo es importante porque evita que el frontend tenga que conocer la estructura interna del coordinador. El usuario solo selecciona cuentas y monto; el gateway se encarga de preparar la solicitud distribuida.

## Consulta del estado de una transaccion

El coordinador ejecuta las transferencias mediante el patron Saga. Por ello, una transferencia puede iniciar en estado `PENDING` y luego finalizar como `COMMITTED` o `ABORTED`.

Para que el frontend pueda consultar el resultado final, el gateway expone:

```http
GET /api/transactions/{transactionId}
```

Este endpoint redirige la consulta al coordinador:

```java
@GetMapping("/api/transactions/{transactionId}")
public Mono<ResponseEntity<Map<String, Object>>> getTransactionStatus(@PathVariable String transactionId) {
    return webClient.get()
            .uri(URI.create(coordinatorUrl + "/api/v1/orchestrator/transfers/" + transactionId))
            .exchangeToMono(response -> response.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {}))
            .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()))));
}
```

Con esto se completa el ciclo de comunicacion:

```text
Frontend inicia transferencia
    |
    v
Gateway envia solicitud al Coordinator
    |
    v
Coordinator ejecuta Saga entre bancos
    |
    v
Gateway permite consultar estado por transactionId
    |
    v
Frontend muestra resultado final
```

## Manejo de errores

El gateway realiza validaciones basicas antes de reenviar una operacion:

- Si falta una cuenta o un monto, responde `INVALID_REQUEST`.
- Si la cuenta tiene un prefijo desconocido, responde `INVALID_ACCOUNT`.
- Si un microservicio interno falla o no responde, retorna un error controlado con estado `500`.

Fragmento comun de envio:

```java
private Mono<ResponseEntity<Map<String, Object>>> forwardPost(String url, Map<String, Object> body) {
    return webClient.post()
            .uri(URI.create(url))
            .bodyValue(body)
            .exchangeToMono(response -> response.toEntity(new ParameterizedTypeReference<Map<String, Object>>() {}))
            .map(response -> ResponseEntity.status(response.getStatusCode()).body(response.getBody()))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()))));
}
```

Este manejo evita que errores internos se propaguen sin control hasta el frontend.

## Integracion con el frontend

El frontend React consume exclusivamente las rutas publicas del gateway. En el archivo `frontend/src/api/coordinatorApi.js` se define la URL base:

```javascript
const API_GATEWAY_BASE_URL = 'http://localhost:8080';
```

Las operaciones se envian mediante funciones dedicadas:

```javascript
export async function submitDeposit(payload) {
  return postGatewayOperation('/api/operations/deposit', payload);
}

export async function submitWithdraw(payload) {
  return postGatewayOperation('/api/operations/withdraw', payload);
}

export async function submitTransfer(payload) {
  return postGatewayOperation('/api/transfers', payload);
}

export async function getTransactionStatus(transactionId) {
  const response = await fetch(`${API_GATEWAY_BASE_URL}/api/transactions/${encodeURIComponent(transactionId)}`);
  const data = await response.json().catch(() => ({}));

  return {
    ok: response.ok,
    statusCode: response.status,
    data,
  };
}
```

Despues de iniciar una transferencia, el frontend consulta periodicamente el estado de la Saga usando el `transactionId`. De esta manera, la interfaz puede mostrar mensajes como transferencia iniciada, en proceso, completada o revertida.

## Validacion mediante pruebas

Para verificar el comportamiento del API Gateway se implementaron pruebas automatizadas en:

```text
services/api-gateway-service/src/test/java/pe/unsa/sd/gateway/controller/GatewayOperationControllerTest.java
```

Estas pruebas cubren:

- Enrutamiento de retiros al banco correspondiente segun el prefijo de cuenta.
- Rechazo de cuentas con prefijo invalido.
- Transformacion de una solicitud de transferencia hacia el formato esperado por el coordinador.
- Consulta del estado de una transaccion mediante `/api/transactions/{transactionId}`.

Ejemplo de caso probado:

```java
@Test
void transferDerivesOriginAndDestinationBanksFromAccountPrefixes() {
    Map<String, Object> payload = Map.of(
            "accessBank", "BANK_A",
            "sourceAccountId", "B-2001",
            "targetAccountId", "C-3001",
            "amount", "25.00");

    ResponseEntity<Map<String, Object>> response = controller.transfer(payload).block();

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    String body = coordinatorRequestBody.get();
    assertTrue(body.contains("\"originBankId\":\"BANK_B\""));
    assertTrue(body.contains("\"destinationBankId\":\"BANK_C\""));
    assertTrue(body.contains("\"originAccountId\":\"B-2001\""));
}
```

Este caso demuestra que el gateway no depende del banco desde donde accede el usuario, sino del prefijo real de la cuenta. Esto refuerza la transparencia de ubicacion exigida por el sistema distribuido.

## Aporte del API Gateway al proyecto

El API Gateway aporta al sistema los siguientes beneficios:

- Transparencia de acceso: el usuario opera desde una sola interfaz sin conocer los microservicios internos.
- Bajo acoplamiento: el frontend no depende directamente de las rutas de cada banco.
- Enrutamiento centralizado: las operaciones se dirigen al banco correcto segun la cuenta.
- Integracion con Saga: las transferencias se delegan al coordinador para mantener consistencia distribuida.
- Observabilidad basica: se exponen endpoints de salud para verificar el estado del gateway, coordinador y bancos.
- Escalabilidad: se pueden agregar nuevos bancos extendiendo la configuracion y la regla de identificacion.

En conclusion, el API Gateway actua como la capa de entrada del ecosistema bancario distribuido. Su implementacion permite unificar la comunicacion entre frontend, coordinador y bancos, manteniendo ocultos los detalles internos de la red de microservicios y facilitando una experiencia de usuario transparente.
