create table if not exists member_info (
    member_id varchar(64) primary key,
    tech_platform_user_id varchar(64) unique,
    external_user_id varchar(64) not null,
    mobile_encrypted varchar(512) not null,
    mobile_hash varchar(128) not null,
    id_card_encrypted varchar(512) not null,
    id_card_hash varchar(128) not null,
    real_name_encrypted varchar(512) not null,
    member_status varchar(32) not null,
    created_ts timestamp not null,
    updated_ts timestamp not null
);

create table if not exists member_channel (
    id bigint auto_increment primary key,
    member_id varchar(64) not null,
    channel_code varchar(64) not null,
    external_user_id varchar(64) not null,
    bind_status varchar(32) not null,
    created_ts timestamp not null,
    updated_ts timestamp not null,
    unique key uk_channel_external_user (channel_code, external_user_id)
);

create table if not exists benefit_product (
    product_code varchar(64) primary key,
    product_name varchar(128) not null,
    fee_rate int not null,
    status varchar(32) not null,
    created_ts timestamp not null,
    updated_ts timestamp not null
);

create table if not exists benefit_order (
    benefit_order_no varchar(64) primary key,
    member_id varchar(64) not null,
    source_channel_code varchar(64) not null,
    external_user_id varchar(64) not null,
    product_code varchar(64) not null,
    agreement_no varchar(64),
    loan_amount bigint not null,
    order_status varchar(32) not null,
    first_deduct_status varchar(32) not null,
    fallback_deduct_status varchar(32) not null,
    exercise_status varchar(32) not null,
    refund_status varchar(32) not null,
    grant_status varchar(32) not null,
    loan_order_no varchar(64),
    sync_status varchar(32) not null,
    request_id varchar(64) not null,
    created_ts timestamp not null,
    updated_ts timestamp not null
);

create table if not exists payment_record (
    payment_no varchar(64) primary key,
    benefit_order_no varchar(64) not null,
    payment_type varchar(32) not null,
    provider_code varchar(64) not null,
    channel_trade_no varchar(64),
    amount bigint not null,
    payment_status varchar(32) not null,
    fail_reason varchar(256),
    request_id varchar(64),
    created_ts timestamp not null,
    updated_ts timestamp not null
);

create table if not exists sign_task (
    task_no varchar(64) primary key,
    benefit_order_no varchar(64) not null,
    contract_type varchar(64) not null,
    sign_url varchar(512) not null,
    sign_status varchar(32) not null,
    created_ts timestamp not null,
    updated_ts timestamp not null
);

create table if not exists contract_archive (
    contract_no varchar(64) primary key,
    task_no varchar(64) not null,
    contract_type varchar(64) not null,
    file_url varchar(512) not null,
    file_hash varchar(128) not null,
    created_ts timestamp not null
);

create table if not exists notification_receive_log (
    notify_no varchar(64) primary key,
    benefit_order_no varchar(64) not null,
    notify_type varchar(64) not null,
    request_id varchar(64) not null,
    process_status varchar(32) not null,
    payload text,
    retry_count int not null,
    received_ts timestamp not null,
    processed_ts timestamp
);

create table if not exists idempotency_record (
    request_id varchar(64) primary key,
    biz_type varchar(64) not null,
    biz_key varchar(128) not null,
    response_body text,
    processed_ts timestamp not null
);
