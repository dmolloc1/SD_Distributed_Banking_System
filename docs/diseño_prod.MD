![](data:image/png;base64...)

# Introducción

En la actualidad, los sistemas distribuidos son fundamentales para el desarrollo de aplicaciones que requieren compartir información entre diferentes ubicaciones de manera eficiente y segura. En este trabajo se presenta el diseño e implementación de un sistema bancario distribuido conformado por tres bancos interconectados, capaces de realizar operaciones entre sí como si pertenecieran a una sola entidad financiera. El proyecto busca resolver problemas relacionados con la gestión de datos distribuidos, la comunicación entre nodos y la consistencia de las transacciones, considerando además que la información se almacena localmente en archivos JSON en lugar de bases de datos centralizadas. De esta manera, se pone en práctica el uso de conceptos de sistemas distribuidos, concurrencia y coordinación de procesos para garantizar que las operaciones bancarias se realicen de forma correcta, confiable y transparente para los usuarios.

# Descripción del Problema

El desafío principal consiste en diseñar e implementar un sistema bancario distribuido lo suficiente como para un consorcio de tres bancos asociados en diferentes países pero interconectados donde realicen transacciones transfronterizas como si fueran locales.

La problemática técnico-académica se divide en tres ejes críticos:

## Problemas criticos

A diferencia de un entorno convencional con bases de datos centralizadas, la información de las cuentas debe almacenarse localmente en archivos planos (JSON/texto) en cada nodo, lo que obliga al software a gestionar la concurrencia a bajo nivel. El sistema debe soportar con éxito la siguiente distribución obligatoria de datos:

* Cada banco tendrá al menos 3 clientes, y cada cliente al menos 3 cuentas.
* Clientes Exclusivos: Mínimo un cliente cuyos datos existen solo en su banco local.
* Cliente Bicorporativo: Un cliente con cuentas activas en dos bancos simultáneamente.
* Cliente Global: Un cliente con cuentas registradas en las tres entidades bancarias.

Cuando un usuario inicia sesión desde cualquier banco y ejecuta operaciones compuestas (por ejemplo, retirar de una cuenta en el Banco A y depositar ese retiro en el Banco B), la desconexión de los archivos locales genera vulnerabilidades críticas

# Marco teórico

El presente marco teórico establece los fundamentos conceptuales para comprender los sistemas distribuidos y su aplicación en el dominio bancario. Un sistema bancario distribuido implica múltiples instituciones autónomas que deben coordinarse para ofrecer una experiencia unificada, permitiendo operaciones financieras desde cualquier entidad participante. Este entorno presenta desafíos fundamentales: comunicación confiable entre nodos, consistencia de datos replicados, coordinación para transacciones atómicas y control de concurrencia. El marco se organiza en seis secciones que cubren los fundamentos arquitectónicos y los mecanismos de coordinación requeridos en sistemas financieros distribuidos.

**Sistemas Distribuidos**

Los sistemas distribuidos se definen como colecciones de computadoras autónomas interconectadas que se presentan ante los usuarios como un sistema único y coherente. Burns et al. (2016) documentan cómo arquitecturas distribuidas a gran escala, como las implementadas en centros de datos, permiten abstraer recursos físicos subyacentes y facilitan el despliegue de aplicaciones con requisitos exigentes de disponibilidad y rendimiento. Estos sistemas se caracterizan por la concurrencia de procesos, la ausencia de un reloj global y la independencia de fallos, lo que introduce desafíos significativos en su diseño e implementación.

Entre las propiedades fundamentales de los sistemas distribuidos destacan la transparencia, la escalabilidad y la tolerancia a fallos. La transparencia oculta al usuario la complejidad de la distribución, presentando el sistema como una entidad única. La escalabilidad permite que el sistema crezca en capacidad sin degradar su rendimiento, añadiendo nuevos nodos según sea necesario. La tolerancia a fallos garantiza que el sistema pueda recuperarse de fallos parciales sin afectar la disponibilidad global. En el contexto bancario, estas propiedades son esenciales para garantizar la continuidad operativa y la integridad de los datos financieros.

**Comunicación entre Procesos Distribuidos**

La comunicación entre procesos distribuidos constituye el mecanismo fundamental que permite la interacción entre los componentes de un sistema distribuido. Yousefpour et al. (2019) analizan los paradigmas de comunicación en sistemas distribuidos, destacando la importancia de los protocolos y estrategias para gestionar la latencia y el ancho de banda en entornos con recursos heterogéneos. Este modelo basado en mensajes reemplaza la memoria compartida de sistemas centralizados, introduciendo nuevos desafíos relacionados con la fiabilidad y el orden de los mensajes.

Los modelos de comunicación se clasifican en síncronos y asíncronos. En la comunicación síncrona, el emisor queda bloqueado hasta recibir confirmación, mientras que en la asíncrona, el emisor continúa su ejecución sin esperar respuesta. Yousefpour et al. (2019) señalan que la comunicación asíncrona basada en mensajes ofrece mayor escalabilidad y flexibilidad, aspectos críticos en sistemas distribuidos modernos. La seguridad en la comunicación es un aspecto crítico, especialmente en sistemas financieros, donde se requiere garantizar la confidencialidad e integridad de los datos transmitidos entre instituciones.

**Sistema Distribuido de Archivos**

Un sistema distribuido de archivos permite a los usuarios acceder y compartir archivos almacenados en múltiples servidores de forma transparente. Ghemawat et al. (2003) presentan el Google File System (GFS), demostrando que la replicación de bloques y la partición de datos son fundamentales para lograr escalabilidad y tolerancia a fallos en sistemas distribuidos de archivos. Este sistema introduce conceptos como la replicación para tolerancia a fallos y la optimización para flujos de trabajo con grandes volúmenes de datos.

Las características fundamentales de estos sistemas incluyen transparencia de ubicación, transparencia de acceso, concurrencia y replicación. Ghemawat et al. (2003) demuestran que la replicación selectiva y la gestión eficiente de metadatos son cruciales para mantener la consistencia y durabilidad de los datos. En aplicaciones bancarias, estos sistemas permiten almacenar información de cuentas y transacciones en múltiples ubicaciones geográficas, garantizando disponibilidad y recuperación ante fallos.

**Coordinación y Acuerdo**

La coordinación en sistemas distribuidos se refiere a los mecanismos que permiten a los nodos autónomos tomar decisiones de manera conjunta y consistente. Ongaro y Ousterhout (2014) proponen Raft, un algoritmo de consenso diseñado para ser más comprensible que Paxos, facilitando su implementación práctica en sistemas reales. Raft divide el problema de consenso en subproblemas manejables: elección de líder, replicación de logs y seguridad, proporcionando una base sólida para construir sistemas distribuidos tolerantes a fallos.

