# Plan de Trabajo - TODO API con Spring Boot

## Información del Proyecto
- **Stack**: Java 17 + Spring Boot 3.x
- **Alcance**: Backend API REST 
- **Entrega**: GitHub con documentación de uso de IA

---

## Fase 0: Preparación

### Tareas
- [ ] Crear repositorio en GitHub: `todo-api-spring-boot`
- [ ] Crear estructura de carpetas locales
  ```
  todo-api/
  ├── .plan/           # Documentación de AI
  ├── backend/         # Código Spring Boot
  └── docs/            # Documentación adicional
  ```
- [ ] Crear carpeta `.plan/` con archivos base:
  - `01-plan-general.md` (este archivo)
  - `02-prompts.md` (registro de prompts a IA)
  - `03-decisiones.md` (decisiones técnicas)

### Prompts para IA
```
Necesito crear un proyecto Spring Boot 3.x con Java 17 para una API REST 
de gestión de tareas. Dame la configuración inicial con Maven, estructura 
de paquetes recomendada, y dependencias básicas: Web, JPA, H2, Validation, Lombok.
```

---

## Fase 1: Configuración del Proyecto 

### Tareas
- [ ] Generar proyecto base con Spring Initializr
  - Java 17
  - Spring Boot 3.2.x
  - Maven
  - Dependencies: Web, JPA, H2, Validation, Lombok, DevTools
- [ ] Configurar `application.yml`:
  - Base de datos H2
  - Puerto 8080
  - Logging level DEBUG
  - H2 console habilitada
- [ ] Configurar estructura de paquetes:
  ```
  com.empresa.todoapi/
  ├── config/
  ├── controller/
  ├── dto/
  │   ├── request/
  │   └── response/
  ├── entity/
  ├── exception/
  ├── handler/
  ├── mapper/
  ├── repository/
  └── service/
      └── impl/
  ```
- [ ] Configurar `.gitignore` apropiado
- [ ] Primer commit: "Initial project setup"

### Prompts para IA
```
Dame la configuración completa de application.yml para Spring Boot con H2 
database en modo desarrollo. Incluye configuración de JPA, logging, y H2 console.
```
---

## Fase 2: Modelo de Dominio 

### Tareas

#### 2.1 Diseñar Entidades
- [ ] Crear enum `TaskStatus`:
  - SCHEDULED (programado)
  - IN_PROGRESS (en ejecución)
  - COMPLETED (finalizada)
  - CANCELLED (cancelada)

- [ ] Crear entidad `Task`:
  - id (Long, auto-increment)
  - title (String, max 255, not null)
  - description (String, max 2000)
  - executionDate (LocalDateTime)
  - status (TaskStatus, not null)
  - createdAt (LocalDateTime)
  - updatedAt (LocalDateTime)
  - items (List<TaskItem>, OneToMany)

- [ ] Crear entidad `TaskItem`:
  - id (Long, auto-increment)
  - description (String, max 500, not null)
  - completed (boolean, default false)
  - task (Task, ManyToOne)
  - createdAt (LocalDateTime)

#### 2.2 Configurar Relaciones
- [ ] Implementar relación bidireccional Task ↔ TaskItem
- [ ] Configurar CASCADE.ALL y orphanRemoval
- [ ] Implementar métodos helper para manejar la relación

#### 2.3 Crear Repositorios
- [ ] `TaskRepository extends JpaRepository<Task, Long>`
  - Métodos custom para búsqueda
  - Query para tareas vencidas
  - Filtros por estado

- [ ] `TaskItemRepository extends JpaRepository<TaskItem, Long>`

### Prompts para IA
```
Necesito el diseño completo de dos entidades JPA con relación OneToMany bidireccional:

Task:
- id, title, description, executionDate, status (enum), timestamps, items (lista)

TaskItem:
- id, description, completed (boolean), task reference

Incluye:
- Todas las anotaciones JPA necesarias
- Manejo correcto de la relación bidireccional
- Métodos helper para agregar/remover items
- @PrePersist y @PreUpdate para timestamps
```

### Criterios de Aceptación
- [ ] Entidades compilan sin errores
- [ ] Relación bidireccional funciona correctamente
- [ ] Timestamps se generan automáticamente
- [ ] H2 console muestra las tablas creadas

### Evidencia
- Código de entidades completo
- Screenshot de H2 console con estructura de tablas

---

## Fase 3: Capa de Transferencia (DTOs) (3-4 horas)

### Tareas

