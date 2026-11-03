# Suscripciones de PartidoYA

## Modelo operativo

El registro y la suscripción son operaciones separadas. Una cuenta de jugador nace con `JUGADOR_BASICO`; una cuenta de administrador nace con `CANCHA_SIN_PLAN`. Elegir un plan pagado durante el registro no modifica ese estado.

Los precios, monedas, intervalos y compatibilidad de rol se obtienen de `subscription_plans`. El frontend nunca envía un importe autorizado ni puede activar un plan.

## Flujo recurrente de Culqi

1. Angular obtiene una tarjeta tokenizada con Culqi Checkout usando importe `0`.
2. El backend crea el cliente, la tarjeta y la suscripción recurrente con la llave secreta.
3. La suscripción local permanece en `PENDING`.
4. Culqi envía un evento al webhook.
5. El backend consulta `GET /v2/events/{id}` con la llave secreta y compara el identificador.
6. Sólo `subscription.charge.succeeded` activa el plan y registra el pago.
7. Los eventos repetidos se descartan mediante la restricción única de `processed_provider_events`.

Los pagos de reserva continúan usando el flujo de cargos de `courts/payments` y no comparten tablas ni servicios con las suscripciones.

## Endpoints

| Método | Ruta | Acceso | Uso |
| --- | --- | --- | --- |
| `GET` | `/api/v1/subscription-plans` | Público | Catálogo vigente |
| `POST` | `/api/v1/subscriptions` | JWT | Iniciar una suscripción pagada |
| `GET` | `/api/v1/subscriptions/me` | JWT | Estado, plan e historial |
| `DELETE` | `/api/v1/subscriptions/me` | JWT | Cancelar la suscripción |
| `POST` | `/api/v1/subscriptions/webhooks/culqi` | Culqi | Procesar un evento verificado |

## Variables de entorno

- `CULQI_PUBLIC_KEY`
- `CULQI_SECRET_KEY`
- `CULQI_PLAN_JUGADOR_PLUS`
- `CULQI_PLAN_CANCHA_EMPRENDEDOR`
- `CULQI_PLAN_CANCHA_BUSINESS`

Cuando falta una llave o un identificador de plan, el catálogo marca la suscripción pagada como no disponible. La cuenta puede registrarse, pero no se abre el checkout ni se concede acceso pagado.

## Compatibilidad y legado

La migración marca como `legacy_plan_access` las cuentas que ya tenían Plus, Emprendedor o Business. Estas cuentas conservan el acceso sin generar cobros retroactivos. Las cuentas nuevas sólo reciben privilegios pagados después de un cobro verificado.

## Despliegue controlado

1. Exportar un respaldo lógico de MySQL.
2. Verificar las cinco variables de Culqi y los tres planes recurrentes en el mismo entorno.
3. Configurar el webhook con `https://partidoya-api-20232545.onrender.com/api/v1/subscriptions/webhooks/culqi`.
4. Ejecutar la migración mediante el arranque de Flyway.
5. Validar registro gratuito, login y consulta del catálogo.
6. Ejecutar una suscripción en sandbox y comprobar el evento confirmado.
7. Mantener disponible la imagen anterior de Render para rollback.

No deben utilizarse tarjetas ni cobros reales durante la validación previa al despliegue.