Los algoritmos de consenso son fundamentales para lograr acuerdos en presencia de fallos. Ongaro y Ousterhout (2014) demuestran que Raft ofrece un rendimiento comparable a Paxos pero con una curva de aprendizaje más accesible. El protocolo de confirmación en dos fases (2PC) es ampliamente utilizado para garantizar atomicidad en transacciones distribuidas, aunque presenta limitaciones como el bloqueo ante fallos del coordinador. En el contexto bancario, estos mecanismos son esenciales para garantizar la consistencia de operaciones que involucran múltiples instituciones financieras.

**Transacciones Distribuidas**

Una transacción distribuida involucra operaciones sobre datos almacenados en múltiples nodos de un sistema distribuido. Harding et al. (2017) evalúan diferentes protocolos de control de concurrencia distribuido, incluyendo mecanismos basados en bloqueo y enfoques optimistas, analizando su rendimiento en escenarios con diferentes niveles de contención. Las transacciones distribuidas deben cumplir las propiedades ACID: atomicidad, consistencia, aislamiento y durabilidad.

Harding et al. (2017) revelan que la elección del protocolo adecuado depende críticamente de las características de la carga de trabajo y la topología de la red. Los protocolos basados en bloqueo ofrecen mejor rendimiento en escenarios de alta contención, mientras que los enfoques optimistas son superiores cuando la contención es baja. El protocolo de confirmación en dos fases (2PC) es el mecanismo más utilizado para coordinar transacciones distribuidas. En el contexto bancario, estas transacciones son críticas para operaciones como transferencias entre cuentas ubicadas en diferentes bancos, donde se debe asegurar que los fondos no se pierdan ni se dupliquen.

**Control de Concurrencia**

El control de concurrencia garantiza que múltiples transacciones que se ejecutan simultáneamente no interfieran entre sí, preservando la consistencia de los datos. Bernstein y Goodman (1981) establecen los fundamentos teóricos del control de concurrencia en bases de datos distribuidas, clasificando los diferentes enfoques como bloqueo, control de concurrencia optimista y técnicas basadas en marcas de tiempo. Su trabajo identifica los desafíos clave, como la gestión de interbloqueos y la serializabilidad.

Bernstein y Goodman (1981) proponen protocolos distribuidos para asegurar que las transacciones concurrentes produzcan resultados consistentes, proporcionando las bases teóricas para implementar mecanismos robustos que eviten condiciones de carrera. Los sistemas de control de concurrencia deben coordinarse entre múltiples nodos en entornos distribuidos, lo que añade complejidad a su implementación. En sistemas bancarios distribuidos, estos mecanismos son fundamentales para evitar condiciones de carrera cuando múltiples clientes intentan operar sobre la misma cuenta simultáneamente, garantizando la integridad de los saldos y la corrección de las operaciones financieras.

# Requerimientos del sistema

## Requerimientos Funcionales (RF)

**Gestión de Usuarios, Cuentas y Accesos**

RF-01 (Autenticación y Sesión Transfronteriza): El sistema debe permitir al usuario iniciar sesión en la interfaz de cualquiera de los 3 bancos y reconocer sus credenciales sin importar el país de origen.

RF-02 (Consolidación de Posición Financiera): El frontend debe unificar y mostrar en una sola pantalla todas las cuentas del cliente (locales, bicorporativas o globales), solicitando los saldos en paralelo a cada banco correspondiente.

RF-03 (Consulta de Historial de Transacciones): El usuario debe poder ver el historial de movimientos de cada cuenta, el cual se extraerá tanto de los archivos locales del banco como del registro de auditoría del orquestador.

**Gestión de Transacciones e Interoperabilidad**

RF-04 (Transferencias Locales e Interbancarias): El sistema debe procesar transferencias entre cuentas del mismo banco (locales) y entre cuentas de bancos diferentes (distribuidas).

RF-05 (Retiros y Depósitos Multi-Operación): El usuario debe poder encadenar transacciones (por ejemplo, retirar $100 del Banco 1, depositar $50 en el Banco 2 y $50 en el Banco 3) desde una sola orden en la interfaz.

RF-06 (Control de Fondos Insuficientes): El sistema debe validar que la cuenta de origen tenga saldo disponible suficiente antes de iniciar cualquier fase de bloqueo o transferencia.

RF-07 (Reversión Automática por Fallo): Si la cuenta de destino es rechazada o el nodo destino se cae a mitad del proceso, el sistema debe ejecutar una acción compensatoria para devolver íntegramente el dinero a la cuenta de origen.

**Gestión de Almacenamiento y Archivos**

RF-08 (Persistencia de Estados Financieros): El sistema debe permitir la creación, lectura y actualización de saldos modificando directamente los archivos planos (JSON/texto) locales de cada banco.

RF-09 (Generación de Archivos Temporales de Bloqueo): Durante una transacción distribuida, el sistema debe crear un archivo temporal (.tmp) que proyecte el nuevo saldo antes de consolidarlo en el archivo principal.

## Requerimientos No Funcionales (RNF)

Gestión de Consistencia, Concurrencia y Sincronización

RNF-01 (Manejo de Bloqueos en Archivos - FileLock): El backend en Spring Boot debe aplicar bloqueos exclusivos a nivel de sistema operativo (FileLock) sobre los archivos de datos para evitar que transacciones concurrentes corrompan la información.

RNF-02 (Protocolo 3PC Avanzado con Timeouts): Las transacciones entre archivos de diferentes bancos deben gobernarse por un flujo de tres fases: Can-Commit (validación), Pre-Commit (preparación en archivo .tmp) y Do-Commit (escritura final). Cada fase debe incluir un temporizador (timeout) para evitar bloqueos perpetuos si un nodo muere.

RNF-03 (Garantía de Estado con Temporal): El estado global de las multi-operaciones debe persistirse en el servidor de Temporal. Si un microservicio de Spring Boot se cae, Temporal debe retener el flujo del Workflow y reanudarlo exactamente en el punto donde se quedó al levantarse el servicio.

RNF-04 (Consistencia Eventual Controlada): Para operaciones complejas de larga duración, el sistema debe priorizar la disponibilidad, permitiendo que los saldos se actualicen de forma asíncrona y consistente en un lapso no mayor a 3 segundos tras la confirmación de Temporal.

Gestión de Arquitectura, Despliegue y Rendimiento

RNF-05 (Aislamiento por Contenedores Docker): Cada banco (Spring Boot), la interfaz web (React) y la infraestructura de Temporal deben ejecutarse en contenedores independientes y aislados dentro de una red virtual simbiótica de Docker.

RNF-06 (Comunicación mediante APIs REST y gRPC): La comunicación entre el Frontend y el Backend, y entre los microservicios de los bancos, debe ser mediante HTTP/REST, mientras que la comunicación de los trabajadores de Spring Boot con Temporal será mediante gRPC.

RNF-07 (Configurabilidad de Rutas de Datos): La ubicación de los archivos de almacenamiento de datos de cada banco debe ser parametrizable mediante variables de entorno en Docker, facilitando el cambio de rutas sin modificar el código.

