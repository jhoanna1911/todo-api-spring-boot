# Ejecuta este comando para crear el archivo con contenido
@"
---
name: spring-boot-todo-api
description: Use this skill for developing Java Spring Boot REST APIs, especially TODO/task management applications. Triggers when user mentions Java, Spring Boot, REST API, backend development, task management, TODO apps, or technical tests requiring Java.
---

# Spring Boot TODO API Development Skill

Este skill guía el desarrollo de APIs REST enterprise-grade con Spring Boot.

## Arquitectura en Capas

**Flujo:** Controller → Service → Repository → Entity

- **Controller**: Endpoints REST, validación de input
- **Service**: Lógica de negocio, transacciones
- **Repository**: Acceso a datos con Spring Data JPA
- **Entity**: Modelo de dominio

## Reglas Fundamentales

1. **Nunca exponer entidades directamente**
   - Usar DTOs para request y response
   - CreateXRequest, UpdateXRequest, XResponse

2. **Inyección de dependencias**
   - Constructor injection (no @Autowired en fields)

3. **Transacciones**
   - @Transactional en clase de servicio
   - @Transactional(readOnly=true) para lecturas

4. **Validación dual**
   - Spring Validation en DTOs (@Valid, @NotBlank, etc.)
   - Lógica de negocio en Service

5. **Manejo de errores**
   - @ControllerAdvice global
   - Excepciones custom con mensajes claros

## Estructura de Proyecto

\`\`\`
src/main/java/com/todoapi/
├── config/              # Configuración Spring
├── controller/          # REST endpoints
├── dto/
│   ├── request/        # CreateTaskRequest, etc.
│   └── response/       # TaskResponse, etc.
├── entity/             # Task, TaskItem (JPA)
├── exception/          # Custom exceptions
├── handler/            # GlobalExceptionHandler
├── mapper/             # Entity ↔ DTO
├── repository/         # Spring Data JPA
└── service/
    └── impl/           # Implementaciones
\`\`\`

## Documentación Requerida

Cada interacción con IA debe documentarse en:
- \`.plan/02-prompts.md\`: Todos los prompts y respuestas
- \`.plan/03-decisiones.md\`: Decisiones técnicas

"@ | Out-File -FilePath ".claude/skills/spring-boot-todo-api/SKILL.md" -Encoding UTF8