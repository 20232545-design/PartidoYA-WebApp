# Arquitectura modular de PartidoYA

## Diagnóstico

La versión original organizaba el backend mediante capas DDD, CQRS y Clean Architecture repetidas dentro de cada dominio. La primera refactorización conservó casi toda esa estructura. Para llegar desde un endpoint hasta su comportamiento era necesario recorrer `interfaces/rest`, `application/commandservices`, `application/internal`, comandos o queries, dominio, adaptadores JPA, ensambladores, entidades y repositorios.

La profundidad no estaba respaldada por implementaciones alternativas: la mayoría de interfaces de aplicación tenía una sola clase `Impl`, y los comandos y queries se limitaban a transportar parámetros hacia métodos sobrecargados llamados `handle`. En canchas, dos servicios genéricos concentraban creación, publicación, disponibilidad, reservas, pagos, búsquedas y reportes.

El frontend de la primera refactorización agrupaba correctamente por funcionalidad, pero añadía los niveles `features/<feature>/pages`, `data-access` y `models`. Los nombres `home`, `deportes`, `canchas` y `admin-canchas` tampoco expresaban directamente la responsabilidad técnica del componente. `CourtService` y `MatchService` reunían flujos HTTP distintos, y el dashboard mezclaba carga, ordenamiento, geolocalización, búsqueda, carrusel y acciones.

La comparación con `PartidoYA/PartidoYA-WebApp` confirmó que los contratos, integraciones y capacidades vigentes debían preservarse. La refactorización se realizó únicamente en `20232545-design/PartidoYA-WebApp-Software2`.

## Estructura backend

No se añadió una carpeta `backend` porque el `pom.xml`, `Dockerfile` y despliegue Render ya están correctamente anclados en la raíz. El árbol Java quedó organizado por funcionalidad y por cuatro responsabilidades como máximo.

```text
src/main/java/com/partidoya/platform
├── auth
│   ├── controllers
│   ├── dto
│   ├── models
│   ├── security
│   └── services
├── users
│   ├── controllers
│   ├── dto
│   ├── models
│   ├── repositories
│   └── services
├── profiles
│   ├── controllers
│   ├── dto
│   ├── models
│   ├── repositories
│   └── services
├── matches
│   ├── controllers
│   ├── dto
│   ├── models
│   ├── repositories
│   ├── services
│   └── storage
├── courts
│   ├── controllers
│   ├── dto
│   ├── models
│   ├── payments
│   ├── repositories
│   └── services
├── payments
│   ├── controllers
│   └── dto
├── shared
│   ├── config
│   ├── exceptions
│   └── models
└── PartidoYaApplication.java
```

Los contratos de repositorio y sus implementaciones JPA se conservan porque aíslan las reglas de negocio de Spring Data y mantienen pruebas y persistencia intercambiables. Se compactaron en `repositories` junto con entidades y ensambladores, evitando cuatro niveles de navegación.

## Estructura frontend

```text
frontend/src/app
├── auth
│   ├── login
│   ├── auth.service.ts
│   ├── auth.models.ts
│   ├── browser-session.store.ts
│   └── auth.guard.ts, guest.guard.ts, plan.guard.ts
├── administration
│   ├── user-management
│   ├── user-admin.service.ts
│   └── admin-user.models.ts
├── courts
│   ├── court-list
│   ├── court-detail
│   ├── court-management
│   ├── availability
│   ├── calendar
│   ├── reports
│   ├── court.service.ts
│   ├── court-availability.service.ts
│   ├── court-reservation.service.ts
│   └── court-report.service.ts
├── dashboard
│   ├── player-dashboard
│   └── dashboard.service.ts
├── matches
│   ├── match-list
│   ├── match-detail
│   ├── my-matches
│   ├── match.service.ts
│   └── join-request.service.ts
├── profiles
│   └── player-profile
├── notifications
│   ├── notification-list
│   └── notification-detail
├── marketing
│   ├── landing
│   ├── contact
│   ├── player-plans
│   └── court-plans
├── shared
│   ├── components
│   └── services
├── app.config.ts
└── app.routes.ts
```

Las URLs públicas siguen en español y no cambiaron. Los nombres internos de clases, archivos y carpetas usan inglés técnico. Los alias se redujeron a `@app`, `@shared` y `@env`.

