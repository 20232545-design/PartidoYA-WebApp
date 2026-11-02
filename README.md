# PartidoYA

Plataforma web para organizar y unirse a partidos deportivos casuales (fútbol, básquet, vóley, tenis), con reserva de canchas y pagos integrados.

## Arquitectura

Backend en Java/Spring Boot siguiendo Domain-Driven Design, organizado en bounded contexts:

- `iam` — usuarios, autenticación (JWT), roles (`JUGADOR`, `ADMIN_CANCHA`) y planes de suscripción.
- `matches` — creación, unión, cancelación y solicitudes de pago de partidos.
- `profiles` — perfil del jugador (deporte principal, nivel, preferencias).
- `courts` — canchas y reservas gestionadas por administradores.
- `payments` — configuración de la pasarela de pagos (Culqi) y comprobantes de pago por Yape.

Cada contexto sigue capas `domain → application → infrastructure → interfaces`, con Value Objects como records de Java, CQRS (servicios de comando y consulta separados), y agregados persistence-ignorant (entidades JPA y ensambladores separados del modelo de dominio).

Frontend en Angular (standalone components), consumiendo la API REST vía HTTPS/JSON.

La estructura detallada, los límites de dependencia y la comparación con la arquitectura anterior están en [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). El inventario de rutas, endpoints e integraciones está en [`docs/FUNCTIONAL_INVENTORY.md`](docs/FUNCTIONAL_INVENTORY.md).

## Tech stack

**Backend**
- Java 21, Spring Boot 4.0.6
- Spring Data JPA + MySQL
- JWT (jjwt) para autenticación
- SpringDoc OpenAPI (Swagger UI)

**Frontend**
- Angular (esbuild `@angular/build:application`)
- TypeScript, SCSS
- Google Maps JavaScript API opcional; OpenStreetMap funciona como respaldo sin clave

**Infraestructura**
- Backend y frontend desplegables en Render mediante `render.yaml`
- MySQL administrado en Aiven
- Pagos vía Culqi

## Funcionalidades principales

- Registro/login con roles diferenciados: jugador vs. administrador de cancha.
- Creación de partidos con dirección autocompletada (Google Places) y ubicación guardada.
- Panel de inicio con partidos cercanos por geolocalización y "tus partidos" (donde ya estás inscrito).
- Detalle de partido con mapa de ruta ("Cómo llegar") calculado automáticamente desde la ubicación del jugador.
- Gestión de canchas y reservas para administradores.
- Pago de partidos vía Culqi o comprobante de Yape con aprobación del organizador.

## Correr el proyecto localmente

### Backend
Requiere MySQL corriendo en `localhost:3306` (o ajustar `application-dev.properties`).

```bash
mvn spring-boot:run
```

- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend
```bash
cd frontend
npm install
npm start
```

- App: `http://localhost:4200`

## Validación

```bash
mvn test
cd frontend
npm ci
npm test
npm run build
```

Los resultados y limitaciones verificadas están en [`docs/VALIDATION.md`](docs/VALIDATION.md).

## Despliegue con Aiven y Render

1. Crea un servicio gratuito de Aiven for MySQL y copia host, puerto, usuario y contraseña.
2. En Render, crea un Blueprint desde este repositorio. `render.yaml` define el backend Docker y el frontend estático.
3. Completa las variables secretas del backend:

- `SPRING_PROFILES_ACTIVE=prod`
- `JDBC_DATABASE_URL=jdbc:mysql://HOST:PUERTO/defaultdb?sslMode=REQUIRED&serverTimezone=UTC`
- `DATABASE_USER` y `DATABASE_PASSWORD`
- `JWT_SECRET` con al menos 32 caracteres aleatorios
- `CORS_ALLOWED_ORIGINS` con el dominio exacto del frontend

Render comprueba el backend mediante `/actuator/health`. Flyway crea y valida el esquema al iniciar.
En el frontend, `API_URL` y `GOOGLE_MAPS_API_KEY` se inyectan durante `npm run build`. La clave de
Maps es opcional: sin ella, las vistas de ubicación usan OpenStreetMap y la dirección se puede
ingresar manualmente. Si se configura una clave, debe restringirse por referente HTTP y a Maps
JavaScript API, Places API y Directions API.

La instancia web gratuita de Render se suspende tras un periodo sin tráfico y puede tardar cerca de
un minuto en despertar. Aiven mantiene su servicio MySQL gratuito, pero no es una configuración de
alta disponibilidad.