Gestión de Seguridad y Usabilidad

RNF-08 (Auditoría e Inmutabilidad de Flujos): Todas las transacciones orquestadas deben registrarse de forma inmutable en el historial de Temporal, sirviendo como bitácora de auditoría ante fallos del sistema.

RNF-09 (Reactividad y Mensajes de Estado): La interfaz en React debe mostrar visualmente el estado del flujo transaccional en tiempo real (ej. "Validando fondos", "Preparando transferencia", "Transacción Completada") para mitigar la incertidumbre del usuario durante el proceso de tres fases.

# Técnicas Evaluadas

# Opciones de arquitectura

Se evaluaron tres paradigmas arquitectónicos principales para estructurar el sistema bancario distribuido:

| **Opción**                                           | **Descripcion**                                                                                     | **Ventajas**                                                                                                                   | **Desventajas**                                                                                                           |
| ---------------------------------------------------- | --------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------- |
| **Arquitectura Monolítica**                          | Todo el sistema bancario se implementa como una única aplicación desplegable.                       | Simplicidad de desarrollo inicial, sin latencia de red interna.                                                                | Un fallo afecta todo el sistema; escalabilidad limitada; dificulta la simulación de bancos independientes.                |
| **Arquitectura SOA (Service-Oriented Architecture)** | Los servicios se comunican mediante un Enterprise Service Bus (ESB) compartido.                     | Reutilización de servicios; mejor que el monolito para integración empresarial.                                                | El ESB se convierte en un punto único de fallo; alto acoplamiento entre servicios vía el bus; complejidad administrativa. |
| **Arquitectura de Microservicios**                   | Cada banco es un servicio independiente con su propio almacenamiento, deployable de forma autónoma. | Aislamiento de fallos; autonomía de datos por banco; escalabilidad independiente; simula fielmente un consorcio bancario real. | Mayor complejidad operacional; requiere orquestación de transacciones distribuidas.                                       |

**Tab.1.**  Opciones de Arquitectura

# Opciones de comunicación entre bancos

Se analizaron tres protocolos de comunicación para la interacción entre los nodos bancarios y entre el frontend y el backend:

| **Opción**  | **Descripcion**                                                                                                                                                                                                            | **Ventajas**                                                                                                                                                                    | **Desventajas**                                                                                                  |
| ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| **REST**    | Estilo arquitectónico basado en recursos accesibles a través de URLs independientes. El cliente realiza operaciones estándar mediante texto legible. (Usa HTTP/1.1 con JSON)                                               | Universal, amplia compatibilidad, fácil de depurar con Postman, bajo acoplamiento, ideal para APIs públicas.                                                                    | Mayor overhead por cabeceras HTTP; texto plano es menos eficiente que binario; no soporta streaming nativo.      |
| **gRPC**    | Sistema de llamada a funciones remotas de alto rendimiento. Permite que un servidor invoque código directamente en otro nodo usando mensajes binarios. (Usa HTTP/2 con Protocol Buffers)                                   | Hasta 10 veces más rápido que REST al comprimir datos; permite enviar y recibir información en tiempo real simultáneamente; usa reglas estrictas que evitan fallos de conexión. | Mayor curva de aprendizaje; requiere tooling especializado; menos legible para depuración directa.               |
| **GraphQL** | Lenguaje de consulta flexible mediante un único punto de acceso. El cliente (frontend) envía un script solicitando las propiedades exactas que necesita del backend en un solo viaje. (Usa HTTP con lenguaje de consultas) | El cliente define exactamente qué datos necesita; reduce over-fetching y under-fetching.                                                                                        | Complejidad de implementación alta; caching difícil; requiere análisis de complejidad de queries para seguridad. |

**Tab. 2**  Opciones de comunicación entre bancos

# Opciones de almacenamiento en archivos

Dado el requerimiento explícito de almacenar datos localmente sin bases de datos centralizadas, se evaluaron tres alternativas de persistencia por nodo

| **Opción**                        | **Descripcion**                                                                                                                                                                    | **Ventajas**                                                                                              | **Desventajas**                                                                                                                 |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **Archivos JSON planos**          | Organiza la información con formato de clave-valor. Permite representar directamente entidades complejas como Clientes Globales o Bicorporativos en archivos individuales legibles | Legibilidad humana; esquema flexible y extensible; nativo en Java/Spring; control total sobre estructura. | Sin motor ACID nativo; la concurrencia debe gestionarse a nivel de aplicación con File Locking.                                 |
| **SQLite (archivo .db)**          | Es un motor de base de datos relacional ligero contenido en un único archivo físico. No requiere instalar un servidor.                                                             | Soporte ACID nativo; consultas SQL; portabilidad total en un solo archivo.                                | No está diseñado para entornos distribuidos; no soporta replicación; concurrencia limitada con múltiples escritores simultáneos |
| **Archivos de texto plano / CSV** | Almacena los datos en líneas continuas de texto, donde cada dato se separa únicamente por una coma o un guión.                                                                     | Máxima simplicidad; compatible con cualquier herramienta.                                                 | Sin soporte para estructuras anidadas; muy limitado para datos complejos como cuentas con múltiples campos.                     |

**Tab. 3** Opciones de almacenamiento en archivos

# Opciones de coordinación y acuerdo

Para garantizar la consistencia en transacciones que involucran múltiples nodos bancarios se evaluaron los siguientes mecanismos de coordinación distribuida:

| **Opción**                             | **Descripcion**                                                                                                                                                   | **Ventajas**                                                                                                                               | **Desventajas**                                                                                                           |
| -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------- |
| **Protocolo 2PC (Two-Phase Commit)**   | Coordinación en dos fases: Prepare y Commit/Abort. El coordinador espera confirmación de todos los participantes.                                                 | Garantiza atomicidad; ampliamente conocido y documentado.                                                                                  | El coordinador se convierte en punto único de fallo; bloqueo indefinido si el coordinador cae en medio de la transacción. |
| **Protocolo 3PC (Three-Phase Commit)** | Extiende el 2PC añadiendo una fase intermedia Pre-Commit, lo que permite que los participantes tomen decisiones autónomas ante timeout.                           | Elimina el bloqueo indefinido del 2PC; los nodos pueden abortar de forma autónoma si no reciben respuesta del coordinador a tiempo.        | Mayor complejidad de implementación; más mensajes de red; mayor latencia por la fase adicional.                           |
| **Patrón Saga (Orquestado)**           | La transacción distribuida se divide en pasos locales secuenciales. Un orquestador central coordina cada paso y ejecuta transacciones compensatorias ante fallos. | Sin bloqueos distribuidos; adecuado para procesos de larga duración; alta resiliencia; consistencia eventual; escalable en microservicios. | La lógica de compensación (rollback) debe diseñarse explícitamente para cada operación.                                   |

**Tab. 4** Opciones de coordinación y acuerdo

# Opciones de transacciones y concurrencia

