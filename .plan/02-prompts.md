# Registro de Prompts - Desarrollo con IA

> **Propósito**: Este archivo documenta TODOS los prompts enviados a la IA durante el desarrollo del proyecto, las respuestas recibidas (resumidas), y cómo fueron aplicadas. Es evidencia del uso de IA asistida según los requisitos de la prueba técnica.

---

## Instrucciones de Uso

Para cada interacción con IA, documenta:

1. **Fecha y hora** de la consulta
2. **Contexto**: Qué estabas tratando de resolver
3. **Prompt enviado** (copia textual)
4. **Respuesta de IA** (resumen o puntos clave)
5. **Aplicación**: Qué código/decisiones resultaron del prompt
6. **Ajustes**: Modificaciones que hiciste a la sugerencia de IA

---

## Formato de Entrada

```markdown
### Prompt #N: [Título descriptivo]
**Fecha**: YYYY-MM-DD HH:MM
**Fase**: [Configuración / Modelo / Servicios / etc.]
**Contexto**: 
Breve explicación de por qué necesitabas ayuda de IA.

**Prompt enviado:**
```
[Copia textual del prompt]
```

**Respuesta de IA (resumen):**
- Punto clave 1
- Punto clave 2
- Punto clave 3

**Código generado/sugerido:**
```java
// Código relevante que la IA sugirió
```

**Aplicación en el proyecto:**
- Creé el archivo X con la estructura sugerida
- Modifiqué la clase Y según la recomendación
- Implementé el patrón Z como se sugirió

**Ajustes realizados:**
- Cambié X por Y porque...
- Agregué validación adicional Z
- Adapté el código a nuestro contexto específico

**Archivos afectados:**
- `src/main/java/com/ejemplo/...`
- `src/test/java/com/ejemplo/...`

---
```

---

## Ejemplo de Documentación

### Prompt #1: Configuración inicial del proyecto Spring Boot
**Fecha**: 2024-04-20 09:15
**Fase**: Configuración
**Contexto**: 
Necesitaba crear la estructura base del proyecto Spring Boot con todas las dependencias necesarias para la prueba técnica.

**Prompt enviado:**
```
Necesito crear un proyecto Spring Boot 3.x con Java 17 para una API REST de gestión de tareas.

Requisitos:
- Maven como build tool
- Dependencies: Web, JPA, H2 Database, Validation, Lombok, DevTools
- Estructura de paquetes siguiendo clean architecture
- Configuración de application.yml para desarrollo con H2

Dame:
1. La estructura de carpetas completa
2. Las dependencias en pom.xml
3. La configuración de application.yml
4. El .gitignore apropiado
```

**Respuesta de IA (resumen):**
- Sugirió estructura de paquetes por capas (controller, service, repository, entity, dto)
- Proporcionó pom.xml completo con Spring Boot 3.2.0 y Java 17
- Configuró H2 en modo desarrollo con console habilitada
- Incluyó logging apropiado y JPA show-sql para debugging

**Código generado/sugerido:**

```xml
<!-- pom.xml parcial -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <!-- ... más dependencies -->
</dependencies>
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:tododb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

**Aplicación en el proyecto:**
- Generé el proyecto con Spring Initializr usando las dependencias sugeridas
- Creé la estructura de paquetes: config/, controller/, service/, repository/, entity/, dto/, exception/, mapper/
- Configuré application.yml exactamente como se sugirió
- Agregué .gitignore con las exclusiones recomendadas

**Ajustes realizados:**
- Cambié el nombre de la base de datos a `tododb` (más descriptivo)
- Agregué profile de desarrollo separado en `application-dev.yml`
- Incluí configuración de CORS para desarrollo local
- Modifiqué logging level a DEBUG solo para mi paquete base

**Archivos afectados:**
- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `.gitignore`

**Evidencia:**
✅ Proyecto compila sin errores
✅ Spring Boot arranca correctamente
✅ H2 console accesible en http://localhost:8080/h2-console

---

### Prompt #2: Diseño de entidades JPA
**Fecha**: 2024-04-20 11:30
**Fase**: Modelo de Dominio
**Contexto**: 
Necesitaba diseñar las entidades Task y TaskItem con su relación OneToMany bidireccional, asegurando buenas prácticas de JPA.

**Prompt enviado:**
```
Necesito el diseño completo de dos entidades JPA para mi aplicación de tareas:

**Task** debe tener:
- id (auto-increment)
- title (String, max 255, obligatorio)
- description (String, max 2000, opcional)
- executionDate (LocalDateTime, opcional)
- status (TaskStatus enum: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)
- createdAt, updatedAt (timestamps automáticos)
- items (relación OneToMany con TaskItem)

**TaskItem** debe tener:
- id (auto-increment)
- description (String, max 500, obligatorio)
- completed (boolean, default false)
- task (relación ManyToOne con Task)
- createdAt (timestamp automático)

Requisitos:
- Relación bidireccional bien manejada
- Cascade ALL y orphanRemoval para items
- Métodos helper para agregar/remover items manteniendo consistencia
- @PrePersist y @PreUpdate para timestamps
- Usar Lombok para reducir boilerplate

