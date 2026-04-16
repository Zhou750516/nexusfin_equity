DELETE FROM notification_receive_log;
DELETE FROM payment_record;
DELETE FROM contract_archive;
DELETE FROM sign_task;
DELETE FROM member_payment_protocol;
DELETE FROM benefit_order;
DELETE FROM member_channel;
DELETE FROM member_info;
DELETE FROM idempotency_record;
DELETE FROM benefit_product;

INSERT INTO benefit_product (
    product_code,
    product_name,
    fee_rate,
    status,
    created_ts,
    updated_ts
) VALUES (
    'QS-PROD-001',
    'Quickstart权益产品',
    299,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO member_info (
    member_id,
    tech_platform_user_id,
    external_user_id,
    mobile_encrypted,
    mobile_hash,
    id_card_encrypted,
    id_card_hash,
    real_name_encrypted,
    member_status,
    created_ts,
    updated_ts
) VALUES (
    'mem-auth-seed-001',
    'tech-user-seed-001',
    'tech-user-seed-001',
    'MTM4MDAwMDAwMDE=',
    'seed-mobile-hash-001',
    'c2VlZC1pZC0wMDE=',
    'seed-id-hash-001',
    '5byg5LiJ',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO member_channel (
    member_id,
    channel_code,
    external_user_id,
    bind_status,
    created_ts,
    updated_ts
) VALUES (
    'mem-auth-seed-001',
    'KJ',
    'tech-user-seed-001',
    'BOUND',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
