# Integración de Spring Cloud Netflix Eureka

Esta sección documenta la arquitectura de descubrimiento de servicios implementada en el ecosistema bancario distribuido utilizando **Spring Cloud Netflix Eureka**.

## Arquitectura de Descubrimiento de Servicios

En un ecosistema de microservicios, las instancias de los servicios pueden cambiar sus direcciones IP o puertos dinámicamente. Para evitar el acoplamiento fuerte y la gestión manual de URLs (como `http://localhost:8081`), se ha introducido **Eureka Service Discovery**, que actúa como un directorio centralizado donde los servicios se registran (proveedores) y consultan (consumidores) para encontrarse entre sí.

La arquitectura actual consta de 6 microservicios conectados mediante Eureka:

1. **Eureka Server** (`eureka-server-service`): El registro central.
2. **API Gateway** (`api-gateway-service`): Cliente de Eureka. Enruta las peticiones de los clientes (frontend) hacia los servicios correctos resolviendo sus nombres a través de Eureka.
3. **Coordinator Service** (`coordinator-service`): Cliente de Eureka. Orquesta las sagas y transferencias distribuidas, ubicando a los bancos correspondientes consultando a Eureka.
4. **Bank A Service** (`bank-a-service`): Cliente de Eureka. Se registra como proveedor.
5. **Bank B Service** (`bank-b-service`): Cliente de Eureka. Se registra como proveedor.
6. **Bank C Service** (`bank-c-service`): Cliente de Eureka. Se registra como proveedor.

---

## Módulo Eureka Server

Se creó un nuevo módulo `eureka-server-service` con las siguientes características:
- **Puerto:** `8761`
- **Anotación Principal:** `@EnableEurekaServer` en `EurekaServerApplication.java`
- **Configuración (application.yml):** Operando en modo "standalone" (no se registra a sí mismo ni obtiene el registro de otros nodos) para el entorno de desarrollo local. La auto-preservación (`enable-self-preservation`) ha sido desactivada para eliminar rápidamente las instancias muertas.

---

## Configuración de Clientes Eureka

Para que los 5 microservicios restantes participen en el ecosistema, se realizaron tres pasos fundamentales en cada uno:

### 1. Dependencias (pom.xml)
Se integró el Spring Cloud BOM (Bill of Materials) versión `2023.0.3` y la dependencia `spring-cloud-starter-netflix-eureka-client`.

### 2. Activación (@EnableDiscoveryClient)
Se añadió la anotación `@EnableDiscoveryClient` a las clases principales (con `@SpringBootApplication`) de cada servicio, indicándole a Spring Boot que debe registrar el servicio en Eureka al iniciar.

### 3. Configuración YAML (application.yml)
Se configuró la URL del servidor Eureka para que los clientes sepan dónde conectarse, junto con el nombre lógico del servicio (`spring.application.name`):

```yaml
spring:
  application:
    name: bank-a-service # Ejemplo

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

---

## Adaptación de la Comunicación HTTP con Balanceo de Carga

El cambio más importante es que los servicios ya no utilizan URLs estáticas, sino los **nombres de las aplicaciones** (`spring.application.name`) registrados en Eureka.

### Coordinator Service (`RestTemplate`)
El `CoordinatorService` interactúa con los bancos para obtener balances y orquestar transacciones. 
- Se inyectó un `RestTemplate` y se decoró con la anotación **`@LoadBalanced`**. 
- Las variables de entorno de las URLs de los bancos se actualizaron de `http://localhost:808x` a `http://bank-x-service`.
- **Ventaja:** Cuando el `RestTemplate` intenta hacer un POST a `http://bank-a-service/api/...`, el balanceador de carga intercepta la petición, consulta a Eureka por la IP real del servicio `bank-a-service` y completa la petición HTTP sin necesidad de conocer de antemano el puerto exacto.

### API Gateway (`Spring Cloud Gateway` y `WebClient`)
El API Gateway cumple un doble rol comunicándose con el backend:
1. **Rutas (application.yml):** Las rutas directas ahora utilizan el prefijo `lb://` (Load Balancer) en lugar de HTTP directo:
   ```yaml
   routes:
     - id: coordinator-transfers
       uri: lb://coordinator-service
   ```
2. **Controlador Programático (WebClient):** Para operaciones que requieren lógica (como agregar cuentas de varios bancos), el Gateway utiliza `WebClient`. Se reemplazó el `WebClient` básico por un `WebClient.Builder` anotado con `@LoadBalanced`, permitiendo que las llamadas a los nombres de los servicios (e.g., `bank-a-service`) sean resueltas dinámicamente.

---

## Orden de Ejecución

Para iniciar el entorno completo, los servicios deben arrancarse en el siguiente orden, garantizando que el registro esté disponible cuando los clientes se conecten:

1. `eureka-server-service` (http://localhost:8761)
2. `bank-a-service`
3. `bank-b-service`
4. `bank-c-service`
5. `coordinator-service`
6. `api-gateway-service`

Una vez iniciados, todos los servicios aparecerán en el Dashboard de Eureka, confirmando que la red distribuida se ha configurado exitosamente.