Dame el código completo con todas las anotaciones necesarias.
```

**Respuesta de IA (resumen):**
- Proporcionó entidades completas con todas las anotaciones JPA
- Sugirió usar @OneToMany(mappedBy="task", cascade=ALL, orphanRemoval=true)
- Recomendó métodos helper: addItem(), removeItem() para mantener bidireccionalidad
- Incluyó @PrePersist y @PreUpdate para timestamps automáticos
- Usó Lombok (@Data, @NoArgsConstructor, @AllArgsConstructor)

**Código generado/sugerido:**
[Ver archivos Task.java y TaskItem.java en el commit XXX]

**Aplicación en el proyecto:**
- Creé Task.java con todas las anotaciones sugeridas
- Creé TaskItem.java con la relación inversa
- Implementé métodos helper exactamente como se recomendó
- Creé el enum TaskStatus con los 4 estados
- Agregué dependencia de Lombok al pom.xml

**Ajustes realizados:**
- Cambié inicialización de items a ArrayList en lugar de HashSet (necesito orden)
- Agregué validación en addItem() para evitar nulls
- Modifiqué removeItem() para ser más defensivo
- Agregué equals() y hashCode() personalizados basados en id

**Archivos afectados:**
- `src/main/java/com/todoapi/entity/Task.java`
- `src/main/java/com/todoapi/entity/TaskItem.java`
- `src/main/java/com/todoapi/entity/TaskStatus.java`

**Evidencia:**
✅ Entidades compilan correctamente
✅ H2 console muestra tablas creadas con estructura correcta
✅ Relación bidireccional funciona en tests manuales

---

### Prompt #1: Configuración inicial del proyecto Spring Boot
**Fecha**: 2026-04-21 21:29
**Fase**: Fase 1 - Configuración del Proyecto
**Contexto**: 
Necesitaba crear el proyecto Spring Boot base con todas las dependencias necesarias para la prueba técnica.

**Prompt enviado:**
Configurar proyecto Spring Boot 3.x con Java 17 usando Spring Initializr.
Dependencies: Web, JPA, H2, Validation, Lombok, DevTools
Group: com.imaginemos
Artifact: todo-api

**Respuesta de IA (resumen):**
- Configuración completa en Spring Initializr
- Project: Maven, Language: Java 17, Spring Boot 3.4.0.5
- Package: com.imaginemos.todoapi
- Descarga y descompresión del proyecto

**Aplicación en el proyecto:**
- Generé proyecto desde https://start.spring.io/
- Descomprimí en carpeta `backend/`
- Ejecuté `mvn clean install` exitosamente
- Arranqué aplicación con `mvn spring-boot:run`

**Resultado:**
✅ Aplicación arranca en puerto 8080
✅ H2 Database conectada: jdbc:h2:mem:tododb
✅ H2 Console disponible en /h2-console
✅ Context path configurado: /api

**Archivos afectados:**
- Todo el directorio `backend/` (proyecto completo)
- `backend/pom.xml` con todas las dependencias
- `backend/src/main/resources/application.properties`

**Evidencia:**
✅ Logs muestran: "Started TodoApiApplication in 3.995 seconds"
✅ Tomcat running on port 8080 with context path '/api'

---

## Resumen de Prompts por Fase

| Fase | Cantidad de Prompts | Archivos Afectados |
|------|--------------------|--------------------|
| Configuración | 2 | 5 |
| Modelo de Dominio | 3 | 8 |
| DTOs y Mappers | 4 | 12 |
| Servicios | 5 | 10 |
| Controladores | 3 | 5 |
| Exception Handling | 2 | 6 |
| Testing | 6 | 15 |
| Documentación | 3 | 4 |
| Docker (opcional) | 2 | 3 |
| **TOTAL** | **30** | **68** |

---

## Reflexión sobre el Uso de IA

### ¿Qué funcionó bien?
- Generación rápida de código boilerplate
- Sugerencias de mejores prácticas de Spring Boot
- Identificación de patrones comunes (mappers, DTOs, etc.)
- Ayuda con configuraciones complejas (JPA, validaciones)

### ¿Qué requirió ajustes?
- Nombres de variables/clases adaptados a nuestro contexto
- Lógica de negocio específica de la aplicación
- Optimizaciones de queries basadas en casos de uso reales
- Manejo de errores más granular

### ¿Qué aprendí?
- [Escribe aquí tus aprendizajes principales]
- [Conceptos nuevos que entendiste mejor]
- [Errores que evitarás en el futuro]

### Balance IA vs. Desarrollo Manual

**IA fue especialmente útil para:**
- ✅ Estructura inicial del proyecto
- ✅ Configuraciones de Spring Boot
- ✅ Patrones de diseño estándar
- ✅ Generación de tests base

**Desarrollé manualmente:**
- ✅ Lógica de negocio específica
- ✅ Validaciones custom
- ✅ Optimizaciones de rendimiento
- ✅ Decisiones arquitectónicas clave

---

## Notas para el Evaluador

Este archivo documenta TODO el proceso de desarrollo asistido por IA. Cada prompt representa una decisión consciente de usar IA para:
1. Aprender mejores prácticas
2. Acelerar desarrollo de código boilerplate
3. Validar decisiones técnicas
4. Resolver problemas específicos

**No fue**: Copiar y pegar código sin entender.
**Fue**: Aprender, adaptar, y construir con criterio usando IA como herramienta.

---

**Última actualización**: [Fecha]
**Total de prompts documentados**: [Número]
