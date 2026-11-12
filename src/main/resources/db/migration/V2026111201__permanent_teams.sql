create table teams (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    name varchar(80) not null,
    normalized_name varchar(80) not null,
    sport varchar(50) not null,
    description varchar(500) not null,
    logo_url varchar(500),
    logo_public_id varchar(255),
    district varchar(80),
    competitive_level varchar(20) not null,
    open_to_requests bit not null,
    captain_id bigint not null,
    status varchar(20) not null,
    max_members integer not null default 30,
    primary key (id),
    constraint uk_teams_normalized_name unique (normalized_name),
    constraint fk_teams_captain foreign key (captain_id) references users (id)
);

create table team_memberships (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    team_id bigint not null,
    user_id bigint not null,
    role varchar(20) not null,
    status varchar(20) not null,
    joined_at datetime(6) not null,
    ended_at datetime(6),
    primary key (id),
    constraint uk_team_membership unique (team_id, user_id),
    constraint fk_team_membership_team foreign key (team_id) references teams (id),
    constraint fk_team_membership_user foreign key (user_id) references users (id)
);

create table team_requests (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    team_id bigint not null,
    user_id bigint not null,
    requested_by bigint not null,
    type varchar(20) not null,
    status varchar(20) not null,
    expires_at datetime(6),
    resolved_at datetime(6),
    primary key (id),
    constraint fk_team_request_team foreign key (team_id) references teams (id),
    constraint fk_team_request_user foreign key (user_id) references users (id),
    constraint fk_team_request_actor foreign key (requested_by) references users (id)
);

create table team_challenges (
    id bigint not null auto_increment,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    challenger_team_id bigint not null,
    challenged_team_id bigint not null,
    created_by bigint not null,
    sport varchar(50) not null,
    message varchar(300),
    status varchar(20) not null,
    match_id bigint,
    resolved_at datetime(6),
    primary key (id),
    constraint fk_team_challenge_challenger foreign key (challenger_team_id) references teams (id),
    constraint fk_team_challenge_challenged foreign key (challenged_team_id) references teams (id),
    constraint fk_team_challenge_creator foreign key (created_by) references users (id),
    constraint fk_team_challenge_match foreign key (match_id) references matches (id),
    constraint ck_team_challenge_distinct check (challenger_team_id <> challenged_team_id)
);

create index idx_teams_public_search on teams (status, sport, district, competitive_level, open_to_requests);
create index idx_team_memberships_user on team_memberships (user_id, status);
create index idx_team_requests_user on team_requests (user_id, status, type);
create index idx_team_requests_team on team_requests (team_id, status, type);
create index idx_team_challenges_teams on team_challenges (challenger_team_id, challenged_team_id, status);
