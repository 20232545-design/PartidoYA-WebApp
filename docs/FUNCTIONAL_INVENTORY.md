# Inventario funcional y contratos

## Roles, sesión y planes

- Registro e inicio de sesión con JWT.
- Roles `JUGADOR`, `ADMIN_CANCHA` y `ADMIN_GENERAL`.
- Planes de jugador, administrador de cancha y administrador general.
- Guards de autenticación, invitado, jugador, administrador de cancha y administrador general.
- Persistencia de token, usuario, correo, rol y plan en sesión local del navegador.
- Activación, suspensión y reactivación administrativa de usuarios con historial de acciones.

## Partidos

- Consulta de partidos abiertos y filtrado por deporte.
- Creación de partido con dirección, fecha, cupos, precio y coordenadas.
- Unión, abandono y cancelación de partidos.
- Consulta de partidos organizados y partidos en los que participa un jugador.
- Pago requerido mediante comprobante Yape.
- Carga, consulta, aprobación y rechazo de comprobantes por el organizador.
- Notificaciones de solicitudes pendientes de verificación.
- Visualización de ubicación, ruta y distancia con Google Maps u OpenStreetMap.

## Canchas y reservas

- Búsqueda de canchas publicadas por deporte y distrito.
- Detalle, horarios reservables e imágenes de cancha.
- Alta, actualización y publicación de canchas por el propietario.
- Reserva con control de horarios y pago mediante Culqi.
- Calendario del propietario con vistas mensual, semanal y diaria.
- Bloqueos y disponibilidades explícitas por cancha y fecha.
- Reportes de ingresos, reservas, ocupación, ticket promedio y ranking.

## Perfiles y navegación pública

- Perfil del usuario, nombre, deporte principal, nivel y disponibilidad.
- Landing, contacto y páginas de planes.
- Dashboard con partidos cercanos, deportes y partidos inscritos.
- Navbar y footer compartidos con navegación sensible al rol.

## Rutas frontend conservadas

| Acceso | Rutas |
| --- | --- |
| Público | `/`, `/login`, `/register`, `/contactanos`, `/planes-jugador`, `/planes-cancha` |
| Autenticado | `/home`, `/mi-perfil`, `/profile`, `/notificaciones`, `/notificaciones/:matchId/:requestId` |
| Jugador | `/deportes`, `/canchas`, `/canchas/:id`, `/mis-partidos`, `/partidos/:id` |
| Administrador de cancha | `/admin-canchas`, `/calendar`, `/availability`, `/reports` |
| Administrador general | `/admin-users` |

## Endpoints REST conservados

### Autenticación y usuarios

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}`
- `PATCH /api/v1/users/{userId}/plan`
- `GET /api/v1/users/admin`
- `GET /api/v1/users/admin/{userId}`
- `PATCH /api/v1/users/admin/{userId}/activate`
- `PATCH /api/v1/users/admin/{userId}/suspend`
- `PATCH /api/v1/users/admin/{userId}/reactivate`

### Perfiles

- `POST /api/v1/profiles`
- `GET /api/v1/profiles/{profileId}`
- `GET /api/v1/profiles/user/{userId}`
- `PATCH /api/v1/profiles/user/{userId}/primary-sport`
- `PATCH /api/v1/profiles/user/{userId}/game-preferences`

### Partidos y comprobantes

- `POST /api/v1/matches`
- `GET /api/v1/matches`
- `GET /api/v1/matches/{matchId}`
- `POST /api/v1/matches/{matchId}/join`
- `POST /api/v1/matches/{matchId}/leave`
- `POST /api/v1/matches/{matchId}/cancel`
- `GET /api/v1/matches/organized/{userId}`
- `GET /api/v1/matches/joined/{userId}`
- `POST /api/v1/matches/{matchId}/join-requests`
- `GET /api/v1/matches/{matchId}/join-requests`
- `POST /api/v1/matches/{matchId}/join-requests/{requestId}/approve`
- `POST /api/v1/matches/{matchId}/join-requests/{requestId}/reject`
- `GET /api/v1/matches/{matchId}/join-requests/{requestId}/proof`

### Canchas y pagos

- `GET /api/v1/courts`
- `GET /api/v1/courts/{courtId}`
- `GET /api/v1/courts/{courtId}/available-schedules`
- `POST /api/v1/courts`
- `PUT /api/v1/courts/{courtId}`
- `POST /api/v1/courts/{courtId}/publication`
- `POST /api/v1/courts/{courtId}/reservations`
- `GET /api/v1/courts/managed/{ownerId}`
- `GET /api/v1/courts/managed/{ownerId}/reservations`
- `GET /api/v1/courts/managed/{ownerId}/availability`
- `POST /api/v1/courts/managed/{ownerId}/availability`
- `PUT /api/v1/courts/managed/{ownerId}/availability/{availabilityId}`
- `DELETE /api/v1/courts/managed/{ownerId}/availability/{availabilityId}`
- `GET /api/v1/courts/managed/{ownerId}/reports`
- `GET /api/v1/payments/config`

## Persistencia e integraciones

- MySQL en producción y H2 en pruebas de migración.
- Flyway con cuatro migraciones existentes, sin cambios de esquema en esta refactorización.
- JWT con secreto externo.
- Culqi para cobros de reservas.
- Comprobantes Yape almacenados mediante la abstracción `PaymentProofStorage`.
- Google Maps/Places/Directions opcional y OpenStreetMap como respaldo.
- Render para backend Docker y frontend estático.

## Variables de entorno

- Backend: `SPRING_PROFILES_ACTIVE`, `JDBC_DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `CORS_ALLOWED_ORIGIN_PATTERNS`, `CULQI_PUBLIC_KEY`, `CULQI_SECRET_KEY`.
- Frontend: `API_URL`, `GOOGLE_MAPS_API_KEY`.
