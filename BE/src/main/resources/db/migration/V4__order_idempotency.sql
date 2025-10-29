-- src/main/resources/db/migration/V4__order_idempotency.sql
ALTER TABLE `order`
  ADD COLUMN idempotency_key VARCHAR(100) NULL,
  ADD CONSTRAINT uq_order_buyer_idem UNIQUE (buyer_id, idempotency_key);
