# Registro de Prompts — Desarrollo asistido por IA

> Bitácora de las interacciones con IA (Claude) durante el desarrollo de la TODO API.
> Cada entrada documenta el contexto, el prompt, lo que se aplicó y los ajustes realizados.

Las fases siguen el plan general ([`01-plan-general.md`](./01-plan-general.md)).

---

## Resumen ejecutivo

| Fase | Prompts | Propósito principal |
|---|---|---|
| 1. Configuración | 2 | Scaffold del proyecto, dependencias, springdoc |
| 2. Modelo de dominio | 3 | Entidades JPA, enum, relación bidireccional |
| 3. DTOs y mappers | 3 | Contratos de request/response, campos computados |
| 4. Servicio | 4 | CRUD, transiciones de estado, búsqueda |
| 5. Controladores | 3 | Endpoints REST, validaciones, paginación |
| 6. Excepciones | 2 | `@ControllerAdvice`, excepciones custom |
| 7. Historia 9 (alerta overdue) | 2 | Flag `isOverdue`, interceptor con headers HTTP |
| 8. Paginación transversal | 2 | Refactor de `/pending` y `/overdue` a `PagedResponse` |
| 9. Testing | 3 | Unit tests + integración con MockMvc |
| 10. Documentación | 2 | Swagger UI, Postman collection, README |
| 11. Docker | 1 | Dockerfile multi-stage + compose |
| **Total** | **27** |  |

---

## Formato

```markdown
### Prompt #N — <título breve>
**Fase:** <fase del plan>
**Contexto:** por qué se consultó a la IA.
**Prompt (resumen):** lo que se pidió.
**Respuesta de IA (resumen):** lo que devolvió.
**Aplicación:** archivos creados/modificados.
**Ajustes manuales:** cambios hechos después de la sugerencia.
```

---

## Fase 1 — Configuración del proyecto

### Prompt #1 — Scaffold Spring Boot 3 + dependencias base
**Fase:** 1 — Configuración
**Contexto:** necesito arrancar el proyecto desde cero con las dependencias correctas y una estructura mantenible.
**Prompt (resumen):** "Necesito proyecto Spring Boot 3.x, Java 17, Maven. Dependencias: Web, JPA, H2, Validation, Lombok, DevTools. Paquete `com.imaginemos.todoapi`. Dame estructura de paquetes en capas (controller, service, repository, entity, dto/request, dto/response, exception, mapper, config) y el `application.yml` para H2 con console habilitada y context path `/api`."
**Respuesta de IA (resumen):** generó la estructura de paquetes, `pom.xml` con Spring Boot 3.5.13 y `application.yml` con H2 en memoria (`jdbc:h2:mem:tododb`), logging DEBUG sobre `com.imaginemos.todoapi`, SQL de Hibernate formateado y console H2 en `/h2-console`.
**Aplicación:**
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- Estructura de paquetes bajo `com.imaginemos.todoapi`
**Ajustes manuales:**
- Se agregaron propiedades de paginación para blindar el API: `spring.data.web.pageable.default-page-size=20`, `max-page-size=100`.
- Se fijó `server.servlet.context-path: /api`.

### Prompt #2 — Fix de Swagger / springdoc incompatible
**Fase:** 1 — Configuración
**Contexto:** al llamar a `/api/v3/api-docs` el servidor devolvía HTTP 500 y Swagger UI no cargaba.
**Prompt (resumen):** "Springdoc 2.6.0 con Spring Boot 3.5.13 devuelve 500 en `/v3/api-docs`. ¿Qué versión de springdoc debo usar para Boot 3.5.x?"
**Respuesta de IA (resumen):** las versiones 2.6.x quedaron incompatibles con Spring Boot ≥ 3.4. Upgrade recomendado a `springdoc-openapi-starter-webmvc-ui 2.8.9`.
**Aplicación:**
- `backend/pom.xml` actualizado a springdoc 2.8.9
**Ajustes manuales:** ninguno; fix puntual de versión.

---

