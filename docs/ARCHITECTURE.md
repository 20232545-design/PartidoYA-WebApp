# Arquitectura de PartidoYA

## Diagnóstico de la arquitectura original

El backend ya aplicaba una base DDD modular con los contextos `iam`, `profiles`, `matches`, `courts` y `payments`. Cada contexto separaba dominio, aplicación, infraestructura e interfaz REST, y aislaba el modelo de dominio de las entidades JPA mediante repositorios y ensambladores. Los principales puntos de fricción eran el controlador de canchas, que reunía descubrimiento público, administración, disponibilidad, reportes y reservas, y el servicio de consultas de canchas, que también calculaba reportes.

El frontend agrupaba todas las pantallas en `pages`, todos los accesos HTTP y servicios de navegador en `shared/services`, y todas las rutas en un único archivo. Los contratos API estaban declarados junto a las clases HTTP, la persistencia de sesión estaba mezclada con autenticación y los guards repetían el mismo flujo de redirección.

## Arquitectura objetivo

### Backend

```text
src/main/java/com/partidoya/platform
├── iam
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── interfaces/rest
├── profiles
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── interfaces/rest
├── matches
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── interfaces/rest
├── courts
│   ├── domain
│   ├── application
│   │   └── internal/queryservices
│   │       ├── CourtQueryServiceImpl.java
│   │       └── CourtReportCalculator.java
│   ├── infrastructure
│   └── interfaces/rest
│       ├── PublicCourtsController.java
│       ├── CourtManagementController.java
│       └── CourtReservationsController.java
├── payments
└── shared
```

Las dependencias siguen la dirección `interfaces e infrastructure → application → domain`. El dominio no depende de Spring Data, HTTP ni JPA. Los controladores conservan los contratos públicos y delegan en servicios de comando o consulta. `CourtReportCalculator` contiene únicamente el cálculo determinista de ingresos, ocupación y ranking; `CourtQueryServiceImpl` coordina autorización y repositorios.

### Frontend

```text
frontend/src/app
├── core
│   ├── auth
│   │   ├── guards
│   │   ├── auth.models.ts
│   │   ├── auth.service.ts
│   │   └── browser-session.store.ts
│   ├── http
│   │   └── api-base-url.token.ts
│   └── location
├── features
│   ├── access
│   ├── administration
│   ├── courts
│   ├── dashboard
│   ├── marketing
│   ├── matches
│   ├── notifications
│   └── profile
├── shared
│   └── components
├── app.config.ts
└── app.routes.ts
```

Cada módulo funcional posee sus rutas lazy, pantallas, contratos y acceso a datos cuando corresponde. `core` contiene capacidades transversales con una sola instancia: autenticación, sesión, URL base de API y localización. `shared` conserva únicamente componentes visuales realmente reutilizados.

Los alias `@core`, `@features`, `@shared` y `@env` evitan imports relativos frágiles. `API_BASE_URL` centraliza la configuración de endpoints. `BrowserSessionStore` encapsula las claves y validación de `localStorage`. Los guards retornan `UrlTree`, lo que evita navegación imperativa duplicada.

## Decisiones y compatibilidad

- No se cambió el esquema de base de datos ni se añadió una migración.
- No se cambiaron rutas frontend, endpoints REST, cuerpos de solicitud ni respuestas.
- No se reemplazaron integraciones de Google Maps, OpenStreetMap, Culqi, Yape, MySQL, Flyway o Render.
- Los HTML, recursos y reglas visuales se conservaron. Los cambios SCSS se limitaron a rutas relativas de imágenes causadas por la nueva ubicación y a retirar comentarios.
- El componente de registro independiente se eliminó porque no era alcanzable: `/register` ya cargaba `LoginComponent` en modo registro.
- Vitest y jsdom se añadieron como dependencias de desarrollo para reparar el comando `npm test` usando el builder oficial de Angular 21.
