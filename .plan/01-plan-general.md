
## 1. Información del proyecto

| Campo | Valor |
|---|---|
| Stack | Java 17 + Spring Boot 3.5.13 |
| Build tool | Maven (wrapper `./mvnw`) |
| Base de datos | H2 en memoria (perfil dev) |
| Docs API | SpringDoc OpenAPI 2.8.9 + Swagger UI |
| Tests | JUnit 5 + Mockito + MockMvc |
| Contenedor | Dockerfile multi-stage + `docker-compose.yml` |
| Colección API | `docs/todo-api.postman_collection.json` |
| Seed de datos | `docs/seed.sh` |

**Paquete raíz:** `com.imaginemos.todoapi`
**Context path:** `/api`
**Puerto:** `8080`

---

## 2. Arquitectura

Arquitectura en capas:

```
com.imaginemos.todoapi
├── config/           # OpenAPI, WebMvc, Interceptors, CORS
├── controller/       # REST endpoints (TaskController)
├── dto/
│   ├── request/      # CreateTaskRequest, UpdateTaskRequest, ...
│   └── response/     # TaskResponse, PagedResponse<T>, ErrorResponse
├── entity/           # Task, TaskItem, TaskStatus
├── exception/        # Custom exceptions + GlobalExceptionHandler
├── mapper/           # Entity <-> DTO
├── repository/       # Spring Data JPA
└── service/
    └── impl/         # Lógica de negocio (transiciones, búsqueda, etc.)
```

---

## 3. Historias de usuario cubiertas

| # | Historia | Endpoint clave | Estado |
|---|---|---|---|
| 1 | Crear tarea | `POST /v1/tasks` | |
| 2 | Editar tarea (título, descripción, fecha, estado) | `PUT /v1/tasks/{id}` + `PATCH /v1/tasks/{id}/status` | |
| 3 | Eliminar tarea | `DELETE /v1/tasks/{id}` | |
| 4 | Consultar tarea por ID | `GET /v1/tasks/{id}` | |
| 5 | Listar tareas paginadas | `GET /v1/tasks` | |
| 6 | Gestionar ítems checkeables (CRUD) | `POST/PUT/PATCH/DELETE /v1/tasks/{taskId}/items/...` | |
| 7 | Buscar por título / descripción / ítem | `GET /v1/tasks/search?query=...` | |
| 8 | Filtrar por estado (listar pendientes, completadas, etc.) | `GET /v1/tasks?status=X` | |
| 9 | **Alertar tareas programadas cuya fecha ya llegó** | `GET /v1/tasks/overdue` + headers `X-Overdue-Count` / `X-Overdue-Alert` + flag `isOverdue` en el response | |
| 10 | Gestionar transiciones de estado | `PATCH /v1/tasks/{id}/status` | |

---

## 4. Fases del plan

### Fase 0 — Preparación
- [x] Crear repositorio Git
- [x] Crear estructura `backend/` + `docs/` + `.plan/`
- [x] Inicializar `.plan/` con plan, prompts y decisiones

### Fase 1 — Configuración del proyecto
- [x] Generar proyecto con Spring Initializr (Web, JPA, H2, Validation, Lombok, DevTools)
- [x] Configurar `application.yml` (H2 + JPA + logging + context path `/api`)
- [x] Ajustar `pom.xml` con `springdoc-openapi-starter-webmvc-ui 2.8.9`
- [x] Agregar cap de paginación (`spring.data.web.pageable.max-page-size: 100`)

### Fase 2 — Modelo de dominio
- [x] Enum `TaskStatus` (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)
- [x] Entidad `Task` (id, title, description, executionDate, status, timestamps, items)
- [x] Entidad `TaskItem` (id, description, completed, task)
- [x] Relación bidireccional `@OneToMany` con `CascadeType.ALL` + `orphanRemoval`
- [x] Métodos helper `addItem` / `removeItem` para mantener consistencia
- [x] Timestamps automáticos vía `@PrePersist` / `@PreUpdate`

### Fase 3 — DTOs y mappers
- [x] `CreateTaskRequest`, `UpdateTaskRequest`, `UpdateTaskStatusRequest`
- [x] `CreateTaskItemRequest`, `UpdateTaskItemRequest`
- [x] `TaskResponse` (record) con campos computados: `isOverdue`, `itemsCompletedCount`, `itemsTotalCount`
- [x] `TaskItemResponse` (record)
- [x] `PagedResponse<T>` genérico con metadata (pageNumber, pageSize, totalElements, totalPages, isFirst, isLast)
- [x] `ErrorResponse` para respuestas de error consistentes
- [x] `TaskMapper` + `TaskItemMapper`

