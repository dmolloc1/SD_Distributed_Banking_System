# Sistema Bancario Distribuido

Base funcional para un sistema bancario distribuido academico.

El proyecto contiene tres servicios bancarios independientes, un servicio coordinador, un API Gateway y una aplicacion frontend React + Vite. En esta etapa los datos se leen desde archivos JSON locales y no existe base de datos centralizada.

## Tecnologias

- Java 17
- Spring Boot
- Spring Cloud Gateway
- Spring Boot Actuator
- Maven
- REST
- JSON files
- LOG files
- React
- Vite

## Estructura

```text
README.md
.gitignore
docs/
  01-arquitectura-general/
  02-sistema-archivos-distribuido/
services/
  api-gateway-service/
  bank-a-service/
  bank-b-service/
  bank-c-service/
  common-persistence/
  coordinator-service/
frontend/
```

## Puertos, Mapeo de bancos

| Servicio | Puerto |
| --- | --- |
| api-gateway-service | 8080 |
| bank-a-service | 8081 |
| bank-b-service | 8082 |
| bank-c-service | 8083 |
| coordinator-service | 8090 |
| frontend | 5173 |

## Nomenclatura
Cliente: C001,C002...
Cuenta Banco 1: A-1001...
Cuenta Banco 2: B-2001...
Cuenta Banco 3: C-3001...

## Ejecucion local

```bash
cd services/bank-a-service && mvn spring-boot:run
cd services/bank-b-service && mvn spring-boot:run
cd services/bank-c-service && mvn spring-boot:run
cd services/coordinator-service && mvn spring-boot:run
cd services/api-gateway-service && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

## Endpoints publicos del API Gateway

```bash
curl http://localhost:8080/api/gateway/health
curl http://localhost:8080/api/coordinator/health
curl http://localhost:8080/api/bank-a/health
curl http://localhost:8080/api/bank-b/health
curl http://localhost:8080/api/bank-c/health
curl http://localhost:8080/api/customers/C005/accounts
curl http://localhost:8080/api/v1/orchestrator/transfers
curl http://localhost:8081/api/v1/bank/accounts/C001/debit
```

## Endpoints internos existentes

Los endpoints internos no deben ser consumidos directamente por el frontend. Se documentan para pruebas tecnicas de servicios aislados.

```bash
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
curl http://localhost:8090/distributed/health
curl http://localhost:8081/clients/C001/accounts
curl http://localhost:8082/clients/C002/accounts
curl http://localhost:8083/clients/C003/accounts
curl http://localhost:8090/distributed/accounts/C005
```

## Endpoints preparados pendientes

El API Gateway expone estos contratos publicos como esqueleto. Actualmente responden `501 Not Implemented` hasta que el coordinator-service implemente la logica transaccional.

```bash
curl -X POST http://localhost:8080/api/operations/deposit
curl -X POST http://localhost:8080/api/operations/withdraw
curl -X POST http://localhost:8080/api/transfers
curl http://localhost:8080/api/transactions/TX-001
```

## Flujo actual

```text
Frontend React
  -> API Gateway
  -> Coordinator Service
  -> Bank Services
  -> JSON files
```

El frontend consume solamente el API Gateway en `http://localhost:8080`.

## Pendiente

- Implementar depositos, retiros, transferencias, reserva, confirmacion y rollback.
- Integrar los bancos con `common-persistence`.
- Agregar Dockerfile por servicio y `docker-compose.yml`.
- Agregar pruebas automatizadas de Gateway e integracion.
