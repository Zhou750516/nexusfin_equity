create table if not exists async_compensation_task (
    task_id varchar(64) primary key,
    task_type varchar(64) not null,
    biz_key varchar(128) not null,
    biz_order_no varchar(64) not null,
    partition_no int not null,
    task_status varchar(32) not null,
    target_code varchar(32) not null,
    request_path varchar(256) not null,
    http_method varchar(16) not null,
    request_headers text,
    request_payload text not null,
    response_payload text,
    retry_count int not null,
    max_retry_count int not null,
    next_retry_ts timestamp,
    last_error_code varchar(64),
    last_error_message varchar(512),
    lease_owner varchar(64),
    lease_expire_ts timestamp,
    success_ts timestamp,
    created_ts timestamp not null,
    updated_ts timestamp not null
);

create unique index uk_task_type_biz_key on async_compensation_task(task_type, biz_key);
create index idx_partition_status_next_retry on async_compensation_task(partition_no, task_status, next_retry_ts);
create index idx_task_status_updated_ts on async_compensation_task(task_status, updated_ts);
create index idx_lease_expire_ts on async_compensation_task(lease_expire_ts);

create table if not exists async_compensation_attempt (
    attempt_id varchar(64) primary key,
    task_id varchar(64) not null,
    task_type varchar(64) not null,
    partition_no int not null,
    worker_id varchar(64) not null,
    attempt_no int not null,
    request_payload text,
    response_payload text,
    result_status varchar(32) not null,
    error_code varchar(64),
    error_message varchar(512),
    started_ts timestamp not null,
    finished_ts timestamp
);

create index idx_attempt_task_id on async_compensation_attempt(task_id);
create index idx_attempt_partition_started_ts on async_compensation_attempt(partition_no, started_ts);

create table if not exists async_compensation_partition_runtime (
    partition_no int primary key,
    worker_id varchar(64),
    worker_status varchar(32) not null,
    current_task_id varchar(64),
    current_task_started_ts timestamp,
    last_heartbeat_ts timestamp not null,
    updated_ts timestamp not null
);