### Fase 4 — Capa de servicio
- [x] Interface `TaskService` y `TaskServiceImpl` con `@Transactional`
- [x] CRUD de tareas
- [x] Búsqueda: título, descripción de tarea **y descripción de ítems** (JPQL con `LEFT JOIN t.items`)
- [x] Búsqueda combinada con filtro opcional de estado
- [x] Filtro por estado en listado (`getTasksByStatus`)
- [x] Reglas de transición de estado:
  ```
  SCHEDULED  → IN_PROGRESS
  IN_PROGRESS → COMPLETED | CANCELLED
  COMPLETED / CANCELLED → terminales (no transicionan)
  ```
- [x] Detección de tareas overdue (`status = SCHEDULED && executionDate < now`)
- [x] `countOverdue()` para el interceptor de alerta

### Fase 5 — Controladores
- [x] `TaskController` bajo `/v1/tasks`
- [x] CRUD completo
- [x] Listado paginado con filtro `status`
- [x] Búsqueda paginada con filtros `query` + `status`
- [x] `/pending` y `/overdue` **paginados** (returnan `PagedResponse`)
- [x] CRUD de ítems (`POST`, `PUT`, `PATCH`, `DELETE` bajo `/tasks/{taskId}/items/...`)
- [x] Status codes correctos (201 Created, 204 No Content, 400, 404, etc.)
- [x] Anotación `@Valid` en todos los `@RequestBody`

### Fase 6 — Manejo de errores
- [x] Excepciones custom: `ResourceNotFoundException`, `InvalidStateTransitionException`, `ValidationException`
- [x] `GlobalExceptionHandler` con `@RestControllerAdvice`
- [x] `ErrorResponse` JSON consistente (timestamp, status, error, message, path, errors[])
- [x] Manejo de `MethodArgumentNotValidException` (validaciones `@Valid`)
- [x] Manejo de `HttpMessageNotReadableException` (JSON malformado)
- [x] Manejo de `MethodArgumentTypeMismatchException` (param con tipo inválido, ej. `status=FOO`)

### Fase 7 — Historia 9: Alerta de tareas programadas vencidas
- [x] Flag computado `isOverdue` en cada `TaskResponse`
- [x] Endpoint dedicado `GET /v1/tasks/overdue` (paginado)
- [x] `OverdueAlertInterceptor` — inyecta en **todos los GET** bajo `/v1/tasks/**`:
  - `X-Overdue-Count: N`
  - `X-Overdue-Alert: "Tienes N tarea(s) programada(s) cuya fecha ya vencio..."` (solo si N > 0)
- [x] CORS configurado para exponer estos headers al frontend (`exposedHeaders`)

### Fase 8 — Paginación transversal
- [x] Todos los endpoints que retornan colecciones devuelven `PagedResponse<T>`:
  - `GET /v1/tasks`
  - `GET /v1/tasks?status=X`
  - `GET /v1/tasks/search`
  - `GET /v1/tasks/pending`
  - `GET /v1/tasks/overdue`
- [x] Soporte universal de `page`, `size`, `sort` vía `@PageableDefault` + `Pageable`
- [x] Tope de seguridad `max-page-size: 100` en `application.yml`

### Fase 9 — Testing
- [x] `TaskServiceImplTest` (unit tests con Mockito):
  - createTask, updateTask, deleteTask
  - getTaskById (OK + NotFound)
  - getAllTasks paginado
  - updateTaskStatus: happy path, transición inválida, noop (mismo estado)
  - getPendingTasks (paginado)
  - getOverdueTasks (paginado)
  - updateTaskItem, deleteTaskItem
- [x] `TaskControllerIntegrationTest` (MockMvc + Spring context completo):
  - POST / GET / PUT / DELETE end-to-end
  - Validaciones 400
  - 404 en recursos inexistentes
  - Transiciones de estado
  - Endpoints `/pending` y `/overdue` con `PagedResponse`
  - Flujo completo de items

