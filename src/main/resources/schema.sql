create table if not exists model_feedback_event (
    id bigint not null auto_increment primary key,
    query_text varchar(255) not null,
    sort_option varchar(50) not null,
    model_name varchar(100) not null,
    score tinyint not null,
    search_duration_millis bigint not null default 0,
    created_at timestamp not null default current_timestamp
);
