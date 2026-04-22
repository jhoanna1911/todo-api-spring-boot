# Registro de Decisiones Técnicas

> **Propósito**: Documentar las decisiones arquitectónicas y técnicas importantes tomadas durante el desarrollo, las alternativas consideradas, y el rol de la IA en cada decisión.

---

## Formato de Decisión

Cada decisión se documenta usando el siguiente formato:

```markdown
## Decisión #N: [Título de la decisión]

**Fecha**: YYYY-MM-DD
**Estado**: [Propuesta / Aceptada / Rechazada / Obsoleta]
**Contexto**: 
Descripción del problema o situación que requería una decisión.

**Alternativas consideradas**:
1. **Opción A**: Descripción
   - Pros: ...
   - Contras: ...

2. **Opción B**: Descripción
   - Pros: ...
   - Contras: ...

3. **Opción C**: Descripción
   - Pros: ...
   - Contras: ...

**Decisión tomada**: [Opción elegida]

**Justificación**:
Explicación detallada de por qué se eligió esta opción.

**Rol de la IA**:
- ¿Consulté a IA sobre esto? [Sí/No]
- ¿Qué recomendó la IA?
- ¿Seguí la recomendación? [Total/Parcial/No]
- ¿Por qué seguí o no la recomendación?

**Consecuencias**:
- **Positivas**: ...
- **Negativas**: ...
- **Riesgos**: ...

**Archivos afectados**:
- Lista de archivos impactados por esta decisión

**Revisión futura**:
- ¿Cuándo revisar esta decisión?
- ¿Bajo qué condiciones reconsiderarla?

---
```

---

## Índice de Decisiones