Se evaluaron tres estrategias para controlar el acceso concurrente a los archivos de datos de cada nodo bancario:

| **Opción**                                   | **Descripcion**                                                                                                                                  | **Ventajas**                                                                                                                    | **Desventajas**                                                                                                                         |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| **Bloqueo Pesimista**                        | Antes de cualquier lectura/escritura se adquiere un bloqueo exclusivo sobre el archivo. Las transacciones entrantes esperan en cola con timeout. | Máxima consistencia; ideal cuando múltiples clientes operando sobre la misma cuenta; evita condiciones de carrera directamente. | Puede generar deadlocks si no se gestiona correctamente el orden de bloqueos; reduce el rendimiento en escenarios de baja contención.   |
| **Bloqueo Optimista**                        | No bloquea el archivo al leer; verifica antes de escribir si el dato fue modificado por otro proceso; si hay conflicto, reintenta.               | Alto rendimiento en escenarios de baja contención; no genera bloqueos ni esperas prolongadas.                                   | En contextos bancarios de alta contención, los reintentos constantes degradan el rendimiento; menor garantía de consistencia inmediata. |
| **MVCC (Multi-Version Concurrency Control)** | Cada transacción trabaja sobre una versión inmutable del dato; se resuelven conflictos al consolidar versiones.                                  | Elimina bloqueos de lectura; alto rendimiento para lecturas concurrentes.                                                       | Complejo de implementar sobre archivos planos JSON; requiere gestión de versiones y garbage collection.                                 |

**Tab. 5** Opciones de transacciones y concurrencia

# Técnicas Seleccionadas

Tomando como referencia las opciones evaluadas en la sección anterior, se seleccionaron las siguientes técnicas para la implementación del sistema bancario distribuido.

# Arquitectura seleccionada

Se seleccionó la arquitectura de microservicios como paradigma base del sistema. Cada entidad bancaria (Banco A, Banco B, Banco C) se implementa como un microservicio independiente en Spring Boot, con su propio sistema de archivos local aislado mediante Docker Volumes.

Esta elección responde directamente a los requerimientos RNF-05 (aislamiento por contenedores Docker) y al principio de autonomía de datos. El aislamiento de fallos garantiza que un fallo crítico en un Banco no comprometa la operatividad de los otros.

**Justificación frente a alternativas:** La arquitectura monolítica fue descartada porque impediría simular tres bancos verdaderamente independientes. La arquitectura SOA fue descartada porque el ESB centralizado contradice el principio de autonomía de datos por nodo y representa un punto único de fallo incompatible con el requerimiento de tolerancia a fallos (RNF-03).

# Comunicación seleccionada

La comunicación seleccionada es la de REST/HTTP para el plano de usuario y gRPC para el plano de coordinación:

* **REST/JSON sobre HTTP**: utilizado para la comunicación entre el Frontend (React) y el API Gateway, y entre el API Gateway y los servicios de consulta. Se seleccionó por su universalidad, compatibilidad con navegadores web y facilidad de depuración con Postman.
* **gRPC sobre HTTP/2:** utilizado para la comunicación interna entre el Saga Orchestrator y los Bank Core Nodes en operaciones críticas (reserve, confirm, rollback). Su menor latencia permite mantener los bloqueos de archivos durante el menor tiempo posible, reduciendo asi la ventana de contención sobre el sistema de archivos local.

# Almacenamiento seleccionado

Se seleccionaron archivos JSON planos como mecanismo de persistencia principal, en cumplimiento explícito del requerimiento RF-08 (Persistencia de Estados Financieros) y RF-09 (Generación de Archivos Temporales de Bloqueo). El formato JSON permite la extensibilidad del esquema para futuros requerimientos de cumplimiento (KYC) sin comprometer la compatibilidad con versiones anteriores.

SQLite fue descartado porque, aunque ofrece ACID nativo, no está diseñado para entornos distribuidos ni para la replicación entre nodos, lo cual es muy importante para el proyecto a desarrollar y su modelo de un único archivo con escritores concurrentes limita el control explícito requerido por el requerimiento RNF-01 (FileLock). Los archivos CSV/texto plano fueron descartados por su incapacidad para representar estructuras de datos anidadas como las cuentas con múltiples atributos.

# Coordinación seleccionada

Se seleccionó el Patrón Saga Orquestado como mecanismo de coordinación de transacciones distribuidas. Este patrón divide cada operación interbancaria en pasos locales secuenciales, con transacciones compensatorias, o también llamados rollbacks lógicos, ante fallos en cualquier nodo participante.

El 2PC fue descartado por su vulnerabilidad ante fallos del coordinador (bloqueo indefinido), incumpliendo el requerimiento RNF-02 que exige timeouts por fase. El 3PC, aunque resuelve el bloqueo del 2PC, introduce una tercera fase que añade latencia de red innecesaria dado que el Saga con compensación cubre el mismo objetivo con mayor flexibilidad para procesos de larga duración. Adicionalmente, el requerimiento RNF-03 establece que el estado global debe persistirse en Temporal, lo que es nativo al patrón Saga orquestado.

# Control de transacciones y concurrencia seleccionado

Se seleccionó el bloqueo pesimista implementado. Este mecanismo garantiza la serializabilidad de las transacciones (depósitos/retiros simultáneos), previniendo condiciones de carrera cuando múltiples clientes intentan operar sobre la misma cuenta, en línea con los fundamentos teóricos de Bernstein y Goodman (1981) citados en el marco teórico del documento.

Las transacciones entrantes sobre una cuenta ya bloqueada esperan en una cola con timeout predefinido para evitar bloqueos infinitos, conforme a la sección Control de Cuentas y Concurrencia del documento (Mecanismo de Seguridad: Bloqueo Pesimista).

El bloqueo optimista fue descartado porque en un sistema bancario la contención sobre cuentas específicas puede ser alta (múltiples clientes operando simultáneamente), y los reintentos frecuentes degradarían el rendimiento y comprometerían la consistencia. El MVCC fue descartado por su complejidad de implementación sobre archivos planos JSON, lo cual requeriría gestión de versiones sin soporte nativo del sistema de archivos.

# Tecnologías de implementación seleccionadas

Para el desarrollo del proyecto se considero las siguientes tecnologías, buscando que permitan el desarrollo de un sistema modular y que permita la intercomunicación entre las tres entidades bancárias

**Arquitectura:** Microservicios

Se utilizará una arquitectura basada en microservicios, donde cada entidad bancaria será representada como un servicio independiente. Esto permitirá separar el Banco A, Banco B, Banco C y el servicio coordinador, facilitando la comunicación entre componentes y representando de mejor manera un sistema distribuido. Su selección se debe a que permite trabajar con módulos independientes, organizar mejor el desarrollo y simular bancos interconectados sin depender de una sola aplicación centralizada.

**Backend:** Spring Boot

