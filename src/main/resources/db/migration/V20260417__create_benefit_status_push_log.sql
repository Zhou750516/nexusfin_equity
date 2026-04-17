create table if not exists benefit_status_push_log (
    event_id varchar(64) primary key,
    benefit_order_no varchar(64) not null,
    event_type varchar(64) not null,
    status_before varchar(64),
    status_after varchar(64),
    push_status varchar(32) not null,
    retry_count int not null,
    request_payload text,
    response_payload text,
    error_message varchar(512),
    created_ts timestamp not null,
    updated_ts timestamp not null
);