1. [Base de datos para desarrollo](#decisión-1-base-de-datos-para-desarrollo)
2. [Arquitectura de capas](#decisión-2-arquitectura-de-capas)
3. [Estrategia de DTOs](#decisión-3-estrategia-de-dtos)
4. [Manejo de relaciones JPA](#decisión-4-manejo-de-relaciones-jpa)
5. [Estrategia de validación](#decisión-5-estrategia-de-validación)
6. [Gestión de estados de tareas](#decisión-6-gestión-de-estados-de-tareas)
7. [Paginación y búsqueda](#decisión-7-paginación-y-búsqueda)
8. [Manejo de excepciones](#decisión-8-manejo-de-excepciones)
9. [Estrategia de testing](#decisión-9-estrategia-de-testing)
10. [Despliegue y contenerización](#decisión-10-despliegue-y-contenerización)

---

## Decisión #1: Base de datos para desarrollo

**Fecha**: 2024-04-20
**Estado**: Aceptada

**Contexto**: 
Necesitaba elegir la base de datos para el entorno de desarrollo. La prueba técnica no especifica qué base de datos usar, solo requiere persistencia de datos.

**Alternativas consideradas**:

1. **H2 Database (in-memory)**
   - Pros: 
     - No requiere instalación
     - Rápida para desarrollo
     - Fácil de resetear
     - Console web integrada
     - Perfecta para demos
   - Contras:
     - No es para producción
     - Datos se pierden al reiniciar
     - Algunas diferencias con PostgreSQL

2. **PostgreSQL local**
   - Pros:
     - Producción-ready
     - Muy completo
     - Gran comunidad
   - Contras:
     - Requiere instalación
     - Más complejo para setup inicial
     - Puede complicar evaluación

3. **MySQL local**
   - Pros:
     - Popular
     - Bien documentado
   - Contras:
     - Requiere instalación
     - Similar complejidad a PostgreSQL

**Decisión tomada**: H2 Database (in-memory)

**Justificación**:
- Prioridad es facilitar la evaluación del proyecto
- El evaluador puede clonar y ejecutar sin instalar nada
- Es apropiado para el alcance de la prueba técnica
- Podemos configurar perfil de producción con PostgreSQL más adelante si es necesario
- Spring Boot hace trivial el cambio a otra DB

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA recomendó H2 para desarrollo y PostgreSQL para producción
- ✅ Seguí la recomendación total
- Es la práctica estándar y simplifica el setup

**Consecuencias**:
- **Positivas**: 
  - Setup instantáneo
  - Console H2 útil para debugging
  - Evaluador puede probar sin configuración
- **Negativas**: 
  - Datos no persisten entre reinicios
  - Pequeñas diferencias de SQL si migramos a producción
- **Riesgos**: 
  - Minimal, es solo para desarrollo

**Archivos afectados**:
- `src/main/resources/application.yml`
- `pom.xml` (dependencia H2)

**Revisión futura**:
- Revisar si se necesita perfil de producción con PostgreSQL
- Si el proyecto pasa a producción, implementar profile específico

---

## Decisión #2: Arquitectura de capas

**Fecha**: 2024-04-20
**Estado**: Aceptada

**Contexto**: 
Necesitaba definir la arquitectura del proyecto. Spring Boot permite muchos estilos arquitectónicos.

**Alternativas consideradas**:

1. **Arquitectura en capas tradicional (Controller → Service → Repository → Entity)**
   - Pros:
     - Separación clara de responsabilidades
     - Fácil de entender y mantener
     - Estándar de la industria
     - Bien soportado por Spring Boot
   - Contras:
     - Puede ser "overkill" para apps pequeñas
     - Más código boilerplate

2. **Clean Architecture / Hexagonal**
   - Pros:
     - Muy desacoplado
     - Testeable
     - Independiente de frameworks
   - Contras:
     - Más complejo para proyecto pequeño
     - Curva de aprendizaje más alta
     - Puede ser excesivo para el alcance

3. **Controller con lógica directa (sin Service layer)**
   - Pros:
     - Menos código
     - Más rápido de implementar
   - Contras:
     - Mezcla responsabilidades
     - Difícil de testear
     - No es profesional

**Decisión tomada**: Arquitectura en capas tradicional

**Justificación**:
- Es el estándar esperado para APIs empresariales
- Balance perfecto entre simplicidad y profesionalismo
- Facilita testing (puedo testear cada capa independientemente)
- Demuestra conocimiento de mejores prácticas
- Es lo que esperaría ver un evaluador técnico

**Estructura implementada**:
```
com.todoapi/
├── controller/      # REST endpoints
├── service/         # Lógica de negocio
├── repository/      # Acceso a datos
├── entity/          # Entidades JPA
├── dto/             # Data Transfer Objects
├── mapper/          # Entity ↔ DTO conversion
├── exception/       # Excepciones custom
└── handler/         # Exception handlers
```

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA recomendó arquitectura en capas para proyecto de este tamaño
- ✅ Seguí la recomendación total
- Es la elección correcta para el contexto

**Consecuencias**:
- **Positivas**: 
  - Código organizado y mantenible
  - Fácil de testear
  - Profesional
  - Escalable si el proyecto crece
- **Negativas**: 
  - Más archivos y clases
  - Algo de boilerplate
- **Riesgos**: 
  - Ninguno significativo

**Archivos afectados**:
- Toda la estructura de paquetes del proyecto

**Revisión futura**:
- No necesita revisión, es apropiado para el proyecto

---

## Decisión #3: Estrategia de DTOs

**Fecha**: 2024-04-20
**Estado**: Aceptada

**Contexto**: 
Necesitaba decidir cómo manejar los contratos de API (request/response). ¿Exponer entidades directamente o usar DTOs?

**Alternativas consideradas**:

1. **DTOs separados para Request y Response**
   - Pros:
     - Contratos de API claros y estables
     - No expone estructura interna de BD
     - Validaciones específicas por operación
     - Campos computados en responses
     - Flexibilidad para evolucionar
   - Contras:
     - Más código (DTOs + Mappers)
     - Duplicación aparente de campos

2. **Exponer entidades directamente**
   - Pros:
     - Menos código
     - Más rápido de implementar
   - Contras:
     - ❌ Mala práctica
     - Acopla API a estructura de BD
     - Problemas con lazy loading
     - No es profesional

3. **Un solo DTO genérico**
   - Pros:
     - Menos clases
   - Contras:
     - Confuso (mezcla request/response)
     - Validaciones complicadas

**Decisión tomada**: DTOs separados para Request y Response

**Estructura implementada**:
- `CreateTaskRequest`: Para crear tareas (validaciones estrictas)
- `UpdateTaskRequest`: Para actualizar (validaciones más flexibles)
- `TaskResponse`: Para respuestas (incluye campos computados)
- `TaskItemResponse`: Para items en respuestas
- Mappers dedicados para conversión

**Justificación**:
- Es la mejor práctica establecida en la industria
- Separa contratos de API de modelo de dominio
- Permite evolucionar la BD sin romper la API
- Facilita validaciones específicas por operación
- Demuestra conocimiento de clean code

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA enfatizó fuertemente no exponer entidades
- ✅ Seguí la recomendación total
- Es un principio fundamental de diseño de APIs

**Consecuencias**:
- **Positivas**: 
  - API limpia y profesional
  - Validaciones precisas
  - Flexibilidad para cambios
  - Fácil de documentar con Swagger
- **Negativas**: 
  - Más clases que mantener
  - Necesidad de mappers
- **Riesgos**: 
  - Ninguno, solo beneficios

**Archivos afectados**:
- `dto/request/*`
- `dto/response/*`
- `mapper/*`

**Revisión futura**:
- Evaluar si necesitamos más DTOs para casos específicos

---

## Decisión #4: Manejo de relaciones JPA

**Fecha**: 2024-04-20
**Estado**: Aceptada

**Contexto**: 
Task tiene una relación OneToMany con TaskItem. Necesitaba decidir cómo configurar esta relación en JPA.

**Alternativas consideradas**:

1. **Relación bidireccional con CASCADE.ALL y orphanRemoval**
   - Pros:
     - Manejo automático de items
     - Eliminar tarea elimina items
     - Código más limpio
     - Consistencia automática
   - Contras:
     - Requiere métodos helper bien implementados
     - Cuidado con lazy loading

2. **Relación unidireccional simple**
   - Pros:
     - Más simple inicialmente
   - Contras:
     - Requiere manejo manual
     - Más código en servicios
     - Propenso a errores

3. **Sin relación JPA (manejo manual con queries)**
   - Pros:
     - Control total
   - Contras:
     - Mucho código repetitivo
     - Fácil cometer errores
     - No aprovecha JPA

**Decisión tomada**: Relación bidireccional con CASCADE.ALL y orphanRemoval

**Implementación**:
```java
// En Task
@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
private List<TaskItem> items = new ArrayList<>();

// Métodos helper
public void addItem(TaskItem item) {
    items.add(item);
    item.setTask(this);
}

public void removeItem(TaskItem item) {
    items.remove(item);
    item.setTask(null);
}
```

**Justificación**:
- Los items son dependientes de la tarea (no existen sin ella)
- CASCADE.ALL asegura que operaciones en Task se propagan a items
- orphanRemoval elimina items huérfanos automáticamente
- Métodos helper mantienen consistencia bidireccional
- Es la configuración correcta para este tipo de relación

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA recomendó exactamente esta configuración
- ✅ Seguí la recomendación total
- Es el patrón estándar para composición padre-hijo

**Consecuencias**:
- **Positivas**: 
  - Código más limpio en servicios
  - No necesito gestionar items por separado
  - Consistencia garantizada
- **Negativas**: 
  - Necesito entender bien el cascade
  - Cuidado con N+1 queries (mitigado con fetch strategies)
- **Riesgos**: 
  - Mínimo, es configuración estándar

**Archivos afectados**:
- `entity/Task.java`
- `entity/TaskItem.java`

**Revisión futura**:
- Monitorear queries para detectar N+1 problems
- Agregar @EntityGraph si es necesario

---

## Decisión #5: Estrategia de validación

**Fecha**: 2024-04-20
**Estado**: Aceptada

**Contexto**: 
Necesitaba decidir dónde y cómo validar los datos de entrada.

**Alternativas consideradas**:

1. **Spring Validation en DTOs + validaciones de negocio en Service**
   - Pros:
     - Separación de validaciones técnicas vs. negocio
     - Mensajes de error automáticos
     - Estándar de Spring Boot
   - Contras:
     - Dos capas de validación

2. **Solo validación manual en Service**
   - Pros:
     - Todo en un lugar
   - Contras:
     - Mucho código repetitivo
     - Errores menos consistentes

3. **Solo anotaciones en entidades**
   - Pros:
     - Validación a nivel de BD
   - Contras:
     - Errores tardíos
     - Mensajes poco claros

**Decisión tomada**: Spring Validation en DTOs + validaciones de negocio en Service

**Implementación**:
```java
// En DTOs
public class CreateTaskRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title too long")
    private String title;
    
    @Future(message = "Execution date must be in future")
    private LocalDateTime executionDate;
}

// En Service
public TaskResponse updateStatus(Long id, TaskStatus newStatus) {
    Task task = findTaskOrThrow(id);
    validateStateTransition(task.getStatus(), newStatus);
    // ...
}
```

**Justificación**:
- Validaciones técnicas (formato, tamaño, nulls) → DTOs con anotaciones
- Validaciones de negocio (transiciones de estado, reglas) → Service layer
- Separación clara de responsabilidades
- Mejores mensajes de error al usuario

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA recomendó exactamente esta separación
- ✅ Seguí la recomendación total
- Es best practice establecida

**Consecuencias**:
- **Positivas**: 
  - Código más limpio
  - Errores claros y útiles
  - Fácil de testear
- **Negativas**: 
  - Validación en dos lugares
- **Riesgos**: 
  - Ninguno

**Archivos afectados**:
- `dto/request/*`
- `service/impl/TaskServiceImpl.java`

---

## Decisión #6: Gestión de estados de tareas

**Fecha**: 2024-04-20
**Estado**: Aceptada

**Contexto**: 
Las tareas tienen estados (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED) y necesitaba definir las transiciones válidas.

**Alternativas consideradas**:

1. **Enum simple + validación manual de transiciones**
   - Pros:
     - Simple de implementar
     - Flexible
   - Contras:
     - Lógica dispersa
     - Fácil olvidar validaciones

2. **State Machine (Spring State Machine)**
   - Pros:
     - Muy robusto
     - Transiciones bien definidas
   - Contras:
     - Overkill para 4 estados
     - Complejidad adicional

3. **Enum con lógica de transición integrada**
   - Pros:
     - Todo en un lugar
     - Fácil de entender
     - No requiere librería adicional
   - Contras:
     - Enum con lógica (algunos prefieren evitarlo)

**Decisión tomada**: Enum simple + validación en Service

**Reglas implementadas**:
- Nueva tarea → SCHEDULED
- SCHEDULED → IN_PROGRESS ✅
- IN_PROGRESS → COMPLETED ✅
- IN_PROGRESS → CANCELLED ✅
- Cualquier otra transición → InvalidStateTransitionException ❌

**Código**:
```java
private void validateStateTransition(TaskStatus current, TaskStatus next) {
    if (current == next) return;
    
    boolean isValid = switch (current) {
        case SCHEDULED -> next == IN_PROGRESS;
        case IN_PROGRESS -> next == COMPLETED || next == CANCELLED;
        case COMPLETED, CANCELLED -> false;
    };
    
    if (!isValid) {
        throw new InvalidStateTransitionException(
            "Cannot transition from " + current + " to " + next
        );
    }
}
```

**Justificación**:
- Son solo 4 estados con reglas simples
- No justifica librería adicional
- Lógica clara en un solo método
- Fácil de testear
- Mensajes de error claros

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA sugirió validación en service (no state machine)
- ✅ Seguí la recomendación
- State machine sería sobre-ingeniería

**Consecuencias**:
- **Positivas**: 
  - Simple y efectivo
  - Fácil de mantener
  - No dependencies extra
- **Negativas**: 
  - Tendría que refactorizar si estados crecen mucho
- **Riesgos**: 
  - Mínimo para este alcance

**Archivos afectados**:
- `entity/TaskStatus.java`
- `service/impl/TaskServiceImpl.java`
- `exception/InvalidStateTransitionException.java`

---

## Decisión #7: Paginación y búsqueda

**Fecha**: 2024-04-21
**Estado**: Aceptada

**Contexto**: 
La prueba técnica requiere listado paginado y búsqueda de tareas.

**Alternativas consideradas**:

1. **Spring Data JPA Pageable + Specifications**
   - Pros:
     - Estándar de Spring
     - Muy potente
     - Fácil de usar
   - Contras:
     - Specifications pueden ser complejas

2. **Query methods + Pageable**
   - Pros:
     - Simple para casos básicos
   - Contras:
     - Limitado para búsquedas complejas

3. **Criteria API manual**
   - Pros:
     - Control total
   - Contras:
     - Mucho código boilerplate

**Decisión tomada**: Query methods + Pageable para búsqueda básica

**Implementación**:
```java
// En Repository
Page<Task> findAll(Pageable pageable);

@Query("SELECT t FROM Task t WHERE " +
       "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))")
Page<Task> searchTasks(@Param("search") String search, Pageable pageable);

List<Task> findByStatus(TaskStatus status);
```

**Justificación**:
- Para este proyecto, búsqueda simple es suficiente (título + descripción)
- Pageable de Spring es muy fácil de usar
- No necesito filtros complejos que justifiquen Specifications
- Puedo escalar a Specifications si es necesario después

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA recomendó empezar con query methods
- ✅ Seguí la recomendación
- Es apropiado para el alcance

**Consecuencias**:
- **Positivas**: 
  - Código simple
  - Paginación funciona bien
  - Búsqueda cubre requisitos
- **Negativas**: 
  - Búsqueda es básica (LIKE puede ser lenta con muchos datos)
- **Riesgos**: 
  - Si necesito búsquedas complejas, refactorizar a Specifications

**Archivos afectados**:
- `repository/TaskRepository.java`
- `service/impl/TaskServiceImpl.java`
- `controller/TaskController.java`

**Revisión futura**:
- Considerar índices en BD si el volumen de datos crece
- Evaluar Specifications si se necesitan filtros complejos

---

## Decisión #8: Manejo de excepciones

**Fecha**: 2024-04-21
**Estado**: Aceptada

**Contexto**: 
Necesitaba un sistema consistente de manejo de errores para la API.

**Alternativas consideradas**:

1. **@ControllerAdvice global + excepciones custom**
   - Pros:
     - Centralizado
     - Consistente
     - Fácil de mantener
   - Contras:
     - Requiere planificación inicial

2. **Try-catch en cada endpoint**
   - Pros:
     - Control fino
   - Contras:
     - Código repetitivo
     - Inconsistente

3. **Solo excepciones estándar de Spring**
   - Pros:
     - Sin código extra
   - Contras:
     - Mensajes genéricos
     - Poco control

**Decisión tomada**: @ControllerAdvice global + excepciones custom

**Excepciones custom creadas**:
- `ResourceNotFoundException` → 404
- `InvalidStateTransitionException` → 400
- `ValidationException` → 400

**ErrorResponse estándar**:
```json
{
  "timestamp": "2024-04-21T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id: 123",
  "path": "/api/v1/tasks/123"
}
```

**Justificación**:
- Respuestas de error consistentes en toda la API
- Fácil de documentar y entender
- Mensajes de error claros para el cliente
- Código DRY (no repetir try-catch)

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA recomendó @ControllerAdvice como best practice
- ✅ Seguí la recomendación total
- Es el estándar de Spring Boot

**Consecuencias**:
- **Positivas**: 
  - API profesional
  - Errores útiles
  - Fácil de testear
- **Negativas**: 
  - Algunas clases adicionales
- **Riesgos**: 
  - Ninguno

**Archivos afectados**:
- `exception/*`
- `handler/GlobalExceptionHandler.java`
- `dto/response/ErrorResponse.java`

---

## Decisión #9: Estrategia de testing

**Fecha**: 2024-04-21
**Estado**: Aceptada

**Contexto**: 
Necesitaba decidir qué tipo de tests escribir y cuánta cobertura lograr.

**Alternativas consideradas**:

1. **Unit + Integration tests con >70% coverage**
   - Pros:
     - Cobertura completa
     - Confianza en el código
     - Profesional
   - Contras:
     - Más tiempo

2. **Solo unit tests básicos**
   - Pros:
     - Rápido
   - Contras:
     - No prueba integración

3. **Solo integration tests**
   - Pros:
     - Prueba flujos completos
   - Contras:
     - Más lentos
     - No aísla problemas

**Decisión tomada**: Unit tests (servicios) + Integration tests (controllers) con objetivo >70%

**Estrategia**:
- Unit tests para:
  - TaskService (mockear repository)
  - Mappers
  - Validaciones de negocio
  
- Integration tests para:
  - Endpoints REST completos
  - Flujos end-to-end
  - Validaciones de API

**Justificación**:
- Combina lo mejor de ambos mundos
- Unit tests rápidos para lógica de negocio
- Integration tests para confianza en la API
- 70% es un buen balance para proyecto de este tamaño

**Rol de la IA**:
- ✅ Consulté a IA sobre esto
- IA recomendó esta combinación
- ✅ Seguí la recomendación
- Es best practice establecida

**Consecuencias**:
- **Positivas**: 
  - Alta confianza en el código
  - Detecta regresiones
  - Documenta comportamiento esperado
- **Negativas**: 
  - Tiempo adicional de desarrollo
- **Riesgos**: 
  - Ninguno, solo beneficios

**Archivos afectados**:
- `src/test/java/**/*`

---

## Decisión #10: Despliegue y contenerización

**Fecha**: 2024-04-21
**Estado**: Propuesta

**Contexto**: 
La prueba técnica tiene como "plus opcional" Docker. Decidir si implementarlo.

**Alternativas consideradas**:

1. **Implementar Docker + Docker Compose**
   - Pros:
     - Demuestra conocimiento adicional
     - Facilita evaluación
     - Punto extra
   - Contras:
     - Tiempo adicional

2. **No implementar Docker**
   - Pros:
     - Enfoque en funcionalidad core
   - Contras:
     - Pierde oportunidad de punto extra

3. **Docker simple (solo backend)**
   - Pros:
     - Balance tiempo/beneficio
   - Contras:
     - Menos completo

**Decisión tomada**: Implementar Docker + Docker Compose (si hay tiempo)

**Plan**:
- Dockerfile multi-stage para optimizar imagen
- docker-compose.yml para levantar todo con un comando
- Documentar en README

**Justificación**:
- Es un plus que agrega valor
- No es complejo de implementar
- Demuestra conocimientos adicionales
- Si funciona, es un diferenciador

**Rol de la IA**:
- 🕐 Aún no consultado (pendiente de implementar)
- Consultaré para optimización de Dockerfile

**Consecuencias**:
- **Positivas**: 
  - Punto extra en evaluación
  - Más profesional
- **Negativas**: 
  - Tiempo adicional
- **Riesgos**: 
  - Si no hay tiempo, se puede omitir

**Archivos afectados**:
- `Dockerfile`
- `docker-compose.yml`
- `README.md`

**Revisión futura**:
- Implementar en Fase 9 si hay tiempo
- De lo contrario, omitir (es opcional)

---

## Resumen de Decisiones

| # | Decisión | Estado | IA Consultada | Recomendación Seguida |
|---|----------|--------|---------------|----------------------|
| 1 | Base de datos H2 | Aceptada | Sí | Total |
| 2 | Arquitectura en capas | Aceptada | Sí | Total |
| 3 | DTOs separados | Aceptada | Sí | Total |
| 4 | Relación bidireccional JPA | Aceptada | Sí | Total |
| 5 | Validación dual | Aceptada | Sí | Total |
| 6 | Estados simples | Aceptada | Sí | Parcial |
| 7 | Query methods + Pageable | Aceptada | Sí | Total |
| 8 | @ControllerAdvice | Aceptada | Sí | Total |
| 9 | Unit + Integration tests | Aceptada | Sí | Total |
| 10 | Docker | Propuesta | No | - |

---

## Reflexión sobre el Proceso de Decisión

### Uso de IA en las decisiones
- IA fue consultada para todas las decisiones arquitectónicas importantes
- Las recomendaciones de IA fueron seguidas en >90% de los casos
- Cuando no seguí completamente, fue por contexto específico del proyecto
- IA ayudó a validar decisiones y conocer alternativas

### Criterios de decisión principales
1. **Simplicidad apropiada**: No sobre-ingenierizar
2. **Profesionalismo**: Seguir industry best practices
3. **Mantenibilidad**: Código fácil de entender y modificar
4. **Testabilidad**: Facilitar testing
5. **Demostración de conocimiento**: Mostrar competencias técnicas

### Lecciones aprendidas
- Consultar IA para decisiones arquitectónicas ahorra tiempo
- Es importante entender el "por qué" de cada recomendación
- Balance entre simplicidad y profesionalismo es clave
- Documentar decisiones facilita revisión y aprendizaje

---

**Última actualización**: [Fecha]
**Total de decisiones documentadas**: 10