Se utilizará para la implementación de las entidades bancarias(A, B, C), donde se considerará a cada uno como un servicio separado al igual que el servicio coordinador. Su selección se debe a que permite crear APIs REST de forma ordenada, separa la lógica del sistema en controladores, servicios y modelos, además de facilitar la construcción de aplicaciones independientes.

**Frontend:** React

Se utilizará para la implementación de la interfaz web del sistema, desde donde el usuario podrá consultar sus cuentas, visualizar saldos y solicitar operaciones como depósitos, retiros y transferencias. Su selección se debe a que permite construir interfaces dinámicas mediante componentes reutilizables, además de facilitar la conexión con servicios backend mediante peticiones HTTP.

**Comunicación:** REST

Se utilizará para la comunicación entre el frontend, el servicio coordinador y los servicios bancarios. Su selección se debe a que REST permite el intercambio de información mediante peticiones HTTP, es compatible con Spring Boot y React, y puede probarse fácilmente con herramientas como Postman. En el sistema, REST permitirá que el coordinador consulte y envíe operaciones a los bancos correspondientes.

**Despliegue:** Docker y Docker Compose

Se utilizarán Docker y Docker Compose para el despliegue local del sistema bancario distribuido. Docker permitirá ejecutar cada componente del sistema de forma independiente, como el Banco A, Banco B, Banco C, el servicio coordinador y el frontend. Esto ayuda a simular un entorno distribuido y evita problemas de configuración entre equipos.

Docker Compose se utilizará para levantar todos los servicios desde un único archivo docker-compose.yml, definiendo puertos, redes y dependencias necesarias. Esto permite ejecutar el sistema completo con un solo comando, sin tener que iniciar cada servicio manualmente.

**Almacenamiento:** JSON y LOG

Se utilizarán archivos JSON para almacenar la información de clientes y cuentas de cada banco, cumpliendo con el requerimiento de guardar los datos en archivos. Además, se utilizarán archivos LOG para registrar las operaciones realizadas, como depósitos, retiros y transferencias. Esto permitirá mantener la trazabilidad de las transacciones y apoyar la recuperación ante posibles fallos.

**Pruebas:** Postman y k6

Se utilizará Postman para validar los endpoints REST del sistema, verificando operaciones como consulta de cuentas, depósitos, retiros y transferencias. También se considera k6 para realizar pruebas de concurrencia, especialmente en escenarios donde varias solicitudes intenten operar sobre una misma cuenta. Estas pruebas permitirán evidenciar el correcto funcionamiento del sistema y apoyar la evaluación de transacciones y control de concurrencia.

**Gestión del código fuente:** GitHub

Se utilizará GitHub como repositorio central del proyecto, donde se almacenará el código fuente del sistema bancario distribuido.

# Arquitectura Propuesta del Sistema

## Descripción general de la Arquitectura

El sistema se fundamenta en un paradigma de Arquitectura de Microservicios con Orquestación de Transacciones Distribuidas. Se ha seleccionado este modelo para reflejar la realidad operativa de un consorcio bancario internacional, donde cada entidad (Nodo Bancario) mantiene autonomía total sobre sus recursos de cómputo y persistencia, pero interoperan bajo un protocolo común de consenso.

La justificación de este modelo se fundamenta en las siguientes capacidades principales que el sistema bancario debe cumplir:

* **Escalabilidad y Aislamiento de Fallos**: Al fragmentar el sistema en unidades autónomas, un fallo crítico en el sistema de archivos del Banco "A" no compromete la operatividad de los Bancos "B" o "C". Esto reduce el radio de explosión de errores técnicos.
* **Autonomía de Datos (Sovereignty)**: Cada banco posee su propia persistencia. La arquitectura distribuida permite que las reglas de negocio de cada país (país de origen de cada banco) se encapsulen dentro de su microservicio correspondiente sin contaminar el dominio de los demás.

Para abordar los riesgos inherentes de una arquitectura distribuida, es necesario considerar principalmente los desafíos relacionados con la consistencia y la latencia.

* **Consistencia Distribuida**: Ante la ausencia de una base de datos centralizada con soporte ACID global, se implementa el Patrón Saga Orquestada. Este patrón garantiza la Atomicidad mediante transacciones compensatorias (rollbacks lógicos) en caso de fallos en alguno de los nodos participantes.
* **Gestión de Latencia**: Para minimizar el impacto del overhead de red entre bancos, se utiliza una separación clara entre el Plano de Control (orquestación y validación rápida) y el Plano de Datos (escritura pesada en archivos y reconciliación), utilizando comunicaciones no bloqueantes donde sea posible.

## Componentes del sistema

El ecosistema distribuido está compuesto por una serie de componentes críticos, donde cada uno posee una responsabilidad específica y aislada dentro de la arquitectura. Esta separación permite mantener un bajo acoplamiento entre servicios, facilitar la escalabilidad independiente y garantizar que cada componente controle únicamente el dominio que le corresponde.

| **Componente**                        | **Responsabilidad dentro del sistema**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| API Gateway (Edge Service)            | Actúa como el punto único de entrada entre el frontend desarrollado en React y los servicios internos del ecosistema. Implementado con Spring Cloud Gateway, se encarga del enrutamiento dinámico de solicitudes, terminación de seguridad mediante SSL/TLS y agregación de respuestas para construir vistas consolidadas del cliente.                                                                                                                                                                                   |
| Global Transaction Orchestrator (GTO) | Representa el componente encargado de coordinar las operaciones distribuidas. No mantiene información de saldos ni datos contables, sino que administra el estado de las transacciones distribuidas y controla el flujo de operaciones interbancarias. Su función principal es garantizar la consistencia del proceso: por ejemplo, si un débito en el Banco A se completa correctamente, debe asegurar la confirmación del crédito en el Banco B o ejecutar las acciones compensatorias correspondientes ante un fallo. |
| Federated Identity Service (FIS)      | Servicio encargado de gestionar la identidad federada dentro del ecosistema bancario. Mantiene el mapeo entre clientes y sus relaciones con diferentes entidades bancarias, permitiendo construir una "vista de unión" del usuario sin duplicar información financiera ni alterar la responsabilidad de cada banco sobre sus propios saldos.                                                                                                                                                                             |
| Bank Core Nodes (Instancias A, B y C) | Representan los núcleos bancarios independientes del sistema. Cada nodo implementa la lógica contable mediante microservicios desarrollados con Spring Boot y mantiene la autoridad exclusiva sobre la lectura y escritura de su propio FileSystem Local. Exponen APIs privadas para operaciones como reserva de fondos, confirmación de movimientos y ejecución de reversos.                                                                                                                                            |
| Local Persistence Manager (LPM)       | Componente interno de cada Bank Core Node responsable de administrar la persistencia local basada en archivos. Implementa mecanismos de control de concurrencia mediante File Locking utilizando Java NIO, además de estrategias de escritura atómica para evitar corrupción de información durante accesos simultáneos o fallos inesperados.                                                                                                                                                                            |

**Tab. 1** Componentes del sistema

