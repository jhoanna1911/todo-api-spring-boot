# TODO API — Spring Boot

API REST para gestión de tareas con ítems checkeables, máquina de estados, búsqueda paginada y alertas de tareas vencidas.

---

## Tabla de contenidos

1. [Qué se construyó](#1-qué-se-construyó)
2. [Cómo ejecutar el proyecto](#2-cómo-ejecutar-el-proyecto)
3. [Cómo probar la solución](#3-cómo-probar-la-solución)
4. [Endpoints y contratos](#4-endpoints-y-contratos)
5. [Historia 9 — Alerta de tareas vencidas](#5-historia-9--alerta-de-tareas-vencidas)
6. [Stack y arquitectura](#6-stack-y-arquitectura)
7. [Testing](#7-testing)
8. [Docker](#8-docker)
9. [Uso de IA en el desarrollo](#9-uso-de-ia-en-el-desarrollo)

---

## 1. Qué se construyó

### Historias de usuario cubiertas

| # | Historia | Implementación | Estado |
|---|---|---|---|
| 1 | Crear tarea | `POST /v1/tasks` (inicia en `SCHEDULED`) | |
| 2 | Editar tarea (título, descripción, fecha, estado) | `PUT /v1/tasks/{id}` + `PATCH /v1/tasks/{id}/status` | |
| 3 | Eliminar tarea | `DELETE /v1/tasks/{id}` (cascade en items) | |
| 4 | Consultar tarea por ID | `GET /v1/tasks/{id}` | |
| 5 | Listar tareas paginadas | `GET /v1/tasks` con `page`, `size`, `sort` | |
| 6 | Gestionar ítems checkeables | CRUD completo bajo `/v1/tasks/{id}/items` | |
| 7 | Buscar por texto | `GET /v1/tasks/search` — matchea en **título, descripción de tarea y descripción de ítems** | |
| 8 | Filtrar por estado | `GET /v1/tasks?status=SCHEDULED` (o IN_PROGRESS / COMPLETED / CANCELLED) | |
| 9 | **Alertar tareas vencidas** | Triple refuerzo: flag `isOverdue`, endpoint `/overdue`, headers `X-Overdue-Count` y `X-Overdue-Alert` en todos los GET | |
| 10 | Transiciones de estado controladas | Máquina de estados en el servicio (ver sección [Endpoints](#4-endpoints-y-contratos)) | |
| 11 | Documentación para el evaluador | Este README + Swagger UI + Postman + seed script + `.plan/` | |

### Características adicionales (plus)

- **Paginación universal** — todos los endpoints que devuelven colecciones retornan `PagedResponse<T>` con metadata (`pageNumber`, `pageSize`, `totalElements`, `totalPages`, `isFirst`, `isLast`).
- **Tope de seguridad** — `max-page-size=100` para evitar que un cliente pida `size=100000`.
- **Manejo de errores consistente** — todos los errores siguen el mismo shape JSON (`ErrorResponse`).
- **Swagger UI** — documentación OpenAPI navegable.
- **Colección Postman** — flujo end-to-end con tests automáticos.
- **Dockerización** — Dockerfile multi-stage + docker-compose.
- **Seed de datos reproducible** — script que crea 11 tareas en los 4 estados, con 1 tarea realmente overdue.

---

## 2. Cómo ejecutar el proyecto

### Opción A — Local con Maven (recomendado para evaluación)

**Requisitos:** JDK 17+ (no hace falta Maven instalado, usamos el wrapper).

```bash
# Desde la raíz del repo
cd backend
./mvnw spring-boot:run
```

En Windows con PowerShell:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

La aplicación queda expuesta en **http://localhost:8080/api**.

**URLs útiles al instante:**

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/api/v3/api-docs |
| H2 Console | http://localhost:8080/api/h2-console |
| API base | http://localhost:8080/api/v1/tasks |

**Para H2 Console:**
- JDBC URL: `jdbc:h2:mem:tododb`
- User: `sa`
- Password: (vacío)

### Opción B — Docker (no requiere Java local)

**Requisitos:** Docker Desktop.

```bash
# Desde la raíz del repo (NO desde backend/)
docker compose up --build
```

La API queda igualmente en `http://localhost:8080/api`.

**Comandos útiles:**
```bash
docker compose up -d --build        # en background
docker compose logs -f backend      # ver logs en vivo
docker compose down                 # parar y limpiar
docker compose ps                   # estado + health
```

**Troubleshooting Docker:**

| Síntoma | Fix |
|---|---|
| `./mvnw: bad interpreter` durante build | CRLF en Windows → `dos2unix backend/mvnw` o re-clonar con `git config --global core.autocrlf input` |
| `port is already allocated` | Cambia `"8080:8080"` a `"9090:8080"` en `docker-compose.yml` |
| Container muere con OOM | Docker Desktop → Settings → Resources → asigna al menos 4GB |

---

## 3. Cómo probar la solución

La base de datos H2 es **en memoria**, se resetea en cada reinicio. Sigue estos pasos en orden para evaluar:

### Paso 1 — Cargar datos de prueba (recomendado)

```bash
# Desde la raíz del repo, con la app corriendo:
bash docs/seed.sh
```

El script:
- Crea 11 tareas variadas
- Las distribuye en los 4 estados: **3 SCHEDULED · 3 IN_PROGRESS · 2 COMPLETED · 2 CANCELLED**
- Deja **1 tarea realmente overdue** (SCHEDULED con fecha vencida, id=11) para probar la historia 9
- Imprime al final la distribución

### Paso 2 — Explorar con Swagger UI

Abre http://localhost:8080/api/swagger-ui.html y prueba cada endpoint desde la UI. Todos tienen descripciones y ejemplos.

### Paso 3 — Ejecutar la colección de Postman (flujo completo)

Importa **[`docs/todo-api.postman_collection.json`](../docs/todo-api.postman_collection.json)** en Postman. Tiene 7 secciones con **tests automáticos** que verifican cada respuesta:

| Sección | Qué cubre |
|---|---|
| 01 — Flujo Happy Path | Crear tarea con ítems |
| 02 — Edición de campos | Editar title / description / executionDate + verificación en GET y listado |
| 03 — Cambios de estado | Transiciones válidas, inválidas, reflejo en consulta y listado, entrada/salida de `/pending` |
| 04 — CRUD de items | Agregar, editar, togglear, eliminar items |
| 05 — Listado, búsqueda y filtros | Paginación, búsqueda por texto (en ítems incluidos), filtro por estado, combinaciones |
| 06 — Casos de error | 400 validación, 404, 400 transición inválida |
| 07 — Limpieza | Delete final |

Corre toda la colección con el **Collection Runner** y verifica que todos los tests pasen en verde.

**Environment:** la colección usa `{{baseUrl}}` = `http://localhost:8080/api`. Si usas Postman sin configurar el environment, los defaults ya funcionan.

### Paso 4 — Probar directamente con curl

```bash
# Crear una tarea
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Preparar release",
        "description": "Checklist de publicacion",
        "executionDate": "2026-05-01T10:00:00",
        "itemDescriptions": ["Correr tests", "Actualizar changelog"]
      }'

# Listar con filtro por estado
curl "http://localhost:8080/api/v1/tasks?status=SCHEDULED&page=0&size=10"

# Buscar (matchea en título, descripción y descripción de items)
curl "http://localhost:8080/api/v1/tasks/search?query=release"

# Cambiar estado
curl -X PATCH http://localhost:8080/api/v1/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "IN_PROGRESS"}'

# Ver headers de alerta (historia 9)
curl -i http://localhost:8080/api/v1/tasks | grep -i "X-Overdue"
```

---

## 4. Endpoints y contratos

Todos bajo el prefijo `/api/v1/tasks`.

### Tareas

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/v1/tasks` | Crear tarea (inicia en `SCHEDULED`) |
| GET | `/v1/tasks` | Listar paginado. Params: `status`, `page`, `size`, `sort` |
| GET | `/v1/tasks/{id}` | Consultar por ID |
| PUT | `/v1/tasks/{id}` | Actualizar (patch parcial: solo campos no nulos) |
| DELETE | `/v1/tasks/{id}` | Eliminar (cascade elimina sus items) |
| PATCH | `/v1/tasks/{id}/status` | Cambiar estado. Body: `{ "newStatus": "IN_PROGRESS" }` |
| GET | `/v1/tasks/search` | Buscar texto en título / descripción / descripción de ítems. Params: `query`, `status`, `page`, `size`, `sort` |
| GET | `/v1/tasks/pending` | Tareas en `SCHEDULED` o `IN_PROGRESS` (paginado) |
| GET | `/v1/tasks/overdue` | Tareas `SCHEDULED` con `executionDate` en el pasado (paginado) |

### Ítems de una tarea

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/v1/tasks/{taskId}/items` | Agregar ítem. Body: `{ "description": "..." }` |
| PUT | `/v1/tasks/{taskId}/items/{itemId}` | Editar ítem (descripción y/o completed) |
| PATCH | `/v1/tasks/{taskId}/items/{itemId}?completed=true` | Toggle rápido |
| DELETE | `/v1/tasks/{taskId}/items/{itemId}` | Eliminar ítem |

### Transiciones de estado permitidas

| De | A |
|---|---|
| `SCHEDULED` | `IN_PROGRESS` |
| `IN_PROGRESS` | `COMPLETED` |
| `IN_PROGRESS` | `CANCELLED` |

- `COMPLETED` y `CANCELLED` son **terminales** (no transicionan).
- Una transición inválida devuelve `400 Bad Request` con mensaje claro.
- Transicionar al mismo estado es un no-op (no es error).

### Paginación estándar

Todos los endpoints de listado aceptan:
- `page` — número de página (desde 0). Default: 0.
- `size` — tamaño de página. Default: 20. Máximo: 100.
- `sort` — campo + dirección (ej: `sort=executionDate,desc`). Default: `createdAt`.

### Formato de errores

```json
{
  "timestamp": "2026-04-23T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/tasks",
  "errors": [
    { "field": "title", "message": "Title is required" }
  ]
}
```

Códigos usados:
- `201 Created` — creación exitosa (con header `Location`)
- `204 No Content` — delete exitoso
- `400 Bad Request` — validación, JSON inválido, transición inválida, parámetro con tipo incorrecto (ej. `status=FOO`)
- `404 Not Found` — tarea o ítem inexistente
- `500 Internal Server Error` — error inesperado (fallback con log)

---

## 5. Historia 9 — Alerta de tareas vencidas

> **"Si una tarea está en Programado y llega su fecha de ejecución, el sistema debe reflejar que existe una tarea pendiente por ejecutar."**

Implementamos la alerta con **triple refuerzo** para que sea imposible pasar por alto:

### a) Flag en cada respuesta — visible en consulta y listado

Cada `TaskResponse` incluye el booleano `isOverdue` calculado al momento:

```json
{
  "id": 11,
  "title": "Auditoria de seguridad",
  "status": "SCHEDULED",
  "executionDate": "2026-03-10T09:00:00",
  "isOverdue": true
}
```

Aparece en: `GET /v1/tasks`, `GET /v1/tasks/{id}`, `GET /v1/tasks?status=X`, `GET /v1/tasks/search`, etc.

### b) Endpoint dedicado

```
GET /v1/tasks/overdue?page=0&size=10&sort=executionDate,asc
```

Devuelve **solo** las tareas vencidas (`status = SCHEDULED` Y `executionDate < now`), paginadas.

### c) Headers HTTP en todos los GET de `/v1/tasks/**`

Un interceptor (`OverdueAlertInterceptor`) inyecta estos headers en **cualquier** GET bajo `/v1/tasks`:

```http
HTTP/1.1 200 OK
X-Overdue-Count: 1
X-Overdue-Alert: Tienes 1 tarea(s) programada(s) cuya fecha ya vencio y siguen sin iniciar
Content-Type: application/json
```

El header `X-Overdue-Alert` solo aparece si el contador es > 0. CORS está configurado con `exposedHeaders` para que un frontend JS pueda leerlos.

**Cómo lo consumiría un frontend:**
```javascript
fetch('/api/v1/tasks').then(r => {
  const count = r.headers.get('X-Overdue-Count');
  if (count > 0) showBadge(r.headers.get('X-Overdue-Alert'));
});
```

**Prueba rápida:**
```bash
# 1. Carga el seed (crea la tarea id=11 overdue)
bash docs/seed.sh

# 2. Verifica los headers
curl -i http://localhost:8080/api/v1/tasks | grep -i "X-Overdue"
# X-Overdue-Count: 1
# X-Overdue-Alert: Tienes 1 tarea(s) programada(s) cuya fecha ya vencio y siguen sin iniciar

# 3. Ver el detalle
curl http://localhost:8080/api/v1/tasks/overdue
```

---

## 6. Stack y arquitectura

### Stack

- **Java 17**
- **Spring Boot 3.5.13**
- **Spring Data JPA** + **H2 Database** (en memoria)
- **Bean Validation** (Jakarta Validation)
- **Lombok** (reducción de boilerplate)
- **Springdoc OpenAPI 2.8.9** (Swagger UI)
- **JUnit 5** + **Mockito** + **MockMvc**
- **Maven** (wrapper incluido)

### Arquitectura en capas

```
src/main/java/com/imaginemos/todoapi/
├── TodoApiApplication.java
├── config/                    # OpenAPI, WebMvc, OverdueAlertInterceptor
├── controller/                # TaskController (REST endpoints)
├── service/                   # TaskService (interface)
│   └── impl/                  # TaskServiceImpl (lógica de negocio + transiciones)
├── repository/                # Spring Data JPA (TaskRepository, TaskItemRepository)
├── entity/                    # Task, TaskItem, TaskStatus
├── dto/
│   ├── request/               # CreateTaskRequest, UpdateTaskRequest, UpdateTaskStatusRequest, ...
│   └── response/              # TaskResponse, TaskItemResponse, PagedResponse<T>, ErrorResponse
├── mapper/                    # Entity ↔ DTO (beans Spring inyectables)
└── exception/                 # Custom exceptions + GlobalExceptionHandler
```

### Decisiones técnicas clave

- **Entidades sin `@Data`** → `equals`/`hashCode` explícitos por `id` para evitar recursión en relaciones bidireccionales.
- **Records para responses** → inmutables, sin boilerplate (Java 17).
- **Máquina de estados** como `Map<TaskStatus, Set<TaskStatus>>` inmutable en el servicio → declarativa y testeable, sin librería extra.
- **`@Transactional` de clase + `readOnly=true` en lecturas** → dirty checking solo donde se necesita.
- **`PagedResponse<T>` custom** → no exponemos la estructura interna de `Page` de Spring al cliente.
- **Mappers como `@Component`** → inyectables y mockeables en tests.
- **Búsqueda con JPQL + `LEFT JOIN t.items` + `SELECT DISTINCT`** → matchea también en descripciones de ítems sin duplicar resultados.

Detalle completo con alternativas evaluadas en [`.plan/03-decisiones.md`](../.plan/03-decisiones.md).

---

## 7. Testing

```bash
./mvnw test
```

### Qué se prueba

**Unit tests — `TaskServiceImplTest`** (con Mockito):
- CRUD completo
- Paginación con `PageImpl`
- Transiciones de estado (happy path, inválida, noop)
- `getPendingTasks` y `getOverdueTasks` paginados
- Gestión de ítems (toggle, add, edit, delete)

**Integration tests — `TaskControllerIntegrationTest`** (con MockMvc, Spring context completo, H2 en memoria):
- POST 201 con header Location
- GET 200 / 404
- PUT 200 con patch parcial
- DELETE 204 con cascade en ítems
- Búsqueda con `MockMvc`
- `/pending` y `/overdue` devuelven `PagedResponse` (`$.content`, `$.totalElements`)
- Transiciones de estado end-to-end
- Validaciones Bean Validation retornando 400 con `errors[]`
- Flujo completo de ítems

---

## 8. Docker

Imagen multi-stage:
- **Build stage:** `eclipse-temurin:17-jdk` + `./mvnw package`
- **Runtime stage:** `eclipse-temurin:17-jre` + usuario no-root + `MaxRAMPercentage=75`

`docker-compose.yml` incluye healthcheck sobre `/api/v3/api-docs`.

```bash
docker compose up --build
```

Archivos:
- [`Dockerfile`](./Dockerfile)
- [`.dockerignore`](./.dockerignore)
- [`../docker-compose.yml`](../docker-compose.yml)

---

## 9. Uso de IA en el desarrollo

Este proyecto fue desarrollado con asistencia de IA (Claude) usada como **pair programmer asíncrono**. La evidencia del proceso está en la carpeta [`.plan/`](../.plan/) en la raíz del repositorio:

| Archivo | Contenido |
|---|---|
| [`.plan/01-plan-general.md`](../.plan/01-plan-general.md) | Plan de trabajo por fases, alcance, criterios de aceptación |
| [`.plan/02-prompts.md`](../.plan/02-prompts.md) | Bitácora completa de los 27 prompts enviados a la IA, agrupados por fase, con contexto, aplicación y ajustes manuales |
| [`.plan/03-decisiones.md`](../.plan/03-decisiones.md) | 14 decisiones arquitectónicas con alternativas evaluadas, justificación y rol de IA |

### Cómo se usó IA

**Como herramienta, no como reemplazo.** En cada interacción:
1. Contextualicé el problema con código y restricciones reales.
2. Revisé críticamente la respuesta antes de aplicarla.
3. Compilé y probé cada cambio antes de continuar.
4. Documenté la decisión con el "por qué", no solo el "qué".

### Dónde IA aportó más valor

- **Configuración inicial** — generación rápida de `pom.xml`/`application.yml` sin buscar versiones.
- **Patrones Spring estándar** — `@ControllerAdvice`, `HandlerInterceptor`, `@PageableDefault`, configuración CORS.
- **Refactors transversales** — migración español→inglés, paginación universal, propagados a tests y Postman sin olvidos.
- **JPQL complejo** — la query de búsqueda con `LEFT JOIN DISTINCT` y parámetro nullable.
- **Diagnóstico puntual** — fix del 500 en Swagger fue identificación directa de versión incompatible de springdoc.

### Dónde hubo ajuste humano

- **Reglas de negocio específicas** — el mapa de transiciones se validó contra el enunciado.
- **Diseño de la alerta (historia 9)** — IA propuso 6 opciones; elegí la más apropiada al alcance (headers HTTP) complementando con el flag y el endpoint dedicado.
- **Redacción al usuario final** — mensajes de error y de alerta en español redactados manualmente.
- **Adaptación de tests** — aprovechar helpers ya existentes (`persistTask`, `sampleTask`) en lugar de duplicar.

---

**Contacto / Repositorio:** ver raíz del proyecto.
