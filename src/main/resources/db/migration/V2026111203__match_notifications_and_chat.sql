-- Notificaciones de partidos (confirmaciones, cambios y cancelaciones) dirigidas a cada usuario.
create table if not exists notifications (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    recipient_id bigint not null,
    match_id bigint not null,
    type varchar(40) not null,
    message varchar(500) not null,
    occurred_at datetime(6) not null,
    read_at datetime(6),
    primary key (id),
    constraint fk_notifications_recipient foreign key (recipient_id) references users (id),
    constraint fk_notifications_match foreign key (match_id) references matches (id)
);

-- Listado paginado (más recientes primero) y conteo de no leídas por usuario.
create index idx_notifications_recipient_id on notifications (recipient_id, id);
create index idx_notifications_recipient_read on notifications (recipient_id, read_at);

-- Chat grupal entre el organizador y los jugadores inscritos en un partido.
create table if not exists match_chat_messages (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    match_id bigint not null,
    author_id bigint not null,
    content varchar(1000) not null,
    sent_at datetime(6) not null,
    primary key (id),
    constraint fk_match_chat_messages_match foreign key (match_id) references matches (id),
    constraint fk_match_chat_messages_author foreign key (author_id) references users (id)
);

-- El historial se pagina hacia atrás por id dentro de cada partido.
create index idx_match_chat_messages_match_id on match_chat_messages (match_id, id);
