# Validación de la refactorización 2.0

## Backend

- Maven: `mvn test` completado con 37 pruebas aprobadas.
- Contexto Spring: iniciado por `DatabaseMigrationIntegrationTest` sin errores de inyección.
- Flyway: cuatro migraciones validadas y aplicadas sobre H2 desde un esquema vacío.
- Persistencia: Spring Data detectó ocho repositorios JPA.
- Pruebas de dominio: estados administrativos, pagos de partidos, solicitudes, disponibilidad de canchas y reportes aprobados.
- Búsqueda de referencias: no quedan imports a los paquetes anteriores `iam`, `application/internal`, `interfaces/rest`, `domain/model` o `infrastructure/persistence` de los módulos refactorizados.

## Frontend

- Producción: `npm run build` completado.
- Pruebas: `npm test -- --watch=false` completado con 3 pruebas Vitest aprobadas.
- Dependencias de producción: `npm audit --omit=dev --audit-level=high` informó 0 vulnerabilidades.
- Rutas: todos los lazy imports resuelven desde la estructura nueva y las URLs públicas permanecen sin cambios.
- Alias: no quedan referencias a `@core` ni `@features`.
- Estructura: no quedan carpetas `features`, `pages`, `data-access` o `models` dentro de `src/app`.

## Validación visual

Se capturó la landing antes y después con Chrome headless y el mismo servidor local:

| Resolución | Resultado |
| --- | --- |
| 390×844 | Coincidencia exacta SHA-256 |
| 1440×1000 | 1,434,154 de 1,440,000 píxeles idénticos; los 5,846 distintos están exclusivamente en `y=648..662`, la franja de ticker animada |

Las capturas están en `docs/visual-regression/before` y `docs/visual-regression/after`. No se detectaron diferencias fuera de la animación dependiente del instante de captura.

## Calidad y limpieza

- `git diff --check`: sin errores.
- No se añadieron comentarios explicativos, `TODO` ni `FIXME`.
- Se eliminaron interfaces de aplicación con una única implementación y records de comandos/queries que solo delegaban.
- Se conservaron interfaces de repositorio y almacenamiento que protegen límites de persistencia y archivos.

## Limitaciones

- Angular mantiene el warning existente del presupuesto de estilos de `player-dashboard.component.scss`: 16.44 kB frente al umbral de 12 kB. Reducirlo sin una revisión visual amplia podría alterar el diseño.
- MySQL administrado, Culqi, Google Maps y Render requieren credenciales y servicios externos. Se validaron compilación, contratos internos, contexto Spring, migraciones H2 y pruebas automatizadas; no se ejecutaron cobros ni despliegues productivos reales.