## Fase 2 — Modelo de dominio

### Prompt #3 — Diseño de entidades con relación bidireccional
**Fase:** 2 — Modelo
**Contexto:** necesito dos entidades (`Task`, `TaskItem`) con relación 1:N bidireccional bien modelada.
**Prompt (resumen):** "Dame las dos entidades JPA con: (a) `CascadeType.ALL` + `orphanRemoval=true`, (b) métodos helper `addItem`/`removeItem` para mantener la bidireccionalidad, (c) `@PrePersist`/`@PreUpdate` para timestamps, (d) `@EqualsAndHashCode` sólo por `id`, (e) Lombok pero sin `@Data` para evitar toString recursivo."
**Respuesta de IA (resumen):** entidades con `FetchType.LAZY` en la colección, exclusión de `items` en `@ToString`, `@EqualsAndHashCode(onlyExplicitlyIncluded=true)`. Inicialización de `items = new ArrayList<>()` dentro del `@Builder.Default`.
**Aplicación:**
- `entity/Task.java`
- `entity/TaskItem.java`
**Ajustes manuales:** `@PrePersist` de `Task` fija status por defecto en `SCHEDULED` si viene null.

### Prompt #4 — Enum de estados + transiciones válidas
**Fase:** 2 — Modelo
**Contexto:** definir los 4 estados y qué transiciones son legales.
**Prompt (resumen):** "TaskStatus con 4 valores en inglés (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED). Reglas: SCHEDULED→IN_PROGRESS; IN_PROGRESS→COMPLETED|CANCELLED; COMPLETED/CANCELLED son terminales. ¿Dónde modelo las transiciones?"
**Respuesta de IA (resumen):** para 4 estados no vale la pena Spring State Machine. Mapa inmutable en el servicio (`Map<TaskStatus, Set<TaskStatus>>`) es suficiente, claro y testeable.
**Aplicación:**
- `entity/TaskStatus.java`
- `service/impl/TaskServiceImpl.java` → constante `ALLOWED_TRANSITIONS`

### Prompt #5 — Migración de estados en español a inglés
**Fase:** 2 — Modelo (refactor)
**Contexto:** los enums estaban en español (`PROGRAMADO`, `EN_EJECUCION`...). Se requirió unificar todo el código en inglés.
**Prompt (resumen):** "Reemplazar en todo el proyecto `PROGRAMADO→SCHEDULED`, `EN_EJECUCION→IN_PROGRESS`, `FINALIZADA→COMPLETED`, `CANCELADA→CANCELLED`. Ajustar enum, mapas de transición, mapper, tests, postman y README."
**Respuesta de IA (resumen):** refactor completo en 1 paso usando `sed` para strings en tests/postman/README y `Edit` para código Java.
**Aplicación:** ~10 archivos modificados.
**Ajustes manuales:** verificación con `grep` de que no queda ninguna referencia en español, y compilación `./mvnw test-compile` como check.

---

## Fase 3 — DTOs y mappers

### Prompt #6 — DTOs request/response + `PagedResponse<T>`
**Fase:** 3 — DTOs
**Contexto:** modelar los contratos de API sin exponer entidades.
**Prompt (resumen):** "Dame `CreateTaskRequest` con `@NotBlank`/`@Size`, `UpdateTaskRequest` con campos todos opcionales (patch parcial), `UpdateTaskStatusRequest` con `@NotNull`, y `TaskResponse` como `record` con campos computados (`isOverdue`, `itemsCompletedCount`, `itemsTotalCount`). También `PagedResponse<T>` genérico para que las listas tengan metadata."
**Respuesta de IA (resumen):** DTOs propuestos. `PagedResponse` como record con factory estático `from(Page<E>, Function<E,T>)`.
**Aplicación:**
- `dto/request/*.java`
- `dto/response/TaskResponse.java` (record)
- `dto/response/PagedResponse.java` (record genérico)
- `dto/response/TaskItemResponse.java`
- `dto/response/ErrorResponse.java`

