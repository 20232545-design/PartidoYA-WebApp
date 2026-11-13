-- Los mensajes del chat se agrupan en una sola notificación por partido ("3 mensajes nuevos en …").
alter table notifications add column grouped_count integer not null default 1;

-- Una notificación agrupada se reutiliza y sube al inicio de la lista: se ordena por fecha del último evento.
create index idx_notifications_recipient_occurred on notifications (recipient_id, occurred_at);

-- Búsqueda de la notificación de chat existente de un usuario en un partido.
create index idx_notifications_recipient_match_type on notifications (recipient_id, match_id, type);