#### 3.1 Request DTOs
- [ ] `CreateTaskRequest`:
  - title (validación: @NotBlank, @Size)
  - description (@Size max 2000)
  - executionDate (@Future opcional)
  - itemDescriptions (List<String>)

- [ ] `UpdateTaskRequest`:
  - Similar a CreateTaskRequest pero todos los campos opcionales
  - Incluir validaciones condicionales

- [ ] `UpdateTaskStatusRequest`:
  - newStatus (@NotNull)

#### 3.2 Response DTOs
- [ ] `TaskResponse`:
  - Todos los campos de Task
  - items (List<TaskItemResponse>)
  - isOverdue (campo computado)
  - itemsCompletedCount (campo computado)
  - itemsTotalCount (campo computado)

- [ ] `TaskItemResponse`:
  - id, description, completed

- [ ] `PagedResponse<T>`:
  - content (List<T>)
  - pageNumber, pageSize, totalElements, totalPages
  - isFirst, isLast

#### 3.3 Mappers
- [ ] `TaskMapper`:
  - toEntity(CreateTaskRequest) → Task
  - toResponse(Task) → TaskResponse
  - toEntity(UpdateTaskRequest, Task) → Task (actualizar existente)

- [ ] `TaskItemMapper`:
  - toResponse(TaskItem) → TaskItemResponse

### Prompts para IA
```
Dame la implementación completa de DTOs para una API REST de tareas:

1. CreateTaskRequest con validaciones Spring Validation
2. UpdateTaskRequest con campos opcionales
3. TaskResponse con campos computados (isOverdue, progress)
4. Mappers para convertir entre Entity y DTO

Usa patrones clean code y builder pattern donde sea apropiado.
```

### Criterios de Aceptación
- [ ] Todas las validaciones funcionan
- [ ] Mappers convierten correctamente
- [ ] Campos computados se calculan bien
- [ ] No se exponen entidades directamente

---

## Fase 4: Capa de Servicio (6-8 horas)

### Tareas

#### 4.1 Crear Interfaces de Servicio
- [ ] `TaskService` interface con todos los métodos:
  ```java
  TaskResponse createTask(CreateTaskRequest);
  TaskResponse updateTask(Long id, UpdateTaskRequest);
  void deleteTask(Long id);
  TaskResponse getTaskById(Long id);
  Page<TaskResponse> getAllTasks(Pageable);
  Page<TaskResponse> searchTasks(String query, Pageable);
  TaskResponse updateTaskStatus(Long id, TaskStatus);
  List<TaskResponse> getPendingTasks();
  List<TaskResponse> getOverdueTasks();
  TaskResponse updateTaskItem(Long taskId, Long itemId, boolean completed);
  ```

#### 4.2 Implementar Servicios
- [ ] `TaskServiceImpl`:
  - Inyección de dependencias por constructor
  - Anotación @Transactional en clase
  - @Transactional(readOnly = true) en lecturas
  - Manejo de excepciones con customs exceptions
  - Lógica de negocio para estados
  - Validación de reglas de negocio

#### 4.3 Reglas de Negocio
- [ ] Nueva tarea siempre inicia en SCHEDULED
- [ ] Validar transiciones de estado válidas:
  - SCHEDULED → IN_PROGRESS
  - IN_PROGRESS → COMPLETED
  - IN_PROGRESS → CANCELLED
  - (bloquear transiciones inválidas)
- [ ] Calcular si tarea está vencida:
  - status == SCHEDULED && executionDate < now
- [ ] Filtrar tareas pendientes:
  - status == SCHEDULED || status == IN_PROGRESS

#### 4.4 Búsqueda y Filtrado
- [ ] Implementar búsqueda por:
  - Título (LIKE case-insensitive)
  - Descripción (LIKE case-insensitive)
- [ ] Implementar filtros por:
  - Estado
  - Rango de fechas
- [ ] Soporte para paginación y ordenamiento

### Prompts para IA
```
Necesito la implementación completa de TaskService con Spring:

Métodos:
- CRUD completo
- Búsqueda con paginación
- Filtros por estado
- Detección de tareas vencidas
- Validación de transiciones de estado

Requisitos:
- @Transactional apropiado
- Excepciones custom (ResourceNotFoundException, InvalidStateTransitionException)
- Validación de reglas de negocio
- Uso de mappers para DTO ↔ Entity
```

### Criterios de Aceptación
- [ ] Todas las operaciones CRUD funcionan
- [ ] Paginación funciona correctamente
- [ ] Búsqueda retorna resultados correctos
- [ ] Validaciones de estado funcionan
- [ ] Tareas vencidas se detectan correctamente
- [ ] Excepciones se lanzan apropiadamente