### Prompt #7 — Campo computado `isOverdue`
**Fase:** 3 — DTOs
**Contexto:** la historia 9 pide alerta de tareas programadas vencidas. Decidimos calcular el flag al momento del mapeo.
**Prompt (resumen):** "En `TaskMapper.toResponse(Task)`, calcular `isOverdue = (status == SCHEDULED && executionDate != null && executionDate < now)`. ¿Hay mejor forma?"
**Respuesta de IA (resumen):** sí, calcular dinámicamente en el mapper. Alternativa de columna persistida fue descartada (quedaría desactualizada sin scheduler).
**Aplicación:** `mapper/TaskMapper.java::isOverdue()`.

### Prompt #8 — Mapper que agregue ítems en lote al crear tarea
**Fase:** 3 — DTOs/Mappers
**Contexto:** `CreateTaskRequest.itemDescriptions` es `List<String>` y debe materializarse en `TaskItem` asociados.
**Prompt (resumen):** "Al crear tarea con `itemDescriptions = [\"a\", \"b\"]`, debe quedar con 2 items completed=false. ¿Dónde lo hago, mapper o servicio?"
**Respuesta de IA (resumen):** en el mapper. Usar `TaskItemMapper.fromDescription(String)` como helper.
**Aplicación:** `mapper/TaskMapper.java::toEntity()` + `mapper/TaskItemMapper.java::fromDescription()`.

---

## Fase 4 — Servicio

### Prompt #9 — Implementación de `TaskServiceImpl` con transicionalidad
**Fase:** 4 — Servicio
**Contexto:** centralizar toda la lógica de negocio.
**Prompt (resumen):** "Dame `TaskServiceImpl` con `@Transactional` a nivel clase, `@Transactional(readOnly=true)` en lecturas, inyección por constructor (Lombok), y logs en operaciones de escritura. Implementar CRUD + `updateTaskStatus` con validación del mapa de transiciones."
**Respuesta de IA (resumen):** implementación completa; delegación de búsqueda de ítem al método privado `findItemOrThrow(task, itemId)`.
**Aplicación:** `service/impl/TaskServiceImpl.java`.

### Prompt #10 — Búsqueda ampliada (título + descripción + items)
**Fase:** 4 — Servicio
**Contexto:** historia 7 dice que la búsqueda debe ayudar a localizar tareas por información relevante como título o descripción. Decidimos ampliar al texto de los ítems.
**Prompt (resumen):** "Cambia el método `search` del repositorio para matchear también contra `t.items.description`. Usa `LEFT JOIN` y `SELECT DISTINCT` para no duplicar resultados cuando varios ítems coinciden. Permite filtrar por `status` opcional en la misma query (`:status IS NULL OR t.status = :status`)."
**Respuesta de IA (resumen):** query JPQL con JOIN + DISTINCT. Parámetro nullable se maneja con `IS NULL OR ...`.
**Aplicación:** `repository/TaskRepository.java::search()` con firma `search(query, status, pageable)`.
**Ajustes manuales:** ajuste de firma en service y controller para pasar `status` opcional.

### Prompt #11 — Filtrar listado principal por estado
**Fase:** 4 — Servicio
**Contexto:** historia 8 pide ver solo tareas de un estado específico (ej. solo SCHEDULED).
**Prompt (resumen):** "Agrega `getTasksByStatus(status, pageable)` al service y permite al controller usarlo cuando la query recibe `?status=X`."
**Respuesta de IA (resumen):** delega en `repository.findByStatus()`, ya existente.
**Aplicación:** `TaskService.java`, `TaskServiceImpl.java`, `TaskController.java`.

