-- Los jugadores siempre usan el plan gratuito y los administradores de cancha siempre pagan.
-- Se desactivan (no se borran) para conservar el historial de usuarios y pagos que los referencian.
update subscription_plans
set active = 0
where code in ('JUGADOR_PLUS', 'CANCHA_SIN_PLAN');