---

## Fase 5: Capa de Controladores (4-6 horas)

### Tareas

#### 5.1 Crear TaskController
- [ ] Anotar con @RestController y @RequestMapping("/api/v1/tasks")
- [ ] Inyección de TaskService por constructor

#### 5.2 Implementar Endpoints

**Gestión básica:**
- [ ] `POST /api/v1/tasks`
  - Body: CreateTaskRequest
  - Response: 201 Created + TaskResponse
  
- [ ] `GET /api/v1/tasks/{id}`
  - Response: 200 OK + TaskResponse
  - Error: 404 si no existe

- [ ] `PUT /api/v1/tasks/{id}`
  - Body: UpdateTaskRequest
  - Response: 200 OK + TaskResponse
  
- [ ] `DELETE /api/v1/tasks/{id}`
  - Response: 204 No Content

**Listado y búsqueda:**
- [ ] `GET /api/v1/tasks`
  - Params: page, size, sort
  - Response: 200 OK + Page<TaskResponse>

- [ ] `GET /api/v1/tasks/search`
  - Params: query, page, size
  - Response: 200 OK + Page<TaskResponse>

**Gestión de estado:**
- [ ] `PATCH /api/v1/tasks/{id}/status`
  - Params: status
  - Response: 200 OK + TaskResponse

**Consultas especiales:**
- [ ] `GET /api/v1/tasks/pending`
  - Response: 200 OK + List<TaskResponse>

- [ ] `GET /api/v1/tasks/overdue`
  - Response: 200 OK + List<TaskResponse>

**Gestión de items:**
- [ ] `PATCH /api/v1/tasks/{taskId}/items/{itemId}`
  - Params: completed
  - Response: 200 OK + TaskResponse

#### 5.3 Validaciones
- [ ] Usar @Valid en todos los @RequestBody
- [ ] Validar @PathVariable cuando sea necesario
- [ ] Validar @RequestParam con defaults apropiados

### Prompts para IA
```
Dame la implementación completa de un RestController para gestión de tareas.

Endpoints necesarios:
- CRUD completo (POST, GET, PUT, DELETE)
- Listado paginado con sorting
- Búsqueda
- Cambio de estado (PATCH)
- Consultas especiales: pending, overdue

Requisitos:
- Seguir convenciones REST
- HTTP status codes apropiados
- Validaciones con @Valid
- Manejo de errores
- Paginación con Pageable
```

### Criterios de Aceptación
- [ ] Todos los endpoints responden correctamente
- [ ] Status codes HTTP apropiados
- [ ] Validaciones funcionan
- [ ] Paginación funciona
- [ ] Errores retornan formato consistente

---

## Fase 6: Manejo de Errores (2-3 horas)

### Tareas

#### 6.1 Crear Excepciones Custom
- [ ] `ResourceNotFoundException extends RuntimeException`
- [ ] `InvalidStateTransitionException extends RuntimeException`
- [ ] `ValidationException extends RuntimeException`

#### 6.2 Crear ErrorResponse DTO
- [ ] timestamp
- [ ] status (HTTP status code)
- [ ] error (mensaje de error)
- [ ] message (detalles)
- [ ] path (endpoint que falló)
- [ ] errors (lista de errores de validación)

#### 6.3 Implementar GlobalExceptionHandler
- [ ] @ControllerAdvice
- [ ] Manejar ResourceNotFoundException → 404
- [ ] Manejar InvalidStateTransitionException → 400
- [ ] Manejar MethodArgumentNotValidException → 400
- [ ] Manejar Exception genérica → 500

### Prompts para IA
```
Necesito un sistema robusto de manejo de errores para Spring Boot API:

1. Excepciones custom para casos de negocio
2. DTO ErrorResponse con estructura consistente
3. @ControllerAdvice para manejo global
4. Mapeo de excepciones a HTTP status codes
5. Manejo especial para errores de validación (@Valid)

La respuesta de error debe ser JSON con:
- timestamp
- status
- error
- message
- path
- errors (array, para validaciones)
```

### Criterios de Aceptación
- [ ] Todos los errores retornan JSON consistente
- [ ] Status codes apropiados por tipo de error
- [ ] Mensajes de error son claros y útiles
- [ ] Errores de validación muestran todos los campos inválidos

---

## Fase 7: Testing (6-8 horas)

### Tareas

#### 7.1 Unit Tests - Servicios
- [ ] `TaskServiceTest`:
  - Test createTask exitoso
  - Test createTask con validaciones
  - Test updateTask exitoso
  - Test updateTask recurso no existe
  - Test deleteTask
  - Test cambio de estado válido
  - Test cambio de estado inválido
  - Test búsqueda
  - Test paginación
  - Test detectar tareas vencidas

