#!/usr/bin/env bash
# Crea 11 tareas y deja varias en distintos estados:
#   - 3 quedan SCHEDULED con fecha futura (las nuevas nacen asi)
#   - 1 queda SCHEDULED con fecha en el pasado -> aparece en /overdue
#   - 3 pasan a IN_PROGRESS
#   - 2 pasan a COMPLETED  (IN_PROGRESS -> COMPLETED)
#   - 2 pasan a CANCELLED  (IN_PROGRESS -> CANCELLED)

BASE="http://localhost:8080/api/v1/tasks"

tasks=(
  '{"title":"Release v1.0","description":"Checklist inicial del release mayor","executionDate":"2026-05-01T10:00:00","itemDescriptions":["Run tests","Update changelog","Tag version","Publish artifacts"]}'
  '{"title":"Migracion de base de datos","description":"Migrar de H2 a PostgreSQL en staging","executionDate":"2026-05-10T14:30:00","itemDescriptions":["Backup actual","Crear schema","Correr Flyway","Validar datos"]}'
  '{"title":"Auditoria de seguridad","description":"Revision OWASP Top 10","executionDate":"2026-04-25T09:00:00","itemDescriptions":["SQL Injection","XSS","Auth broken","Sensitive data exposure"]}'
  '{"title":"Refactor modulo de pagos","description":"Separar logica de gateway del servicio core","executionDate":"2026-06-15T08:00:00","itemDescriptions":["Extraer interfaz","Implementar Stripe adapter","Migrar tests"]}'
  '{"title":"Documentacion API","description":"Completar ejemplos en Swagger","executionDate":"2026-04-30T12:00:00","itemDescriptions":["Endpoints de tasks","Endpoints de items","Codigos de error"]}'
  '{"title":"Reunion sprint planning","description":"Definir objetivos del sprint 24","executionDate":"2026-04-28T15:00:00","itemDescriptions":["Priorizar backlog","Estimar historias","Asignar tickets"]}'
  '{"title":"Limpieza tecnica","description":"Resolver TODOs y deprecations","executionDate":"2026-07-01T10:00:00","itemDescriptions":["Borrar codigo muerto","Actualizar dependencias","Fix warnings"]}'
  '{"title":"Setup CI/CD","description":"Pipeline con GitHub Actions","executionDate":"2026-05-20T11:00:00","itemDescriptions":["Build stage","Test stage","Deploy staging","Deploy prod"]}'
  '{"title":"Tarea sin items (vencida)","description":"Para probar endpoint /overdue","executionDate":"2026-01-15T10:00:00","itemDescriptions":[]}'
  '{"title":"Optimizacion de queries","description":"Anadir indices y revisar N+1","executionDate":"2026-06-30T16:00:00","itemDescriptions":["Analizar slow log","Crear indices","Revisar fetch lazy/eager","Benchmark antes/despues","Documentar cambios"]}'
  '{"title":"Tarea OVERDUE (SCHEDULED + fecha vencida)","description":"Debia iniciarse hace semanas, sigue sin arrancar","executionDate":"2026-03-10T09:00:00","itemDescriptions":["Retomar","Escalar al lider"]}'
)

ids=()

echo "=== Creando 11 tareas ==="
for t in "${tasks[@]}"; do
  curl -s -o /tmp/resp.json -w "HTTP %{http_code}  " -X POST "$BASE" -H "Content-Type: application/json" -d "$t"
  id=$(grep -o '"id":[0-9]*' /tmp/resp.json | head -1 | cut -d: -f2)
  title=$(grep -o '"title":"[^"]*"' /tmp/resp.json | head -1)
  echo "id=$id  $title"
  ids+=("$id")
done

set_status() {
  local id=$1
  local new=$2
  curl -s -o /dev/null -X PATCH "$BASE/$id/status" -H "Content-Type: application/json" -d "{\"newStatus\":\"$new\"}"
}

echo ""
echo "=== Ajustando estados ==="
# Indices en ids[]: 0..9 -> tareas 1..10

# Tareas 4, 5, 6 -> IN_PROGRESS (quedan asi)
for i in 3 4 5; do
  set_status "${ids[$i]}" "IN_PROGRESS"
  echo "id=${ids[$i]} -> IN_PROGRESS"
done

# Tareas 7, 8 -> IN_PROGRESS -> COMPLETED
for i in 6 7; do
  set_status "${ids[$i]}" "IN_PROGRESS"
  set_status "${ids[$i]}" "COMPLETED"
  echo "id=${ids[$i]} -> COMPLETED"
done

# Tareas 9, 10 -> IN_PROGRESS -> CANCELLED
for i in 8 9; do
  set_status "${ids[$i]}" "IN_PROGRESS"
  set_status "${ids[$i]}" "CANCELLED"
  echo "id=${ids[$i]} -> CANCELLED"
done

# Tareas 1, 2, 3 quedan en SCHEDULED por default

# Tareas 1, 2, 3, 11 quedan en SCHEDULED (la 11 es la overdue)

echo ""
echo "=== /overdue ==="
overdue_count=$(curl -s "$BASE/overdue" | grep -o '"id":[0-9]*' | wc -l)
echo "  overdue count: $overdue_count"

echo ""
echo "=== Distribucion final por estado ==="
for s in SCHEDULED IN_PROGRESS COMPLETED CANCELLED; do
  count=$(curl -s "$BASE?status=$s&size=100" | grep -o '"totalElements":[0-9]*' | cut -d: -f2)
  printf "  %-12s  totalElements=%s\n" "$s" "$count"
done

echo ""
echo "=== Total ==="
curl -s "$BASE?size=100" | grep -o '"totalElements":[0-9]*'
