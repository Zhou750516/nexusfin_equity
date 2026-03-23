DELETE FROM notification_receive_log;
DELETE FROM payment_record;
DELETE FROM contract_archive;
DELETE FROM sign_task;
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