#### 7.2 Integration Tests - Controllers
- [ ] `TaskControllerIntegrationTest`:
  - Test POST /tasks → 201
  - Test GET /tasks/{id} → 200
  - Test GET /tasks/{id} no existe → 404
  - Test PUT /tasks/{id} → 200
  - Test DELETE /tasks/{id} → 204
  - Test GET /tasks con paginación
  - Test GET /tasks/search
  - Test PATCH /tasks/{id}/status
  - Test validaciones retornan 400

#### 7.3 Coverage
- [ ] Configurar JaCoCo para coverage
- [ ] Objetivo: >70% coverage
- [ ] Generar reporte HTML

### Prompts para IA
```
Necesito tests completos para mi API Spring Boot de tareas:

1. Unit tests para TaskService usando Mockito
   - Mockear repository y mapper
   - Probar lógica de negocio
   - Probar manejo de excepciones

2. Integration tests para TaskController
   - MockMvc para simular requests HTTP
   - Validar responses y status codes
   - Probar flujos end-to-end

Dame ejemplos concretos con @SpringBootTest, @WebMvcTest, y assertions.
```

### Criterios de Aceptación
- [ ] Todos los tests pasan
- [ ] Coverage > 70%
- [ ] Tests documentan comportamiento esperado
- [ ] Tests son independientes y repetibles

---

## Fase 8: Documentación (3-4 horas)

### Tareas

#### 8.1 README.md Principal
- [ ] Título y descripción del proyecto
- [ ] Características implementadas
- [ ] Stack tecnológico
- [ ] Requisitos previos (Java 17, Maven)
- [ ] Instrucciones de instalación paso a paso
- [ ] Cómo ejecutar el proyecto
- [ ] Cómo ejecutar tests
- [ ] Endpoints disponibles (tabla resumen)
- [ ] Ejemplos de uso con curl o Postman
- [ ] Estructura del proyecto
- [ ] Decisiones técnicas importantes
- [ ] Referencias a carpeta .plan/

#### 8.2 Documentación en .plan/
- [ ] **01-plan-general.md**:
  - Objetivo del proyecto
  - Arquitectura elegida
  - Fases de desarrollo
  - Decisiones de diseño
  - Checklist de completitud

- [ ] **02-prompts.md**:
  - Todos los prompts enviados a IA
  - Respuestas recibidas (resumen)
  - Cómo se aplicaron las sugerencias
  - Ajustes realizados

- [ ] **03-decisiones.md**:
  - Decisiones arquitectónicas
  - Justificación de cada decisión
  - Alternativas consideradas
  - Rol de la IA en cada decisión

#### 8.3 API Documentation
- [ ] Agregar dependencia Springdoc OpenAPI
- [ ] Configurar Swagger UI en /swagger-ui.html
- [ ] Documentar endpoints con @Operation
- [ ] Documentar schemas con @Schema
- [ ] Exportar colección Postman (opcional)

### Prompts para IA
```
Ayúdame a crear un README.md profesional para mi API REST de tareas.

Debe incluir:
- Descripción del proyecto
- Features implementadas
- Stack tecnológico
- Instrucciones de instalación y ejecución
- Tabla de endpoints con método, ruta, descripción
- Ejemplos de requests/responses
- Estructura del proyecto
- Cómo ejecutar tests

Tono: profesional pero accesible, como un proyecto open source de calidad.
```

### Criterios de Aceptación
- [ ] README es claro y completo
- [ ] Alguien puede clonar y ejecutar siguiendo el README
- [ ] .plan/ muestra claramente el uso de IA
- [ ] Swagger UI funciona y documenta todos los endpoints

---

## Fase 9: Docker (Opcional - Plus) (2-3 horas)

### Tareas

#### 9.1 Dockerfile
- [ ] Base image: openjdk:17-alpine
- [ ] Copiar JAR
- [ ] Exponer puerto 8080
- [ ] Comando de ejecución

#### 9.2 Docker Compose
- [ ] Servicio backend
- [ ] (Opcional) Servicio PostgreSQL
- [ ] Network configuration
- [ ] Variables de entorno

#### 9.3 Documentación
- [ ] Actualizar README con instrucciones Docker
- [ ] Comandos para build y run
- [ ] Troubleshooting común