## Diagrama de Arquitectura

El siguiente diagrama representa la topología del sistema.

![](data:image/png;base64...)

**Fig. 1** Topología del sistema

## Flujo general de comunicación

El sistema define diferentes mecanismos de comunicación según el tipo de interacción requerida. Las comunicaciones orientadas al usuario priorizan simplicidad e interoperabilidad, mientras que las comunicaciones internas entre componentes críticos priorizan eficiencia, tipado fuerte y reducción de latencia.

| **Tipo de comunicación**     | **Componentes involucrados**                  | **Protocolo**        | **Propósito**                                                                                                                                                                                                                                                                                                                                                   |
| ---------------------------- | --------------------------------------------- | -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Comunicación síncrona        | Frontend , API Gateway, Servicios de consulta | REST/JSON sobre HTTP | Utilizada para las interacciones directas con el usuario y operaciones de consulta. REST permite una integración sencilla con aplicaciones web, facilita la depuración y mantiene un bajo acoplamiento entre frontend y backend. Se utilizan Idempotency Keys en las cabeceras HTTP para evitar la duplicación de operaciones cuando ocurren reintentos de red. |
| Comunicación de coordinación | Saga Orchestrator, Bank Core Nodes            | gRPC sobre HTTP/2    | Utilizada para operaciones críticas entre servicios bancarios internos. gRPC permite comunicación eficiente mediante Protocol Buffers, proporcionando contratos fuertemente tipados y reduciendo errores de integración entre nodos bancarios. Su menor latencia permite mantener bloqueos de archivos durante el menor tiempo posible.                         |

**Tab. 2** Tipos de comunicación

Una operación interbancaria sigue un flujo coordinado por el Global Transaction Orchestrator, quien administra el estado de la transacción y garantiza la consistencia mediante acciones compensatorias cuando ocurre un fallo.

1. *Inicio de la transacción*

El usuario genera una solicitud de transferencia. El API Gateway recibe la petición y la deriva al Global Transaction Orchestrator, quien inicia el proceso distribuido.

1. *Fase de preparación (Locking / Reserve)*

El Orquestador solicita al banco origen la reserva temporal de los fondos. El Bank Core Node bloquea el registro correspondiente dentro del almacenamiento local para evitar modificaciones concurrentes.

1. *Fase de ejecución (Commit parcial)*

Una vez reservados los fondos, el Orquestador solicita al banco destino la aplicación del crédito correspondiente.

1. *Fase de confirmación (Commit definitivo)*

Si todos los nodos participantes responden correctamente, el Orquestador confirma la operación. Los bancos liberan los bloqueos y actualizan la información persistida junto con los metadatos de auditoría correspondientes.

1. *Fase de compensación (Rollback lógico)*

Si alguno de los pasos falla, el Orquestador ejecuta una transacción compensatoria. Por ejemplo, si el Banco Destino no puede completar el crédito, se envía una instrucción de cancelación al Banco Origen para liberar la reserva realizada previamente.

# Diseño del Sistema Distribuido de Archivos

## Organización y Aislamiento de Datos

Para asegurar la soberanía de la información y la multi-tenencia, cada microservicio de banco opera en un entorno aislado mediante Docker Volumes.

| **Directorio** | **Propósito**                            | **Tipo de Datos**   |
| -------------- | ---------------------------------------- | ------------------- |
| /identity/     | Almacenamiento de metadatos de clientes. | JSON (Documento)    |
| /ledger/       | Estado actual de las cuentas y saldos.   | JSON (Estado)       |
| /journal/      | Registro histórico de transacciones.     | JSON (Append-only)  |
| /locks/        | Control de concurrencia y semáforos.     | Archivos de sistema |

