# Flujo de trabajo Git (obligatorio — trabajo en equipo)

- `main`: rama estable. No se trabaja ni se hace commit directamente en ella.
- `develop`: rama de integración. Toda tarea terminada se mergea aquí.
- **Una rama por tarea**, creada siempre desde `develop` actualizada:
  ```bash
  git switch develop && git pull
  git switch -c tarea/<descripcion-corta>
  ```
- Al terminar la tarea: merge a `develop` (preferentemente vía Pull Request en GitHub) y borrar la rama de la tarea.
- `develop` → `main` solo cuando se decida publicar una versión estable.
- Nunca mezclar varias tareas en la misma rama. Nunca hacer commit directo en `develop` ni en `main`.

Remoto: `git@github.com:ManuelRuaEchalar/wovra-nexo.git`.