### Fase 10 — Documentación y entregables
- [x] `README.md` en `backend/` con instrucciones de instalación/ejecución
- [x] Swagger UI en `/api/swagger-ui.html` con `@Operation`, `@Tag` en controlador y descripciones
- [x] Colección Postman `docs/todo-api.postman_collection.json` con **end-to-end validation scripts** (status + body + test de reflejo en consulta/listado)
- [x] Script de seed `docs/seed.sh` que crea 11 tareas y deja 4 estados representados + 1 overdue real
- [x] `.plan/` con los 3 archivos (plan, prompts, decisiones)

### Fase 11 — Docker
- [x] `Dockerfile` multi-stage (build con maven → runtime con JRE)
- [x] `.dockerignore`
- [x] `docker-compose.yml` en la raíz para arrancar el backend con `docker compose up`

---

## 5. Endpoints finales

### Tasks

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/v1/tasks` | Crear tarea (SCHEDULED por defecto) |
| GET | `/v1/tasks` | Listar paginado. Filtros: `status`, `page`, `size`, `sort` |
| GET | `/v1/tasks/{id}` | Consultar tarea por ID |
| PUT | `/v1/tasks/{id}` | Actualizar title / description / executionDate |
| DELETE | `/v1/tasks/{id}` | Eliminar (cascade en items) |
| PATCH | `/v1/tasks/{id}/status` | Cambiar estado (valida transiciones) |
| GET | `/v1/tasks/search` | Buscar por texto (title / description / item description) + `status` opcional |
| GET | `/v1/tasks/pending` | SCHEDULED + IN_PROGRESS (paginado) |
| GET | `/v1/tasks/overdue` | SCHEDULED con `executionDate < now` (paginado) |

### Items

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/v1/tasks/{taskId}/items` | Agregar item |
| PUT | `/v1/tasks/{taskId}/items/{itemId}` | Editar descripción y/o flag completed |
| PATCH | `/v1/tasks/{taskId}/items/{itemId}?completed=true` | Togglear completado |
| DELETE | `/v1/tasks/{taskId}/items/{itemId}` | Eliminar item |

### Alerta (historia 9)

Todos los `GET` bajo `/v1/tasks/**` devuelven estos headers:
- `X-Overdue-Count: <N>`
- `X-Overdue-Alert: <mensaje>` (solo si N > 0)

---

## 6. Criterios de aceptación globales

### Funcional
- [x] Todas las historias 1–10 cubiertas por endpoints + tests
- [x] Transiciones de estado validadas (no se puede ir de COMPLETED → IN_PROGRESS)
- [x] Alerta de tareas vencidas visible en consulta, listado y headers HTTP
- [x] Búsqueda encuentra por título, descripción y **descripción de items** (JOIN con `LEFT JOIN t.items`)
- [x] Paginación universal — ningún GET devuelve lista sin paginar
- [x] Validaciones `@NotBlank`, `@Size` en request DTOs

### No funcional
- [x] Códigos HTTP correctos (201, 204, 400, 404)
- [x] Formato de error JSON consistente
- [x] Idioma uniforme del código: **inglés** (enum values, campos, logs, mensajes)
- [x] Swagger UI funcional en `/api/swagger-ui.html`
- [x] CORS configurado (headers custom expuestos)
- [x] Logs informativos (INFO/DEBUG) en operaciones clave

### Calidad
- [x] Tests unitarios + integración verdes
- [x] Sin warnings de compilación
- [x] Proyecto arranca en frío con `./mvnw spring-boot:run`
- [x] `docker compose up` levanta todo
- [x] Seed reproducible para evaluador (`bash docs/seed.sh`)

---

## 7. Cómo ejecutar 

```bash
# 1. Arrancar backend
cd backend
./mvnw spring-boot:run

# 2. Cargar datos de prueba (11 tareas en distintos estados)
bash ../docs/seed.sh

# 3. Probar
# - Swagger UI:   http://localhost:8080/api/swagger-ui.html
# - H2 Console:   http://localhost:8080/api/h2-console  (JDBC URL: jdbc:h2:mem:tododb, user: sa)
# - Postman:      importar docs/todo-api.postman_collection.json y usar el environment baseUrl=http://localhost:8080/api

# O en Docker
docker compose up --build
```

---

## 8. Uso de IA en el proyecto

- IA (Claude) se usó como **pair programmer asíncrono** — nunca como autogenerador ciego.
- Cada decisión arquitectónica relevante se consultó, validó y adaptó al contexto.
- Toda la interacción queda registrada en [`02-prompts.md`](./02-prompts.md) y las decisiones razonadas en [`03-decisiones.md`](./03-decisiones.md).
- El código final fue revisado, compilado y probado manualmente antes de cada commit.
