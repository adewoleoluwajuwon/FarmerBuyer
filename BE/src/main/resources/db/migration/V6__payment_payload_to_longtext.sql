-- V6__payment_payload_to_longtext.sql
ALTER TABLE payment
  MODIFY COLUMN raw_payload_json LONGTEXT NULL;
