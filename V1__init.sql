-- V1__init.sql
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- =========================================================
-- Users & Auth
-- =========================================================
USE farmer_buyer;

CREATE TABLE app_user (
  id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  phone_e164      VARCHAR(20) NOT NULL,        -- e.g. 2348012345678
  full_name       VARCHAR(120) NULL,
  role            ENUM('FARMER','BUYER','ADMIN') NOT NULL DEFAULT 'BUYER',
  whatsapp_opt_in TINYINT(1) NOT NULL DEFAULT 1,
  is_verified     TINYINT(1) NOT NULL DEFAULT 0,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at      TIMESTAMP NULL,
  CONSTRAINT uq_user_phone UNIQUE (phone_e164)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE otp_token (
  id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  phone_e164      VARCHAR(20) NOT NULL,
  code_hash       VARBINARY(64) NOT NULL,    -- store hash, not raw code
  expires_at      TIMESTAMP NOT NULL,
  attempts        INT NOT NULL DEFAULT 0,
  consumed_at     TIMESTAMP NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_otp_phone_exp (phone_e164, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Optional lightweight profiles (keep MVP simple)
CREATE TABLE farmer_profile (
  user_id         BIGINT UNSIGNED PRIMARY KEY,
  farm_name       VARCHAR(160) NULL,
  verified_at     TIMESTAMP NULL,
  FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE buyer_profile (
  user_id         BIGINT UNSIGNED PRIMARY KEY,
  company_name    VARCHAR(160) NULL,
  FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Master Data
-- =========================================================
CREATE TABLE crop (
  id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(120) NOT NULL,   -- e.g. Tomatoes
  category        VARCHAR(120) NULL,       -- e.g. Vegetables
  default_unit    ENUM('KG','BAG','CRATE','BUNCH','TON') NOT NULL DEFAULT 'KG',
  is_active       TINYINT(1) NOT NULL DEFAULT 1,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_crop_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Listings
-- =========================================================
CREATE TABLE listing (
  id                   BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  farmer_id            BIGINT UNSIGNED NOT NULL,
  crop_id              BIGINT UNSIGNED NOT NULL,
  title                VARCHAR(160) NOT NULL,
  description          TEXT NULL,
  unit                 ENUM('KG','BAG','CRATE','BUNCH','TON') NOT NULL,
  quantity_available   DECIMAL(12,3) NOT NULL,               -- current stock
  min_order_qty        DECIMAL(12,3) NOT NULL DEFAULT 1.000,
  price_per_unit_ngn   DECIMAL(12,2) NOT NULL,               -- NGN only for v1
  location_text        VARCHAR(200) NULL,                     -- e.g. "Mile 12, Lagos"
  lat                  DECIMAL(10,7) NULL,
  lng                  DECIMAL(10,7) NULL,
  status               ENUM('DRAFT','ACTIVE','SOLD_OUT','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
  featured_until       TIMESTAMP NULL,
  photos_count         INT NOT NULL DEFAULT 0,
  created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at           TIMESTAMP NULL,
  FOREIGN KEY (farmer_id) REFERENCES app_user(id),
  FOREIGN KEY (crop_id)   REFERENCES crop(id),
  INDEX idx_listing_status_crop (status, crop_id),
  INDEX idx_listing_location (location_text),
  INDEX idx_listing_farmer (farmer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE listing_photo (
  id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  listing_id  BIGINT UNSIGNED NOT NULL,
  url         VARCHAR(500) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (listing_id) REFERENCES listing(id) ON DELETE CASCADE,
  INDEX idx_photo_listing (listing_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Orders & Events
-- =========================================================
CREATE TABLE `order` (
  id                     BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  buyer_id               BIGINT UNSIGNED NOT NULL,
  farmer_id              BIGINT UNSIGNED NOT NULL,
  listing_id             BIGINT UNSIGNED NOT NULL,
  quantity_ordered       DECIMAL(12,3) NOT NULL,
  unit_price_snapshot    DECIMAL(12,2) NOT NULL,   -- copy from listing at order time
  subtotal_ngn           DECIMAL(12,2) NOT NULL,   -- qty * unit_price_snapshot
  platform_fee_ngn       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total_ngn              DECIMAL(12,2) NOT NULL,   -- subtotal + fee
  payment_method         ENUM('COD','PAYSTACK','FLUTTERWAVE') NOT NULL DEFAULT 'COD',
  payment_status         ENUM('UNPAID','PAID','REFUNDED') NOT NULL DEFAULT 'UNPAID',
  order_status           ENUM('PENDING','CONFIRMED','CANCELLED','FULFILLED') NOT NULL DEFAULT 'PENDING',
  delivery_requested     TINYINT(1) NOT NULL DEFAULT 0,
  pickup_location_text   VARCHAR(200) NULL,
  created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at             TIMESTAMP NULL,
  FOREIGN KEY (buyer_id)   REFERENCES app_user(id),
  FOREIGN KEY (farmer_id)  REFERENCES app_user(id),
  FOREIGN KEY (listing_id) REFERENCES listing(id),
  INDEX idx_order_buyer_status (buyer_id, order_status),
  INDEX idx_order_farmer_status (farmer_id, order_status),
  INDEX idx_order_payment (payment_status, payment_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_event (
  id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id     BIGINT UNSIGNED NOT NULL,
  event_type   VARCHAR(60) NOT NULL,        -- e.g. ORDER_PLACED, ORDER_CONFIRMED
  note         VARCHAR(500) NULL,
  metadata_json JSON NULL,                  -- MySQL 8 JSON
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES `order`(id) ON DELETE CASCADE,
  INDEX idx_order_event (order_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Payments
-- =========================================================
CREATE TABLE payment (
  id               BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id         BIGINT UNSIGNED NOT NULL,
  provider         ENUM('PAYSTACK','FLUTTERWAVE') NOT NULL,
  provider_ref     VARCHAR(120) NOT NULL,     -- reference from gateway
  amount_ngn       DECIMAL(12,2) NOT NULL,
  status           ENUM('INITIATED','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'INITIATED',
  paid_at          TIMESTAMP NULL,
  raw_payload_json JSON NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES `order`(id),
  CONSTRAINT uq_payment_provider_ref UNIQUE (provider, provider_ref),
  INDEX idx_payment_order (order_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Trust & Safety (MVP-light)
-- =========================================================
CREATE TABLE rating (
  id           BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id     BIGINT UNSIGNED NOT NULL,
  rater_id     BIGINT UNSIGNED NOT NULL,     -- who gave rating
  ratee_id     BIGINT UNSIGNED NOT NULL,     -- who received rating
  stars        TINYINT UNSIGNED NOT NULL,    -- 1..5
  comment      VARCHAR(500) NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES `order`(id),
  FOREIGN KEY (rater_id) REFERENCES app_user(id),
  FOREIGN KEY (ratee_id) REFERENCES app_user(id),
  INDEX idx_rating_ratee (ratee_id),
  CONSTRAINT chk_stars CHECK (stars BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dispute (
  id             BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id       BIGINT UNSIGNED NOT NULL,
  opened_by_id   BIGINT UNSIGNED NOT NULL,
  reason         VARCHAR(200) NOT NULL,
  status         ENUM('OPEN','IN_REVIEW','RESOLVED','REJECTED') NOT NULL DEFAULT 'OPEN',
  resolution     VARCHAR(500) NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at      TIMESTAMP NULL,
  FOREIGN KEY (order_id)     REFERENCES `order`(id),
  FOREIGN KEY (opened_by_id) REFERENCES app_user(id),
  INDEX idx_dispute_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Notifications (for WhatsApp/SMS/Email later)
-- =========================================================
CREATE TABLE notification (
  id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT UNSIGNED NOT NULL,
  channel         ENUM('WHATSAPP','SMS','EMAIL') NOT NULL,
  template_key    VARCHAR(120) NOT NULL,
  payload_json    JSON NULL,
  status          ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING',
  sent_at         TIMESTAMP NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES app_user(id),
  INDEX idx_notification_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================================================
-- Admin Audit (simple)
-- =========================================================
CREATE TABLE admin_action_log (
  id            BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  actor_id      BIGINT UNSIGNED NOT NULL,
  action        VARCHAR(120) NOT NULL,         -- e.g. 'UPDATE_LISTING'
  target_type   VARCHAR(80) NOT NULL,          -- e.g. 'listing'
  target_id     BIGINT UNSIGNED NOT NULL,
  before_json   JSON NULL,
  after_json    JSON NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (actor_id) REFERENCES app_user(id),
  INDEX idx_admin_log_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