### Prompt #12 — Paginar `/pending` y `/overdue`
**Fase:** 4 — Servicio
**Contexto:** ambos endpoints devolvían `List<TaskResponse>` plano; pedimos que fueran `PagedResponse` como los demás.
**Prompt (resumen):** "Cambia `findByStatusIn` y `findOverdue` en el repositorio para devolver `Page<Task>` aceptando `Pageable`. Propaga el cambio a service, controller y tests."
**Respuesta de IA (resumen):** cambio directo de firma + actualización de tests a assertions sobre `Page.getContent()`.
**Aplicación:**
- `TaskRepository.java`
- `TaskServiceImpl.java` — `getPendingTasks(Pageable)`, `getOverdueTasks(Pageable)`
- `TaskController.java` — retornan `PagedResponse`
- `TaskControllerIntegrationTest.java` — assertions a `$.content.length()` y `$.totalElements`
- `TaskServiceImplTest.java` — uso de `PageImpl`

---

## Fase 5 — Controladores

### Prompt #13 — `TaskController` completo
**Fase:** 5 — Controladores
**Contexto:** exponer todos los endpoints con validaciones, status codes y documentación Swagger.
**Prompt (resumen):** "Dame `@RestController` bajo `/v1/tasks` con CRUD + listado paginado + search + endpoint de cambio de estado + CRUD de items. Anota cada endpoint con `@Operation` en inglés. Usa `@Valid` en cada `@RequestBody`. `POST /v1/tasks` debe devolver `201 Created` con header `Location`."
**Respuesta de IA (resumen):** implementación con `UriComponentsBuilder` para el header `Location`, `@ResponseStatus(NO_CONTENT)` en DELETE y `@PatchMapping` para el cambio de estado.
**Aplicación:** `controller/TaskController.java`.

### Prompt #14 — Query param `status` con enum y 400 claro cuando es inválido
**Fase:** 5 — Controladores
**Contexto:** `GET /v1/tasks?status=FOO` debe responder 400, no 500.
**Prompt (resumen):** "Si recibo `status=FOO`, Spring lanza `MethodArgumentTypeMismatchException`. Asegúrate de que el `GlobalExceptionHandler` lo convierta a 400 con mensaje claro."
**Respuesta de IA (resumen):** handler específico para `MethodArgumentTypeMismatchException` → 400 con mensaje `"Invalid value for parameter 'status': FOO"`.
**Aplicación:** `exception/GlobalExceptionHandler.java`.

### Prompt #15 — Paginación visible en todos los endpoints
**Fase:** 5 — Controladores
**Contexto:** asegurar que todos los endpoints de listado acepten `page`, `size` y `sort` como query params estándar.
**Prompt (resumen):** "Usa `@PageableDefault(size=20, sort=\"createdAt\")` en cada endpoint que liste. Documenta los params en `@Operation.description`. Garantiza que `max-page-size` está capado en `application.yml`."
**Respuesta de IA (resumen):** config + defaults aplicados.
**Aplicación:** `TaskController.java` + `application.yml`.

---

## Fase 6 — Manejo de excepciones

### Prompt #16 — `@RestControllerAdvice` global
**Fase:** 6 — Excepciones
**Contexto:** estandarizar respuestas de error.
**Prompt (resumen):** "Crear `GlobalExceptionHandler` que maneje: `ResourceNotFoundException→404`, `InvalidStateTransitionException→400`, `ValidationException→400`, `MethodArgumentNotValidException→400` (con lista de FieldError), `HttpMessageNotReadableException→400` (JSON inválido), `MethodArgumentTypeMismatchException→400`, `Exception→500`."
**Respuesta de IA (resumen):** implementación con `ErrorResponse.of(status, error, message, path, fieldErrors?)`.
**Aplicación:**
- `exception/GlobalExceptionHandler.java`
- `exception/ResourceNotFoundException.java`
- `exception/InvalidStateTransitionException.java`
- `exception/ValidationException.java`
- `dto/response/ErrorResponse.java`

### Prompt #17 — Excepción factoría `ResourceNotFoundException.of(resource, id)`
**Fase:** 6 — Excepciones
**Contexto:** evitar concatenaciones repetidas en el servicio.
**Prompt (resumen):** "Factory static `of(String, Long)` que devuelva `ResourceNotFoundException(\"<Task> not found with id: <123>\")`."
**Respuesta de IA (resumen):** método estático sencillo.
**Aplicación:** `exception/ResourceNotFoundException.java`.

