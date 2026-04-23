# Registro de Decisiones Técnicas

> Log de decisiones arquitectónicas y de diseño tomadas durante el desarrollo de la TODO API.
> Cada decisión incluye contexto, alternativas evaluadas, opción elegida, justificación y rol de la IA en el razonamiento.

---

## Índice

1. [Base de datos en memoria para desarrollo (H2)](#1-base-de-datos-en-memoria-para-desarrollo-h2)
2. [Arquitectura en capas clásica](#2-arquitectura-en-capas-clásica)
3. [DTOs separados para request / response + `records`](#3-dtos-separados-para-request--response--records)
4. [Relación `Task ↔ TaskItem` bidireccional con `CascadeType.ALL` + `orphanRemoval`](#4-relación-task--taskitem-bidireccional-con-cascadetypeall--orphanremoval)
5. [Máquina de estados sin librería: mapa inmutable en el servicio](#5-máquina-de-estados-sin-librería-mapa-inmutable-en-el-servicio)
6. [Validación dual: `@Valid` en DTOs + reglas de negocio en servicio](#6-validación-dual-valid-en-dtos--reglas-de-negocio-en-servicio)
7. [`@ControllerAdvice` global con `ErrorResponse` consistente](#7-controlleradvice-global-con-errorresponse-consistente)
8. [Búsqueda con JPQL `LEFT JOIN` sobre ítems](#8-búsqueda-con-jpql-left-join-sobre-ítems)
9. [Paginación universal + tope `max-page-size`](#9-paginación-universal--tope-max-page-size)
10. [Alerta de tareas overdue: headers HTTP + flag + endpoint](#10-alerta-de-tareas-overdue-headers-http--flag--endpoint)
11. [Idioma único del código: inglés](#11-idioma-único-del-código-inglés)
12. [Upgrade de springdoc-openapi por incompatibilidad con Spring Boot 3.5](#12-upgrade-de-springdoc-openapi-por-incompatibilidad-con-spring-boot-35)
13. [Seed reproducible para el evaluador](#13-seed-reproducible-para-el-evaluador)
14. [Docker opcional pero incluido](#14-docker-opcional-pero-incluido)

---

## 1. Base de datos en memoria para desarrollo (H2)

**Contexto:** el enunciado no obliga a una BD específica; solo persistencia.

**Alternativas:**
- H2 en memoria → setup cero
- PostgreSQL local → requiere instalación
- MySQL local → requiere instalación

**Decisión:** H2 en memoria (`jdbc:h2:mem:tododb`) con consola web habilitada en `/api/h2-console`.

**Justificación:**
- El evaluador clona y corre con `./mvnw spring-boot:run` sin instalar nada.
- La console H2 ayuda a inspeccionar datos durante la revisión.
- Spring Boot permite migrar a Postgres cambiando solo `application.yml` + dependencia, sin tocar código.

**Rol de IA:** recomendación estándar (Sí → seguida).

**Consecuencias:** los datos se pierden al reiniciar; por eso se entrega `docs/seed.sh` para repoblar rápidamente (ver decisión 13).

**Archivos:** `application.yml`, `pom.xml`.

---

## 2. Arquitectura en capas clásica

**Contexto:** definir el estilo arquitectónico.

**Alternativas:**
- Capas tradicional (Controller → Service → Repository → Entity) con DTOs y Mappers.
- Clean/Hexagonal Architecture.
- Controller con lógica directa.

**Decisión:** capas tradicional.

**Justificación:**
- Alcance pequeño/mediano — hexagonal sería sobre-ingeniería.
- Es lo que espera ver un evaluador técnico en Spring Boot.
- Cada capa se testea independientemente (ver decisión 6 y suite de tests).

**Estructura final:**
```
com.imaginemos.todoapi/
├── config/           # OpenAPI, WebMvc, Interceptor
├── controller/       # TaskController
├── dto/
│   ├── request/
│   └── response/
├── entity/
├── exception/
├── mapper/
├── repository/
└── service/
    └── impl/
```

**Rol de IA:** recomendación (Sí → seguida).

**Archivos:** estructura completa del proyecto.

---

## 3. DTOs separados para request / response + `records`

**Contexto:** contratos de API vs. modelo de dominio.

**Alternativas:**
- DTOs separados Request + Response → best practice.
- Exponer entidades directamente → anti-patrón (lazy loading, acoplamiento a BD).
- Un DTO único → mezcla responsabilidades.

**Decisión:** DTOs separados. `TaskResponse` y `PagedResponse<T>` son `records` inmutables.

**Justificación:**
- Responses inmutables reducen bugs por mutación accidental.
- Validaciones específicas por operación (crear exige título; editar puede venir parcial).
- Campos computados (`isOverdue`, `itemsCompletedCount`, `itemsTotalCount`) se calculan en el mapper, no se persisten.
- Evolución independiente del esquema de BD.

**Rol de IA:** recomendación fuerte (Sí → seguida).

**Archivos:** `dto/request/*.java`, `dto/response/*.java`, `mapper/*.java`.

---

## 4. Relación `Task ↔ TaskItem` bidireccional con `CascadeType.ALL` + `orphanRemoval`

**Contexto:** los ítems no existen sin su tarea contenedora (composición, no agregación).

**Alternativas:**
- Bidireccional con cascade + orphanRemoval.
- Unidireccional (Task → Items).
- Sin relación JPA (queries manuales).

**Decisión:** bidireccional con `CascadeType.ALL` + `orphanRemoval = true`, inicialización `@Builder.Default private List<TaskItem> items = new ArrayList<>()`.

**Implementación clave:**
```java
public void addItem(TaskItem item) {
    items.add(item);
    item.setTask(this);
}
public void removeItem(TaskItem item) {
    items.remove(item);
    item.setTask(null);
}
```

**Justificación:**
- Semántica correcta: borrar tarea borra ítems; quitar un ítem del `List` lo elimina.
- Métodos helper garantizan consistencia en ambas direcciones.
- `FetchType.LAZY` por default evita carga innecesaria.

**Trampas mitigadas:**
- `@ToString(exclude = "items")` para evitar recursión infinita.
- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` sobre `id` para evitar N+1 en equality.

**Rol de IA:** sugerencia estándar (Sí → seguida).

**Archivos:** `entity/Task.java`, `entity/TaskItem.java`.

---

## 5. Máquina de estados sin librería: mapa inmutable en el servicio

**Contexto:** 4 estados con reglas simples.

**Alternativas:**
- Mapa `Map<TaskStatus, Set<TaskStatus>>` en el servicio.
- Spring State Machine (dependencia extra).
- Enum con método `canTransitionTo(TaskStatus)`.

**Decisión:** mapa inmutable en `TaskServiceImpl`.

**Implementación:**
```java
private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
    TaskStatus.SCHEDULED,   Set.of(TaskStatus.IN_PROGRESS),
    TaskStatus.IN_PROGRESS, Set.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED),
    TaskStatus.COMPLETED,   Set.of(),
    TaskStatus.CANCELLED,   Set.of()
);
```

**Reglas:**
- `SCHEDULED → IN_PROGRESS`
- `IN_PROGRESS → COMPLETED | CANCELLED`
- Transición a mismo estado: no-op (no es error).
- Terminal states: cualquier cambio lanza `InvalidStateTransitionException`.

**Justificación:** para 4 estados la librería sería overkill. Mapa es legible, fácil de testear y mantiene toda la lógica en un lugar.

**Rol de IA:** recomendación directa (Sí → seguida).

**Archivos:** `service/impl/TaskServiceImpl.java`, `exception/InvalidStateTransitionException.java`.

---

## 6. Validación dual: `@Valid` en DTOs + reglas de negocio en servicio

**Contexto:** dónde validar.

**Decisión:**
- **Formato / tipos / tamaño / nulabilidad** → anotaciones Bean Validation en DTOs.
- **Reglas de negocio** (transiciones, existencia, unicidad) → servicio con excepciones específicas.

**Ejemplo request:**
```java
@NotBlank(message = "Title is required")
@Size(max = 255, message = "Title must be at most 255 characters")
private String title;
```

**Ejemplo servicio:**
```java
if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
    throw new InvalidStateTransitionException(current, newStatus);
}
```

**Justificación:** separación de preocupaciones. El frontend recibe `400` con `errors[]` listando campos inválidos, pero también `400` con `message` claro cuando se rompe una regla de negocio.

**Rol de IA:** (Sí → seguida).

**Archivos:** `dto/request/*`, `service/impl/TaskServiceImpl.java`.

---

## 7. `@ControllerAdvice` global con `ErrorResponse` consistente

**Contexto:** respuestas de error homogéneas.

**Decisión:** `GlobalExceptionHandler` con `@RestControllerAdvice`, captura:

| Excepción | HTTP | Origen |
|---|---|---|
| `ResourceNotFoundException` | 404 | Custom — tarea o ítem no existe |
| `InvalidStateTransitionException` | 400 | Custom — transición de estado inválida |
| `ValidationException` | 400 | Custom — reglas de negocio |
| `MethodArgumentNotValidException` | 400 | Spring — `@Valid` falló |
| `HttpMessageNotReadableException` | 400 | Spring — JSON malformado |
| `MethodArgumentTypeMismatchException` | 400 | Spring — `?status=FOO` no parsea a enum |
| `Exception` | 500 | Fallback con log ERROR |

**Estructura `ErrorResponse`:**
```json
{
  "timestamp": "2026-04-23T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Title is required",
  "path": "/api/v1/tasks",
  "errors": [
    { "field": "title", "message": "Title is required" }
  ]
}
```

**Justificación:** contrato estable. Frontend puede distinguir error de campo vs. error de negocio leyendo `errors[]` vs. `message`.

**Rol de IA:** (Sí → seguida).

**Archivos:** `exception/GlobalExceptionHandler.java`, `exception/*Exception.java`, `dto/response/ErrorResponse.java`.

---

## 8. Búsqueda con JPQL `LEFT JOIN` sobre ítems

**Contexto:** historia 7 pide que la búsqueda localice tareas por información relevante. Se decidió ampliar al texto de los ítems (un checklist útil suele describir la tarea).

**Alternativas:**
- Búsqueda solo en `title` y `description` de Task.
- Búsqueda en title + description + descripción de ítems.
- Elasticsearch / full-text search.

**Decisión:** búsqueda JPQL con `LEFT JOIN` + `SELECT DISTINCT` + filtro opcional de estado en la misma query.

**Query final:**
```sql
SELECT DISTINCT t FROM Task t LEFT JOIN t.items i
WHERE (:status IS NULL OR t.status = :status)
  AND (
        LOWER(t.title)       LIKE LOWER(CONCAT('%', :query, '%'))
     OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
     OR LOWER(COALESCE(i.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))
  )
```

**Justificación:**
- Una sola query, evita N+1.
- `DISTINCT` evita duplicar la tarea cuando varios ítems matchean.
- `:status IS NULL OR ...` permite que el mismo método sirva para búsqueda con y sin filtro de estado (API combinada).
- `COALESCE(..., '')` cubre descripciones nulas.

**Alternativa futura:** si el volumen crece, migrar a Hibernate Search / OpenSearch. Por ahora, LIKE es suficiente.

**Rol de IA:** diseño de la query (Sí → seguida, validada manualmente con datos de prueba).

**Archivos:** `repository/TaskRepository.java`.

---

## 9. Paginación universal + tope `max-page-size`

**Contexto:** historia 5 pide listado paginado. Decidimos ir más allá y **paginar todo endpoint de colección**.

**Decisión:**
- **Todos** los endpoints que devuelven listas retornan `PagedResponse<T>`:
  - `GET /v1/tasks`
  - `GET /v1/tasks?status=X`
  - `GET /v1/tasks/search`
  - `GET /v1/tasks/pending`
  - `GET /v1/tasks/overdue`
- Todos aceptan `page`, `size` y `sort` estándar de Spring Data.
- Defaults centralizados: `size=20`, `sort=createdAt`.
- Tope de seguridad en `application.yml`:
  ```yaml
  spring:
    data:
      web:
        pageable:
          default-page-size: 20
          max-page-size: 100
  ```

**Justificación:**
- Uniformidad: un cliente aprende un contrato (PagedResponse) y lo reutiliza.
- Seguridad: evita que `size=100000` tumbe el servidor; Spring lo capa a 100.
- Consistencia en tests e integración con frontends (tablas con paginación uniforme).

**Rol de IA:** propuesta y refactor propagando el cambio a repo, service, controller y tests (Sí → seguida con revisión).

**Archivos afectados:** `TaskRepository.java`, `TaskService.java`, `TaskServiceImpl.java`, `TaskController.java`, tests, `application.yml`.

---

## 10. Alerta de tareas overdue: headers HTTP + flag + endpoint

**Contexto:** historia 9 — "Si una tarea está en Programado y llega su fecha, el sistema debe reflejar que existe una tarea pendiente por ejecutar".

**Alternativas consideradas** (6 opciones evaluadas, ver [Prompt #18](./02-prompts.md#prompt-18--opciones-para-implementar-la-alerta)):
1. Header HTTP `X-Overdue-Count` + `X-Overdue-Alert`.
2. Endpoint resumen `/alerts`.
3. Scheduler + WebSocket/SSE.
4. Email / Slack / Teams.
5. Push notifications móvil.
6. Sistema de notificaciones persistido en BD.

**Decisión:** implementar **1** como mecanismo transversal + mantener **flag `isOverdue` en cada response** + **endpoint dedicado `/overdue` paginado**.

**Implementación (triple refuerzo):**

**a) Flag en el DTO (visible en cualquier consulta o listado):**
```java
private boolean isOverdue(Task task) {
    return task.getStatus() == TaskStatus.SCHEDULED
        && task.getExecutionDate() != null
        && task.getExecutionDate().isBefore(LocalDateTime.now());
}
```

**b) Endpoint específico:**
```
GET /v1/tasks/overdue?page=0&size=10&sort=executionDate,asc
```

**c) Headers HTTP en todos los GET de `/v1/tasks/**`** (via `OverdueAlertInterceptor`):
```
X-Overdue-Count: 3
X-Overdue-Alert: Tienes 3 tarea(s) programada(s) cuya fecha ya vencio y siguen sin iniciar
```
+ CORS con `exposedHeaders` para que un frontend JS los pueda leer.

**Justificación:**
- El enunciado permite al desarrollador definir el formato. Los tres refuerzos dan visibilidad desde múltiples ángulos.
- El flag es **observable en consulta y listado** como pide explícitamente la historia.
- El header es la alerta "activa" — un frontend puede mostrar un badge/toast sin hacer un segundo request.
- Sin dependencias extras (no se usó WebSocket/Firebase/SMTP para no inflar el alcance).

**Rol de IA:** análisis comparativo de 6 alternativas + implementación de la opción elegida (Sí → seguida).

**Archivos:**
- `config/OverdueAlertInterceptor.java`
- `config/WebMvcConfig.java`
- `mapper/TaskMapper.java::isOverdue()`
- `repository/TaskRepository.java::countOverdue()`
- `service/impl/TaskServiceImpl.java::countOverdue()`
- `controller/TaskController.java::overdue()`

---

## 11. Idioma único del código: inglés

**Contexto:** los enums iniciales estaban en español (`PROGRAMADO`, `EN_EJECUCION`...), mezclados con nombres de variables en inglés. Decidimos homogeneizar.

**Decisión:** todo el código fuente, mensajes de log, descripciones Swagger, nombres de métodos y enum values en inglés.

**Mapa del refactor:**
| Antes (ES) | Después (EN) |
|---|---|
| `PROGRAMADO` | `SCHEDULED` |
| `EN_EJECUCION` | `IN_PROGRESS` |
| `FINALIZADA` | `COMPLETED` |
| `CANCELADA` | `CANCELLED` |

**Justificación:**
- Código profesional e idiomático.
- Consistencia con convenciones REST internacionales.
- Evita confusión cuando un modelo es `Task` pero su estado es `PROGRAMADO`.
- Los mensajes de usuario (ej. `X-Overdue-Alert`) sí están en español porque son user-facing.

**Rol de IA:** refactor propagado a enum, service, mapper, tests, Postman y README (Sí → seguida, verificado con `grep`).

---

## 12. Upgrade de springdoc-openapi por incompatibilidad con Spring Boot 3.5

**Contexto:** al cargar Swagger UI, `GET /api/v3/api-docs` respondía HTTP 500 y la UI mostraba "Failed to load API definition".

**Causa raíz:** `springdoc-openapi-starter-webmvc-ui 2.6.0` rompe en Spring Boot ≥ 3.4 por cambios internos en `RequestMappingInfoHandlerMapping`.

**Decisión:** upgrade directo a `springdoc-openapi-starter-webmvc-ui 2.8.9`.

**Justificación:** es la versión oficialmente compatible con Spring Boot 3.5.x.

**Rol de IA:** diagnóstico + recomendación de versión (Sí → seguida).

**Archivos:** `pom.xml`.

---

## 13. Seed reproducible para el evaluador

**Contexto:** H2 en memoria se resetea en cada reinicio. Un evaluador que quiera probar manualmente no puede trabajar con la BD vacía.

**Alternativas:**
- `data.sql` nativo de Spring → aplica siempre al arrancar, menos explícito.
- `CommandLineRunner` → código Java extra.
- Script externo invocando la API REST.

**Decisión:** script externo `docs/seed.sh` con `curl`.

**Qué hace:**
1. POST 11 tareas variadas.
2. PATCH de estados para dejar 4 estados representados: **3 SCHEDULED, 3 IN_PROGRESS, 2 COMPLETED, 2 CANCELLED**.
3. Incluye **1 tarea overdue real** (SCHEDULED con fecha 2026-03-10, ~44 días en el pasado).
4. Imprime al final la distribución por estado.

**Justificación:**
- El script documenta por sí mismo cómo usar el API (es ejemplo de uso real).
- Opcional: puede no correrse y el API sigue funcionando.
- No contamina el código: no hay datos hardcodeados en producción.

**Rol de IA:** generación del script + iteración para cubrir overdue real (Sí → seguida).

**Archivos:** `docs/seed.sh`.

---

## 14. Docker opcional pero incluido

**Contexto:** el enunciado marca Docker como "plus opcional".

**Decisión:** incluido, con build multi-stage.

**Archivos:**
- `backend/Dockerfile` — multi-stage (build Maven → runtime JRE 17).
- `backend/.dockerignore` — excluye target/, .idea, *.md, etc.
- `docker-compose.yml` — arranca el backend publicando 8080.

**Justificación:** agrega puntos sin costo significativo; facilita que el evaluador levante el proyecto con `docker compose up` si no tiene Java 17 local.

**Rol de IA:** scaffold del Dockerfile optimizado (Sí → seguida).

---

## Resumen ejecutivo

| # | Decisión | IA consultada | Seguida |
|---|---|---|---|
| 1 | H2 en memoria | Sí | Total |
| 2 | Arquitectura en capas | Sí | Total |
| 3 | DTOs separados + records | Sí | Total |
| 4 | Relación bidireccional cascade+orphanRemoval | Sí | Total |
| 5 | State machine vía mapa inmutable | Sí | Total |
| 6 | Validación dual | Sí | Total |
| 7 | `@ControllerAdvice` global | Sí | Total |
| 8 | Búsqueda con JOIN sobre ítems | Sí | Total |
| 9 | Paginación universal + max cap | Sí | Total |
| 10 | Alerta overdue triple (flag + endpoint + headers) | Sí | Parcial (se eligió 1 de 6 opciones) |
| 11 | Código en inglés | Sí | Total |
| 12 | Upgrade springdoc 2.8.9 | Sí | Total |
| 13 | Seed script externo | Sí | Total |
| 14 | Docker multi-stage | Sí | Total |

---

## Principios guía del proyecto

1. **Simplicidad apropiada** — no sobre-ingenierizar (State Machine, Hexagonal, Elasticsearch descartados).
2. **Best practices de Spring Boot** — se cumple con lo que un revisor espera ver: `@ControllerAdvice`, DTOs, Pageable, Bean Validation.
3. **Experiencia del evaluador** — seed reproducible, Swagger UI, colección Postman, H2 console, Docker.
4. **Código en inglés, mensajes de usuario en español** — profesionalismo + usabilidad local.
5. **Historia 9 como punto clave** — alerta visible por 3 mecanismos complementarios sin depender de infraestructura externa.