### Prompts para IA
```
Necesito contenerizar mi aplicación Spring Boot:

1. Dockerfile optimizado para producción
   - Multi-stage build
   - Imagen pequeña (Alpine)
   - Non-root user

2. docker-compose.yml para desarrollo local
   - Backend service
   - Variables de entorno

3. Instrucciones para el README

El comando `docker-compose up` debe levantar todo.
```

### Criterios de Aceptación
- [ ] Docker build exitoso
- [ ] Contenedor arranca sin errores
- [ ] API accesible desde host
- [ ] Variables de entorno funcionan

---

## Fase 10: Revisión Final (2-3 horas)

### Checklist de Calidad

#### Código
- [ ] Todo el código compila sin warnings
- [ ] No hay código comentado innecesario
- [ ] Nombres de variables/métodos son descriptivos
- [ ] No hay código duplicado
- [ ] Logs apropiados en lugares clave
- [ ] No hay credenciales hardcodeadas

#### Funcionalidad
- [ ] ✅ Crear tarea
- [ ] ✅ Editar tarea
- [ ] ✅ Eliminar tarea
- [ ] ✅ Consultar tarea por ID
- [ ] ✅ Listar tareas paginadas
- [ ] ✅ Buscar tareas
- [ ] ✅ Gestionar estados
- [ ] ✅ Visualizar tareas pendientes
- [ ] ✅ Alertar tareas vencidas
- [ ] ✅ Gestionar ítems checkeables

#### Testing
- [ ] Todos los tests unitarios pasan
- [ ] Todos los tests de integración pasan
- [ ] Coverage > 70%
- [ ] No hay tests ignorados sin justificación

#### Documentación
- [ ] README completo y claro
- [ ] .plan/01-plan-general.md completo
- [ ] .plan/02-prompts.md con todos los prompts
- [ ] .plan/03-decisiones.md con decisiones clave
- [ ] Swagger UI accesible y completo
- [ ] Código tiene comentarios donde es necesario

#### Git/GitHub
- [ ] Repositorio público en GitHub
- [ ] .gitignore apropiado
- [ ] Commits con mensajes descriptivos
- [ ] Estructura de carpetas clara
- [ ] README visible desde página principal
- [ ] .plan/ visible en repositorio

#### Extras
- [ ] Docker funcional (si se implementó)
- [ ] Colección Postman (opcional)
- [ ] CI/CD configurado (opcional)

---

## Estimación de Tiempo Total

| Fase | Tiempo Estimado |
|------|----------------|
| 0. Preparación | 2-3 horas |
| 1. Configuración | 3-4 horas |
| 2. Modelo de Dominio | 4-6 horas |
| 3. DTOs | 3-4 horas |
| 4. Servicios | 6-8 horas |
| 5. Controladores | 4-6 horas |
| 6. Manejo de Errores | 2-3 horas |
| 7. Testing | 6-8 horas |
| 8. Documentación | 3-4 horas |
| 9. Docker (opcional) | 2-3 horas |
| 10. Revisión Final | 2-3 horas |
| **TOTAL** | **37-52 horas** |

**Distribución recomendada:**
- Día 1: Fases 0-2 (9-13 horas)
- Día 2: Fases 3-4 (9-12 horas)
- Día 3: Fases 5-6 (6-9 horas)
- Día 4: Fase 7 (6-8 horas)
- Día 5: Fases 8-10 (7-10 horas)

---

## Estrategia de Uso de IA

### Cuándo Usar IA
✅ **Sí usar IA para:**
- Generar código boilerplate
- Sugerir estructura de proyecto
- Revisar mejores prácticas
- Generar configuraciones
- Crear tests base
- Documentación técnica
- Resolver dudas específicas
- Code review y optimización

❌ **No usar IA para:**
- Copiar código sin entender
- Evitar aprender conceptos
- Generar todo el proyecto de una vez
- Delegar decisiones importantes sin criterio

### Workflow Recomendado
1. **Planificar** la tarea específica
2. **Consultar** a IA con prompt claro y contexto
3. **Revisar** la respuesta críticamente
4. **Adaptar** el código a tus necesidades
5. **Documentar** en .plan/ qué pediste y cómo lo usaste
6. **Probar** que funcione correctamente
7. **Entender** qué hace cada parte del código

---

## Contactos y Recursos

### Documentación Oficial
- Spring Boot: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Validation: https://spring.io/guides/gs/validating-form-input/

### Herramientas
- Spring Initializr: https://start.spring.io/
- Postman: https://www.postman.com/
- H2 Console: http://localhost:8080/h2-console

---

**¡Éxito en tu prueba técnica!** 🚀

Recuerda: La calidad del código y la documentación del proceso son tan importantes como la funcionalidad.
