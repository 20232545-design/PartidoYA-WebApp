alter table subscriptions
    add column payment_mode varchar(30) not null default 'RECURRING_LEGACY';

update subscriptions
set payment_mode = 'RECURRING_LEGACY'
where payment_mode is null or payment_mode = '';

update subscription_plans
set description = 'Acceso Plus por 30 días. Renovación manual, sin cobros automáticos.'
where code = 'JUGADOR_PLUS';

update subscription_plans
set description = 'Publicación y gestión de canchas por 30 días. Renovación manual.'
where code = 'CANCHA_EMPRENDEDOR';

update subscription_plans
set description = 'Gestión avanzada de canchas por 30 días. Renovación manual.'
where code = 'CANCHA_EMPRESA';

alter table courts add column google_place_id varchar(255);
alter table courts add column google_place_name varchar(180);
alter table courts add column surface_type varchar(60);
alter table courts add column capacity integer;
alter table courts add column covered bit not null default 0;

create table court_media (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    court_id bigint not null,
    source varchar(30) not null,
    cloudinary_public_id varchar(255),
    secure_url varchar(500) not null,
    google_photo_name varchar(500),
    attribution varchar(500),
    display_order integer not null default 0,
    primary_asset bit not null default 0,
    primary key (id),
    constraint fk_court_media_court foreign key (court_id) references courts (id),
    constraint uk_court_media_public_id unique (cloudinary_public_id)
);

create index idx_court_media_court_order on court_media (court_id, display_order);
