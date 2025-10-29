CREATE TABLE IF NOT EXISTS otp_token (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  phone_e164 VARCHAR(20) NOT NULL,
  code_hash VARBINARY(64) NOT NULL,  -- SHA-256(phone + code + secret)
  expires_at TIMESTAMP NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  consumed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_otp_phone_exp (phone_e164, expires_at)
);
