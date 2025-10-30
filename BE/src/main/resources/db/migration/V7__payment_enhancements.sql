-- Adding new optional metadata I want to persist from the PSP (payment service provider)
-- I keep existing provider/provider_ref/unique index/raw_payload_json as-is.

ALTER TABLE payment ADD COLUMN channel VARCHAR(64) NULL;
ALTER TABLE payment ADD COLUMN currency VARCHAR(8) NULL;
ALTER TABLE payment ADD COLUMN gateway_fee DECIMAL(12,2) NULL;

-- NOTE:
-- - I intentionally do NOT add `reference` because I already use `provider_ref`.
-- - I intentionally do NOT add `raw_payload` JSON because I already have `raw_payload_json` (LONGTEXT via V6).
-- - The unique index on (provider, provider_ref) already exists; I will not recreate it.
