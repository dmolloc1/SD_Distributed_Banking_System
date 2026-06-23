# Sistema Bancario Distribuido

Base funcional de Hito 1 para un sistema bancario distribuido academico.

El proyecto contiene tres servicios bancarios independientes, un servicio coordinador y una aplicacion frontend React + Vite. En esta etapa los datos se leen desde archivos JSON locales y no existe base de datos ni logica de transacciones distribuidas.

## Tecnologias

- Java 17
- Spring Boot
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
  arquitectura/
  informe/hito-1/
  evidencias/capturas-repositorio/
  evidencias/estructura-proyecto/
services/
  bank-a-service/
  bank-b-service/
  bank-c-service/
  coordinator-service/
frontend/
```

## Puertos

| Servicio | Puerto |
| --- | --- |
| coordinator-service | 8080 |
| bank-a-service | 8081 |
| bank-b-service | 8082 |
| bank-c-service | 8083 |
| frontend | 5173 |

## Ejecucion local

```bash
cd services/bank-a-service && mvn spring-boot:run
cd services/bank-b-service && mvn spring-boot:run
cd services/bank-c-service && mvn spring-boot:run
cd services/coordinator-service && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

## Endpoints de prueba

```bash
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
curl http://localhost:8080/distributed/health
curl http://localhost:8081/clients/C001/accounts
curl http://localhost:8082/clients/C002/accounts
curl http://localhost:8083/clients/C003/accounts
curl http://localhost:8080/distributed/accounts/C005
```

## Nota

This is Hito 1. Deposits, withdrawals, distributed transfers, 2PC, rollback, concurrency control, Docker, and Docker Compose will be implemented in Hito 2.