## Equivalencias principales

| Antes | Ahora | Decisión |
| --- | --- | --- |
| `iam/application/internal/commandservices/UserCommandServiceImpl` | `auth/services/AuthService`, `users/services/UserService`, `users/services/UserAdministrationService` | Separación por autenticación, cuenta y administración |
| `iam/application/internal/queryservices/UserQueryServiceImpl` | `UserService`, `UserAdministrationService` | Consultas explícitas junto a su caso de uso |
| `profiles/.../ProfileCommandServiceImpl` + `ProfileQueryServiceImpl` | `profiles/services/ProfileService` | Una responsabilidad cohesionada sin interfaces duplicadas |
| `matches/.../MatchCommandServiceImpl` + `MatchQueryServiceImpl` | `matches/services/MatchService` | Métodos `create`, `join`, `leave`, `cancel`, `searchOpen` y consultas nombradas |
| Servicios command/query de solicitudes | `matches/services/JoinRequestService` | Flujo Yape aislado del ciclo de vida del partido |
| `courts/.../CourtCommandServiceImpl` | `CourtManagementService`, `CourtAvailabilityService`, `CourtReservationService` | Escrituras separadas por regla funcional |
| `courts/.../CourtQueryServiceImpl` | `CourtSearchService`, `CourtAvailabilityService`, `CourtReservationService`, `CourtReportService` | Consultas y cálculos con propietario explícito |
| `CourtReportCalculator` | `CourtReportService.calculate` | Cálculo determinista probado dentro del servicio responsable |
| `features/dashboard/pages/home` | `dashboard/player-dashboard` | Nombre funcional y ruta de archivo directa |
| `features/courts/pages/canchas` | `courts/court-list` | Eliminación de `features/pages` y nombre técnico claro |
| `features/courts/pages/cancha-detail` | `courts/court-detail` | Nombre técnico claro |
| `features/courts/pages/admin-canchas` | `courts/court-management` | Responsabilidad administrativa explícita |
| `features/matches/pages/deportes` | `matches/match-list` | Pantalla de listado identificable |
| `features/matches/pages/mis-partidos` | `matches/my-matches` | Nombre interno consistente |
| `core/auth/guards` | `auth` | Guards junto a autenticación, sin nivel intermedio |
| `features/*/data-access` y `features/*/models` | raíz de cada funcionalidad | Menos navegación sin perder cohesión |

## Refactorizaciones internas

- `CourtAccessPolicy` centraliza propiedad de cancha, permisos del plan y elegibilidad para reservas.
- `CourtAvailabilityService` encapsula solapamientos, bloqueos y cálculo de horarios reservables.
- `CourtReservationService` divide las validaciones de jugador, fecha, disponibilidad, conflicto, pago e idempotencia en operaciones nombradas.
- `CourtReportService` concentra rangos, ingresos, ocupación, ticket promedio y ranking.
- `MatchService` centraliza la carga segura del partido y la validación de cuentas jugador, sin comandos genéricos.
- `JoinRequestService` conserva autorización, almacenamiento de comprobantes y transición de estados como un flujo independiente.
- `UserAdministrationService` encapsula autorización administrativa, auditoría, filtros, paginación y métricas.
- `CourtService` de Angular conserva búsqueda y administración; disponibilidad, reservas y reportes tienen clientes HTTP independientes.
- `JoinRequestService` de Angular separa carga y revisión de comprobantes del servicio de partidos.
- `DashboardService` prepara resúmenes, ordena partidos, calcula proximidad, filtra direcciones y carga inscripciones. El componente conserva únicamente estado de vista, carrusel y navegación.

## Compatibilidad

- No se modificaron endpoints, métodos HTTP, parámetros, cuerpos ni respuestas públicas.
- No se modificaron tablas ni las cuatro migraciones Flyway.
- Se conservaron JWT, roles, planes y verificaciones administrativas.
- Se conservaron MySQL, Culqi, comprobantes Yape, Google Maps, OpenStreetMap y Render.
- No se modificaron HTML ni textos visibles. Los cambios SCSS se limitaron a corregir rutas relativas de las mismas imágenes después de mover archivos.
- `pom.xml`, `Dockerfile`, `render.yaml`, `angular.json` y los archivos de entorno mantienen sus rutas de despliegue.