---

## Fase 7 — Historia 9: Alerta de tareas programadas vencidas

### Prompt #18 — Opciones para implementar la alerta
**Fase:** 7 — Historia 9
**Contexto:** la historia dice "la forma de mostrar esta alerta puede ser definida por el desarrollador, siempre que sea clara y visible".
**Prompt (resumen):** "Dame opciones de menor a mayor esfuerzo para alertar cuando haya tareas SCHEDULED con fecha vencida. Quiero algo claro y observable en consulta o listado."
**Respuesta de IA (resumen):** 6 opciones:
1. Header HTTP `X-Overdue-Count` (15 min)
2. Endpoint `/alerts` resumen (20 min)
3. Scheduled task con WebSocket/SSE (1–2 h)
4. Email / Slack / Teams (2–3 h)
5. Push notifications móvil (4–8 h)
6. Sistema de notificaciones persistido en BD (3–4 h)

Recomendación: 1 + 2 cumplen la historia sin sobre-ingeniería.
**Aplicación:** se eligió la **opción 1** (headers HTTP) complementando el flag `isOverdue` ya existente y el endpoint `/overdue`.

### Prompt #19 — Interceptor de alerta overdue
**Fase:** 7 — Historia 9
**Contexto:** implementar la opción 1.
**Prompt (resumen):** "Crea un `HandlerInterceptor` que en cada GET bajo `/v1/tasks/**` añada los headers `X-Overdue-Count: N` y (si N>0) `X-Overdue-Alert: \"Tienes N tarea(s) programada(s) cuya fecha ya vencio...\"`. Configura CORS para exponer estos headers."
**Respuesta de IA (resumen):**
- `OverdueAlertInterceptor` que llama a `taskService.countOverdue()` en `preHandle`.
- `WebMvcConfig implements WebMvcConfigurer` que registra el interceptor con `addPathPatterns("/v1/tasks/**", "/v1/tasks")` y expone los headers vía `CorsRegistry.exposedHeaders(...)`.
- Nuevos métodos `countOverdue()` en repo (`@Query COUNT`), service y service interface.
**Aplicación:**
- `config/OverdueAlertInterceptor.java`
- `config/WebMvcConfig.java`
- `repository/TaskRepository.java::countOverdue()`
- `service/TaskService.java` + `TaskServiceImpl.java::countOverdue()`

---

## Fase 8 — Paginación transversal (refuerzo)

### Prompt #20 — Exponer `page`, `size`, `sort` visibles en Postman
**Fase:** 8 — Pagination DX
**Contexto:** aunque los endpoints ya aceptaban paginación por default, los requests de Postman no traían los params visibles, lo que confundía a evaluadores.
**Prompt (resumen):** "Añade `page=0&size=10&sort=createdAt,desc` a TODOS los GET de la colección Postman. Agrega también la `query` de paginación en `/v1/tasks/pending` y `/v1/tasks/overdue`. Muestra los params explícitos en el `query` array de Postman."
**Respuesta de IA (resumen):** ~15 requests de la colección actualizados con params visibles.
**Aplicación:** `docs/todo-api.postman_collection.json` (sección 05 principalmente).

### Prompt #21 — Tope de seguridad en `max-page-size`
**Fase:** 8 — Pagination DX
**Contexto:** riesgo de `size=100000` tumbando el servicio.
**Prompt (resumen):** "Configura Spring Data para que el tamaño máximo aceptado sea 100, y por defecto 20."
**Respuesta de IA (resumen):** propiedades `spring.data.web.pageable.*` en `application.yml`.
**Aplicación:** `application.yml`.

---

## Fase 9 — Testing

