# Sistema Bancario Distribuido

## Contexto

- **Issue(s) vinculada(s):** Closes #
- **Equipo Asignado:** [ ] Equipo 1 (Persistencia) | [ ] Equipo 2 (Lógica) | [ ] Equipo 3 (Gateway/UI)
- **Componente(s) Afectado(s):** `coordinator-service` | `bank-a` | `bank-b` | `bank-c` | `gateway` | `frontend`

---

## Resumen de Cambios

Proporciona una explicación técnica clara de los cambios introducidos en este PR:

- 

---

## Impacto en la Arquitectura y Persistencia

Responde brevemente a estas verificaciones arquitectónicas:

1. **¿Modifica la estructura o la forma de escribir en los archivos JSON/LOG?**
   - [ ] Sí (Explicar abajo)
   - [ ] No
   *Detalle:* 

2. **¿Introduce bloqueos de archivos o llamadas síncronas entre microservicios?**
   - [ ] Sí
   - [ ] No

---

## Evidencia de Pruebas y Funcionamiento

Adjunta las evidencias obligatorias que demuestren que el código funciona correctamente sin romper la consistencia del sistema:

- [ ] **Captura/Log de ejecución de cURL o Postman.**
- [ ] **Estado del archivo `.json` de cuentas antes y después de la prueba.**
- [ ] **Log generado en `transactions_X.log` (Auditoría).**

*(Pega las imágenes o bloques de código aquí)*

---

## Lista de Verificación (Checklist previa a revisión)

Antes de solicitar la revisión de código, confirma que has completado los siguientes puntos:

- [ ] Mi código sigue las convenciones del proyecto y no introduce advertencias de compilación.
- [ ] He realizado pruebas locales con los contenedores/servicios encendidos.
- [ ] He verificado que los cambios no producen condiciones de carrera en operaciones concurrentes.
- [ ] Si modifiqué un endpoint, actualicé la documentación correspondiente.