**Tab. 3** Directorios de directorios */var/lib/bank-core/data/*

El acceso a los archivos es estrictamente local por nodo. La interoperabilidad y consistencia entre bancos se gestiona exclusivamente a través del Saga Orchestrator, emulando protocolos financieros internacionales como ISO 20022 o SWIFT MT.

## Gestión de Identidad (Entidad Cliente)

La información de identidad reside en/identity/customer\_{id}.json. El sistema emplea un esquema federado para el reconocimiento inter-nodo.

| **Campo**        | **Tipo**  | **Descripción**                                 |
| ---------------- | --------- | ----------------------------------------------- |
| customerId       | UUID-STR  | Identificador único global.                     |
| identityDocument | String    | DNI, Pasaporte o identificación legal.          |
| fullName         | String    | Nombre completo del titular.                    |
| clientType       | ENUM      | Categoria: *INDIVIDUAL* o CORPORATE.            |
| homeBank         | String    | ID del banco de origen principal.               |
| affiliations     | Array     | Lista de bancos donde el cliente posee cuentas. |
| createdAt        | Timestamp | Fecha de creación en formato ISO-8601.          |

**Tab. 4** Estructura del JSON Schema

El formato JSON permite la evolución del esquema (extensibilidad) para futuros requerimientos de cumplimiento (KYC) sin comprometer la compatibilidad con versiones anteriores.

## Control de Cuentas y Concurrencia

Cada cuenta se almacena de forma individual en /ledger/account\_{number}.json. Se implementan mecanismos de alta precisión y protección contra condiciones de carrera.

### Atributos de la Cuenta

* accountNumber: Identificador único en formato IBAN.
* balance: Valor decimal de alta precisión (mapeado a BigDecimal en Java).
* currency: Código de divisa según ISO 4217.
* status: Estados operativos (ACTIVE, LOCKED\_FOR\_TRANSFER, SUSPENDED).

### Mecanismo de Seguridad: Bloqueo Pesimista

Para garantizar la Serializabilidad de las transacciones (depósitos/retiros simultáneos), se utiliza:

1. **Java NIO** FileChannel.lock(): Antes de cualquier edición, el microservicio adquiere un bloqueo exclusivo sobre el archivo de la cuenta.
2. **Gestión de Colas**: Si una cuenta está bloqueada, las transacciones entrantes esperan en una cola con un *timeout* predefinido para evitar bloqueos infinitos.

## Ledger Inmutable (Libro Diario)

A diferencia del estado de cuenta, el directorio /journal/ es estrictamente de "solo adición" (Append-only) y constituye la Fuente de la Verdad (Source of Truth).

| **Característica** | **Implementación**                                                     |
| ------------------ | ---------------------------------------------------------------------- |
| Identificación     | transactionId único generado por el Orquestador.                       |
| Integridad         | Encadenamiento de bloques mediante Hash SHA-256 del registro anterior. |
| Operaciones        | Tipos definidos: DEBIT, CREDIT, REVERSAL.                              |
| Validación         | checksum (firma digital) para detectar manipulaciones manuales.        |

**Tab. 5** Propiedades de los Registros de Transacción

Ante una corrupción del archivo de saldos (/ledger/), el sistema puede reconstruir el saldo actual procesando linealmente el historial contenido en el journal.

## Estrategias de Resiliencia y Recuperación

Para mitigar riesgos de pérdida de datos por fallos críticos, se han definido tres capas de protección:

| **Estrategia**        | **Método**                | **Propósito**                                                                                                                                  |
| --------------------- | ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| **Escritura Atómica** | *Shadow Paging*           | Se escribe en un archivo temporal (.tmp) y se renombra solo tras confirmar el *flush* a disco. Evita archivos truncados por cortes de energía. |
| **Snapshots**         | *Hot Backup*              | Copia de seguridad horaria comprimida (.tar.gz) de los directorios /ledger/ e /identity/ en volúmenes externos.                                |
| **Sincronización**    | Reconciliación Post-Fallo | Al reiniciar, el nodo valida su journal contra los eventos del Saga Orchestrator para aplicar transacciones perdidas durante el *downtime*.    |

**Tab. 6** Capas de protección

# Diseño de Endpoints

Esta sección detalla los contratos de comunicación para el sistema de banca distribuida. Se divide en endpoints de orquestación (públicos/gateway) y endpoints de nodo (privados/internos).

## Endpoints del Coordinador (Saga Orchestrator)

Estos servicios son el punto de entrada principal para clientes externos y el Frontend. Gestionan procesos de larga duración que involucran múltiples nodos.

### A. Inicio de Transferencia Interbancaria

**Ruta:** POST /api/v1/orchestrator/transfers
**Descripción:** Inicia el flujo de trabajo Saga para mover fondos entre dos instituciones. El orquestador actúa como el guardián del estado global de la transacción.

| **Campo**            | **Ubicación** | **Tipo** | **Obligatorio** | **Descripción**                                 |
| -------------------- | ------------- | -------- | --------------- | ----------------------------------------------- |
| originAccountId      | Body          | String   | Sí              | Identificador de cuenta origen (Formato IBAN).  |
| destinationAccountId | Body          | String   | Sí              | Identificador de cuenta destino (Formato IBAN). |
| originBankId         | Body          | String   | Sí              | ID del nodo bancario emisor.                    |
| destinationBankId    | Body          | String   | Sí              | ID del nodo bancario receptor.                  |
| amount               | Body          | Decimal  | Sí              | Valor numérico de la operación (ej. 100.50).    |
| currency             | Body          | String   | Sí              | Código ISO 4217 (ej. "USD", "EUR").             |

**Tab. 7** Campos de transferencia bancaria

**Ejemplo de Petición (Request):**

{

"originAccountId": "ACC-12345",

"destinationAccountId": "ACC-67890",

"originBankId": "BANK\_A",

"destinationBankId": "BANK\_B",

"amount": 500.00,

"currency": "USD"

}

**Alg. 1** JSON de petición

**Códigos de Respuesta:**

| **HTTP Code**           | **Significado**                   | **Estructura de Respuesta**                      |
| ----------------------- | --------------------------------- | ------------------------------------------------ |
| 202 Accepted            | Saga iniciada exitosamente.       | {"transactionId": "TX-998", "status": "PENDING"} |
| 400 Bad Request         | Datos inválidos o monto negativo. | {"error": "INVALID\_FORMAT", "message": "..."}   |
| 503 Service Unavailable | Nodo de destino inalcanzable.     | {"error": "NODE\_OFFLINE", "node": "BANK\_B"}    |

**Tab. 8** Códigos de respuesta de transferencia bancaria

## Endpoints de Servicios Bancarios

Operaciones atómicas ejecutadas localmente por cada nodo sobre su sistema de archivos.

### A. Consulta de Posición Consolidada Local

**Ruta:** GET /api/v1/bank/customers/{customerId}/accounts
**Descripción:** Obtiene el estado financiero de un cliente limitado a la jurisdicción del banco consultado.

| **Campo**  | **Ubicación** | **Tipo**      | **Obligatorio** | **Descripción**                     |
| ---------- | ------------- | ------------- | --------------- | ----------------------------------- |
| customerId | Path          | String (UUID) | Sí              | Identificador federado del cliente. |

**Fig. 9** Campos de consulta de posición local

**Respuestas:**

| **HTTP Code** | **Descripción**                   | **Ejemplo de Cuerpo**                                        |
| ------------- | --------------------------------- | ------------------------------------------------------------ |
| 200 OK        | Lista de cuentas recuperada.      | [{"account": "ACC-1", "balance": 150.0, "status": "ACTIVE"}] |
| 404 Not Found | Cliente no registrado en el nodo. | {"error": "CUSTOMER\_NOT\_FOUND"}                            |

**Fig. 10** Respuestas de consulta de posición local

## Protocolo de Coordinación Distribuida (Saga Steps)

Endpoints de uso interno, consumidos exclusivamente por el Saga Orchestrator para garantizar la atomicidad (ACID) en el sistema distribuido.

### A. Fase 1: Reserva de Fondos (Prepare/Lock)

**Ruta:** POST /api/v1/bank/accounts/{accountId}/reserve
**Descripción:** Solicita un bloqueo preventivo sobre el archivo de la cuenta y el monto especificado.

| **Campo**       | **Tipo** | **Descripción**                      |
| --------------- | -------- | ------------------------------------ |
| orchestrationId | String   | ID de seguimiento de la Saga global. |
| amount          | Decimal  | Monto a congelar en el saldo.        |
| operation       | Enum     | Tipo de reserva: DEBIT\_RESERVATION. |

**Fig. 11** Campos de Reserva de fondos

**Respuestas Técnicas:**

| **HTTP Code** | **Descripción**         | **Ejemplo de Cuerpo**                   |
| ------------- | ----------------------- | --------------------------------------- |
| 200 OK        | Reserva exitosa.        | {"status": "LOCKED", "lockId": "LK-55"} |
| 409 Conflict  | Saldo insuficiente.     | {"error": "INSUFFICIENT\_FUNDS"}        |
| 423 Locked    | Concurrencia detectada. | {"error": "FILE\_ALREADY\_LOCKED"}      |

**Fig. 12** Respuestas de Reserva de fondos

### B. Fase 2: Confirmación (Commit)

**Ruta:** POST /api/v1/bank/accounts/{accountId}/confirm
**Descripción:** Consolida la transacción de forma definitiva, libera el lock de archivo y actualiza el *Journal* inmutable.

| **Campo**       | **Tipo** | **Descripción**               |
| --------------- | -------- | ----------------------------- |
| orchestrationId | String   | ID de seguimiento de la Saga. |
| action          | Enum     | Siempre COMMIT.               |

**Fig. 13** Campos de Confirmación

**Respuesta Exitosa:**

* **200 OK**: {"status": "COMMITTED", "newBalance": 1000.00}.

### C. Fase de Compensación (Rollback)

**Ruta:** POST /api/v1/bank/accounts/{accountId}/rollback
**Descripción:** Revierte los cambios de la fase de reserva y libera los archivos sin afectar el saldo final. Se dispara ante fallos en nodos terciarios.

| **Campo**       | **Tipo** | **Descripción**                                      |
| --------------- | -------- | ---------------------------------------------------- |
| orchestrationId | String   | ID de seguimiento de la Saga.                        |
| reason          | String   | Motivo de la reversión (ej. "TIMEOUT\_DESTINATION"). |

**Fig. 14** Campos de Compensación

**Respuesta Exitosa:**

* **200 OK**: {"status": "ROLLED\_BACK", "message": "Reserva liberada"}.

# Ejecución Hito 1

Link de github: https://github.com/dmolloc1/SD\_Distributed\_Banking\_System/tree/main

Se desplegó los 3 bancos más el controlador :

![](data:image/png;base64...)

Servicios ejecutados:

![](data:image/png;base64...)

Interfaz prueba:

![](data:image/png;base64...)

Fig. muestra el banco B

![](data:image/png;base64...)

Fig. Muestra el banco A

**Ejecución** ruta /health

![](data:image/png;base64...)

# Planificación Hito 2

### 12. Planificación del Hito 2

El Hito 2 se organizará por fases generales de desarrollo, con el propósito de completar la implementación del sistema bancario distribuido. La planificación considera la construcción de operaciones bancarias, el manejo de archivos locales, la coordinación entre bancos, el control de concurrencia, la integración de la interfaz y la preparación de las evidencias finales del proyecto.

| **Fase** | **Enfoque de trabajo**                     | **Actividades a desarrollar**                                                                                                                                                                                                                                      | **Producto esperado**                                                                                              |
| -------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |
| Fase 1   | Definición técnica final                   | Revisar los requerimientos pendientes, definir los endpoints finales, establecer las reglas de operación para depósitos, retiros y transferencias, además de precisar cómo se manejarán errores como saldo insuficiente, cuenta inexistente o banco no disponible. | Contratos de endpoints definidos, reglas de negocio claras y estructura técnica lista para implementación.         |
| Fase 2   | Operaciones bancarias locales              | Implementar en cada banco las operaciones de consulta, depósito y retiro. Cada operación deberá modificar los saldos en los archivos JSON correspondientes y registrar el movimiento en archivos LOG.                                                              | Bancos capaces de realizar operaciones locales sobre sus propios archivos.                                         |
| Fase 3   | Manejo de archivos y concurrencia          | Organizar la persistencia local de cada banco mediante archivos de clientes, cuentas, transacciones y respaldos. Implementar escritura controlada, archivos temporales y bloqueo de archivos para evitar modificaciones simultáneas incorrectas.                   | Sistema de archivos por banco funcionando con JSON, LOG, respaldos, archivos temporales y control de concurrencia. |
| Fase 4   | Transferencias distribuidas                | Implementar transferencias entre cuentas del mismo banco y entre bancos diferentes. El coordinador deberá validar la cuenta origen, cuenta destino, saldo disponible, débito, crédito y confirmación de la operación.                                              | Transferencias locales e interbancarias funcionando desde el servicio coordinador.                                 |
| Fase 5   | Manejo de fallos e integración del usuario | Implementar mecanismos de compensación o rollback cuando una transferencia falle parcialmente. Integrar el frontend en React para consultar cuentas, realizar operaciones y mostrar mensajes de estado, éxito o error.                                             | Sistema integrado con interfaz web funcional y manejo básico de fallos distribuidos.                               |
| Fase 6   | Pruebas, despliegue y cierre               | Probar consultas, depósitos, retiros, transferencias, fondos insuficientes, caída de un banco y concurrencia. Preparar Docker Compose, capturas, informe final, presentación, artículo y video demostrativo.                                                       | Sistema probado, desplegado localmente, documentado y listo para sustentación.                                     |

### Distribución general de responsabilidades

| **Integrante** | **Responsabilidad principal**                                                      | **Resultado esperado**                               |
| -------------- | ---------------------------------------------------------------------------------- | ---------------------------------------------------- |
| Integrante 1   | Integración general, organización del repositorio y despliegue con Docker Compose. | Sistema integrado y ejecutable localmente.           |
| Integrante 2   | Implementación de depósitos, retiros y validaciones de saldo.                      | Operaciones bancarias locales funcionando.           |
| Integrante 3   | Manejo de archivos, LOG, respaldos, archivos temporales y FileLock.                | Persistencia local segura y control de concurrencia. |
| Integrante 4   | Transferencias locales, transferencias interbancarias, coordinación y rollback.    | Transacciones distribuidas funcionando.              |
| Integrante 5   | Frontend React, pruebas, capturas y apoyo en documentación final.                  | Interfaz funcional y evidencias para el informe.     |

# Conclusiones

En este primer avance del Hito 1, se ha completado exitosamente la fase de análisis y diseño del sistema bancario distribuido, estableciendo una base sólida que guiará las siguientes etapas de implementación. Se ha definido una arquitectura de microservicios con orquestación mediante el patrón Saga, seleccionando tecnológicamente Spring Boot para el backend, React para el frontend, archivos JSON para la persistencia local y Docker para el despliegue aislado de los componentes. Asimismo, se han especificado los requerimientos funcionales y no funcionales, así como los protocolos de comunicación (REST/gRPC) y los mecanismos de control de concurrencia (bloqueo pesimista) que asegurará la consistencia y confiabilidad del sistema. Este análisis preliminar sienta las bases conceptuales y técnicas necesarias para proceder con la implementación en los siguientes hitos del proyecto.

# Referencias bibliográficas

Bernstein, P. A., & Goodman, N. (1981). Concurrency control in distributed database systems. ACM Computing Surveys, 13(2), 185-221. https://doi.org/10.1145/356842.356846

Burns, B., Grant, B., Oppenheimer, D., Brewer, E., & Wilkes, J. (2016). Borg, Omega, and Kubernetes. Communications of the ACM, 59(5), 44-54. https://doi.org/10.1145/2890784

Ghemawat, S., Gobioff, H., & Leung, S. T. (2003). The Google file system. Proceedings of the 19th ACM Symposium on Operating Systems Principles (SOSP 2003), 29-43. https://doi.org/10.1145/945445.945450

Harding, R., Van Aken, D., Pavlo, A., & Stonebraker, M. (2017). An evaluation of distributed concurrency control. Proceedings of the VLDB Endowment, 10(5), 553-564. https://doi.org/10.14778/3055540.3055548

Ongaro, D., & Ousterhout, J. (2014). In search of an understandable consensus algorithm. Proceedings of the USENIX Annual Technical Conference, 305-320. https://www.usenix.org/conference/atc14/technical-sessions/presentation/ongaro

Yousefpour, A., Fung, C., Nguyen, T., Kadiyala, K., Jalali, F., Niakanlahiji, A., & Jue, J. P. (2019). All one needs to know about fog computing and related edge computing paradigms: A complete survey. Journal of Systems Architecture, 98, 289-330. https://doi.org/10.1016/j.sysarc.2019.02.009