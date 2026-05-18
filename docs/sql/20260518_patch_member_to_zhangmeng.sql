/*
  Temporary joint-debug patch SQL.

  Purpose:
  - Patch the current local ABS member bound by the usable joint-entry token to Zhang Meng test data.
  - This does not prove the upstream Xiaohua/Yunka token belongs to Zhang Meng.
  - Do not trigger joint-entry again after running this SQL, otherwise upstream old user data may overwrite member_info.

  Important crypto prerequisite:
  - The encrypted values below were generated with the project default DEK:
    NEXUSFIN_CRYPTO_DEK_KEY_ID=DEK_USER_PROFILE
    NEXUSFIN_CRYPTO_DEK_KEY_VERSION=1
    NEXUSFIN_CRYPTO_DEK_PLAINTEXT_BASE64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
  - Run this file only if Aliyun backend has NOT overridden NEXUSFIN_CRYPTO_DEK_PLAINTEXT_BASE64.
  - If Aliyun uses a different DEK, stop and regenerate mobile_encrypted/id_card_encrypted/real_name_encrypted with that DEK.

  Do not paste full JWT, Qiwei keys, or screenshots containing full sensitive values into tickets/logs.
*/

START TRANSACTION;

SET @member_id := 'mem484e1914f6cf411ebff413f812ba40a9';
SET @cid := '20250804000000359032';
SET @channel_code := 'KJ';

SET @mobile_encrypted := 'DEK_USER_PROFILE:1:pb79gmurbVqj2blWutizAXK1HSsSHrtZS+jkYrTvVOlzF+SeUakT';
SET @id_card_encrypted := 'DEK_USER_PROFILE:1:943pA+6vUyB9Rlpq5KQj4H1Ak3GzvYMewQy8jXTU7h2WxojO4BCjNZ25ZwfQPw==';
SET @real_name_encrypted := 'DEK_USER_PROFILE:1:6yU7zOvg+0RGgKkh7Gc5I/Z7UIxeGoyQfNV62bAyC6BjeQ==';
SET @mobile_hash := '05fd3f303c29d17c7d0e578f97d83738f83418848436245def87cec22b47840f';
SET @id_card_hash := '12642394bb97f0616b184bf8e508a8c0b07aed0bdee473e98ae18be0c1aa70eb';

-- Patch local member profile. Keep member_id and cid binding unchanged.
UPDATE member_info
SET
    tech_platform_user_id = @cid,
    external_user_id = @cid,
    mobile_encrypted = @mobile_encrypted,
    mobile_hash = @mobile_hash,
    id_card_encrypted = @id_card_encrypted,
    id_card_hash = @id_card_hash,
    real_name_encrypted = @real_name_encrypted,
    member_status = 'ACTIVE',
    updated_ts = NOW()
WHERE member_id = @member_id;

-- Keep current joint-entry channel binding usable.
INSERT INTO member_channel (
    member_id,
    channel_code,
    external_user_id,
    bind_status,
    created_ts,
    updated_ts
)
VALUES (
    @member_id,
    @channel_code,
    @cid,
    'ACTIVE',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    member_id = VALUES(member_id),
    bind_status = 'ACTIVE',
    updated_ts = NOW();

-- Patch local receiving account cache to Zhang Meng's test card.
UPDATE member_receiving_account
SET
    is_default = 0,
    updated_ts = NOW()
WHERE member_id = @member_id
  AND account_id <> '622908328976881119';

INSERT INTO member_receiving_account (
    member_id,
    account_id,
    bank_name,
    last_four,
    account_status,
    is_default,
    source,
    source_index,
    created_ts,
    updated_ts
)
VALUES (
    @member_id,
    '622908328976881119',
    '齐为联调测试卡',
    '8119',
    'ACTIVE',
    1,
    'manual',
    0,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    bank_name = VALUES(bank_name),
    last_four = VALUES(last_four),
    account_status = 'ACTIVE',
    is_default = 1,
    source = VALUES(source),
    source_index = VALUES(source_index),
    updated_ts = NOW();

-- Ensure Qiwei test product exists.
INSERT INTO benefit_product (
    product_code,
    product_name,
    fee_rate,
    status,
    created_ts,
    updated_ts
)
VALUES (
    'abs001',
    '艾博生月卡',
    300,
    'ACTIVE',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    fee_rate = VALUES(fee_rate),
    status = 'ACTIVE',
    updated_ts = NOW();

-- Do not create fake QW_SIGN. Only normalize an existing active QW_SIGN if one already exists.
UPDATE member_payment_protocol
SET
    member_id = @member_id,
    external_user_id = @cid,
    channel_code = @channel_code,
    last_verified_ts = COALESCE(last_verified_ts, NOW()),
    updated_ts = NOW()
WHERE provider_code = 'QW_SIGN'
  AND protocol_status = 'ACTIVE'
  AND (member_id = @member_id OR external_user_id = @cid);

-- Minimal post-check. If usable_active_qw_sign_count is 0, the benefit flow may still fail with QW_SIGN_REQUIRED.
SELECT
    member_id,
    tech_platform_user_id,
    external_user_id,
    mobile_hash,
    id_card_hash,
    member_status
FROM member_info
WHERE member_id = @member_id;

SELECT
    member_id,
    channel_code,
    external_user_id,
    bind_status
FROM member_channel
WHERE channel_code = @channel_code
  AND external_user_id = @cid;

SELECT
    member_id,
    account_id,
    bank_name,
    last_four,
    account_status,
    is_default,
    source,
    source_index
FROM member_receiving_account
WHERE member_id = @member_id
ORDER BY is_default DESC, updated_ts DESC;

SELECT
    product_code,
    product_name,
    fee_rate,
    status
FROM benefit_product
WHERE product_code = 'abs001';

SELECT
    COUNT(*) AS usable_active_qw_sign_count
FROM member_payment_protocol
WHERE member_id = @member_id
  AND external_user_id = @cid
  AND provider_code = 'QW_SIGN'
  AND protocol_status = 'ACTIVE'
  AND protocol_no IS NOT NULL
  AND protocol_no <> ''
  AND sign_request_no IS NOT NULL
  AND sign_request_no <> '';

COMMIT;
