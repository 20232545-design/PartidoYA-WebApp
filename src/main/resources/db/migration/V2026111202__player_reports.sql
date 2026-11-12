-- Reportes de jugadores enviados desde el detalle de un partido y revisados por administradores.
create table if not exists player_reports (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    match_id bigint not null,
    reporter_id bigint not null,
    reported_player_id bigint not null,
    reason varchar(40) not null,
    details varchar(1000),
    status varchar(20) not null,
    submitted_at datetime(6) not null,
    reviewed_at datetime(6),
    reviewed_by bigint,
    resolution_note varchar(500),
    primary key (id),
    -- Un jugador solo puede reportar una vez a otro jugador por partido.
    constraint uk_player_reports_match_reporter_reported unique (match_id, reporter_id, reported_player_id),
    constraint fk_player_reports_match foreign key (match_id) references matches (id),
    constraint fk_player_reports_reporter foreign key (reporter_id) references users (id),
    constraint fk_player_reports_reported foreign key (reported_player_id) references users (id),
    constraint fk_player_reports_reviewer foreign key (reviewed_by) references users (id)
);

create index idx_player_reports_status_submitted on player_reports (status, submitted_at);
create index idx_player_reports_reported on player_reports (reported_player_id);
