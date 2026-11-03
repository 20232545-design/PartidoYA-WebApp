alter table users
    add column first_name varchar(60);

alter table users
    add column last_name varchar(60);

alter table users
    add column terms_accepted_at datetime(6);

alter table users
    add column privacy_accepted_at datetime(6);

alter table users
    add column marketing_consent bit not null default 0;

alter table users
    add column legacy_plan_access bit not null default 0;

update users
set first_name = full_name,
    last_name = '',
    legacy_plan_access = case
        when plan in ('JUGADOR_PLUS', 'CANCHA_EMPRENDEDOR', 'CANCHA_BUSINESS') then 1
        else 0
    end;

alter table profiles
    add column district varchar(80);

alter table profiles
    add column business_name varchar(140);

alter table profiles
    add column business_phone varchar(20);

alter table profiles
    add column operation_district varchar(80);

create table subscription_plans (
    code varchar(40) not null,
    display_name varchar(80) not null,
    description varchar(300) not null,
    role varchar(30) not null,
    amount_cents integer not null,
    currency varchar(3) not null,
    interval_unit varchar(20) not null,
    active bit not null,
    display_order integer not null,
    primary key (code)
);

insert into subscription_plans
    (code, display_name, description, role, amount_cents, currency, interval_unit, active, display_order)
values
    ('JUGADOR_BASICO', 'Básico', 'Organiza y participa en partidos sin costo.', 'JUGADOR', 0, 'PEN', 'MONTH', 1, 10),
    ('JUGADOR_PLUS', 'Plus', 'Incluye insignia Plus y gestión de renovación e historial de pagos.', 'JUGADOR', 1490, 'PEN', 'MONTH', 1, 20),
    ('CANCHA_SIN_PLAN', 'Sin plan comercial', 'Crea tu cuenta y contrata un plan cuando quieras; no incluye operaciones comerciales.', 'ADMIN_CANCHA', 0, 'PEN', 'MONTH', 1, 25),
    ('CANCHA_EMPRENDEDOR', 'Emprendedor', 'Administra una cancha, disponibilidad y reservas.', 'ADMIN_CANCHA', 2990, 'PEN', 'MONTH', 1, 30),
    ('CANCHA_BUSINESS', 'Business', 'Administra múltiples canchas y accede a reportes del negocio.', 'ADMIN_CANCHA', 5990, 'PEN', 'MONTH', 1, 40);

create table subscriptions (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    user_id bigint not null,
    plan_code varchar(40) not null,
    status varchar(30) not null,
    provider varchar(30) not null,
    provider_customer_id varchar(120),
    provider_card_id varchar(120),
    provider_subscription_id varchar(120),
    idempotency_key varchar(120) not null,
    current_period_start datetime(6),
    current_period_end datetime(6),
    next_renewal_at datetime(6),
    canceled_at datetime(6),
    primary key (id),
    constraint fk_subscriptions_user foreign key (user_id) references users (id),
    constraint fk_subscriptions_plan foreign key (plan_code) references subscription_plans (code),
    constraint uk_subscriptions_idempotency unique (idempotency_key),
    constraint uk_subscriptions_provider_id unique (provider_subscription_id)
);

create table subscription_payments (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    subscription_id bigint not null,
    user_id bigint not null,
    plan_code varchar(40) not null,
    amount_cents integer not null,
    currency varchar(3) not null,
    status varchar(30) not null,
    provider_transaction_id varchar(120),
    provider_event_id varchar(120),
    paid_at datetime(6),
    failure_reason varchar(300),
    primary key (id),
    constraint fk_subscription_payments_subscription foreign key (subscription_id) references subscriptions (id),
    constraint fk_subscription_payments_user foreign key (user_id) references users (id),
    constraint fk_subscription_payments_plan foreign key (plan_code) references subscription_plans (code),
    constraint uk_subscription_payments_event unique (provider_event_id),
    constraint uk_subscription_payments_transaction unique (provider_transaction_id)
);

create table processed_provider_events (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    provider varchar(30) not null,
    provider_event_id varchar(120) not null,
    event_type varchar(100) not null,
    payload_hash varchar(64) not null,
    processed_at datetime(6) not null,
    primary key (id),
    constraint uk_processed_provider_event unique (provider, provider_event_id)
);

create index idx_subscriptions_user_status on subscriptions (user_id, status);
create index idx_subscription_payments_user on subscription_payments (user_id, created_at);