### Prompt #22 — Tests unitarios del servicio con Mockito
**Fase:** 9 — Testing
**Contexto:** validar la lógica pura de `TaskServiceImpl`.
**Prompt (resumen):** "Dame tests de `TaskServiceImpl` mockeando `TaskRepository` y usando `TaskMapper` real. Cubre: createTask, updateTask, deleteTask, getById (OK y NotFound), listado paginado con `PageImpl`, updateTaskStatus (happy/invalid/noop), getPendingTasks, getOverdueTasks, updateTaskItem, deleteTaskItem."
**Respuesta de IA (resumen):** suite completa con `@ExtendWith(MockitoExtension.class)`, `@InjectMocks`, `PageImpl`, matchers (`any`, `eq`).
**Aplicación:** `TaskServiceImplTest.java`.

### Prompt #23 — Tests de integración con MockMvc
**Fase:** 9 — Testing
**Contexto:** probar end-to-end (controller + service + repo + BD H2 en memoria).
**Prompt (resumen):** "Test de integración con `@SpringBootTest` + `MockMvc`. Cubre: POST 201, GET 200/404, PUT 200, DELETE 204, búsqueda, transiciones válidas/inválidas, `/pending` y `/overdue` con respuesta paginada (`$.content`, `$.totalElements`), CRUD de items, validaciones 400."
**Respuesta de IA (resumen):** test con `persistTask(status, executionDate)` helper y `ObjectMapper` para serializar bodies.
**Aplicación:** `TaskControllerIntegrationTest.java`.

### Prompt #24 — Actualización de tests tras refactor de paginación
**Fase:** 9 — Testing
**Contexto:** al paginar `/pending` y `/overdue`, los integration tests que esperaban `$.length()` dejaron de pasar.
**Prompt (resumen):** "Ajusta los tests de `/pending` y `/overdue` para que verifiquen `$.content.length()` y `$.totalElements`."
**Respuesta de IA (resumen):** fix aplicado.
**Aplicación:** `TaskControllerIntegrationTest.java`.

---

## Fase 10 — Documentación

### Prompt #25 — Colección Postman con validación end-to-end
**Fase:** 10 — Documentación
**Contexto:** queremos que el evaluador corra la colección y vea tests pasar automáticamente.
**Prompt (resumen):** "Arma una colección Postman con 7 secciones: 01 Happy Path, 02 Edición de campos, 03 Cambios de estado, 04 CRUD de items, 05 Listado/búsqueda/consultas especiales, 06 Casos de error, 07 Limpieza. Cada request debe validar no solo el status sino también que el cambio se refleja en consulta y listado posterior."
**Respuesta de IA (resumen):** ~35 requests con scripts `pm.test(...)` verificando body + persistencia cruzada.
**Aplicación:** `docs/todo-api.postman_collection.json`.

### Prompt #26 — Seed de datos para evaluador
**Fase:** 10 — Documentación
**Contexto:** la BD es en memoria, cada reinicio la deja vacía. El evaluador necesita datos rápidos para probar.
**Prompt (resumen):** "Shell script con curl que cree 11 tareas variadas (1 overdue real con fecha pasada) y deje 4 estados representados (3 SCHEDULED, 3 IN_PROGRESS, 2 COMPLETED, 2 CANCELLED). Que al final imprima la distribución por estado."
**Respuesta de IA (resumen):** `docs/seed.sh` con array de bodies, loop de POST y un segundo loop que ajusta estados via PATCH.
**Aplicación:** `docs/seed.sh`.

---

## Fase 11 — Docker

### Prompt #27 — Dockerfile multi-stage + compose
**Fase:** 11 — Docker
**Contexto:** entregable "plus" según el enunciado.
**Prompt (resumen):** "Dockerfile multi-stage (build con Maven sobre `eclipse-temurin:17-jdk`, runtime con `eclipse-temurin:17-jre`). `.dockerignore` sensato. `docker-compose.yml` en la raíz para levantar solo el backend. Publicar puerto 8080."
**Respuesta de IA (resumen):** archivos generados; el compose levanta el backend en `http://localhost:8080/api`.
**Aplicación:**
- `backend/Dockerfile`
- `backend/.dockerignore`
- `docker-compose.yml`

---

