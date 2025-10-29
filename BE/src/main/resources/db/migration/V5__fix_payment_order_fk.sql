-- Ensure engines are InnoDB
ALTER TABLE `order`  ENGINE=InnoDB;
ALTER TABLE `payment` ENGINE=InnoDB;

-- Align column types
ALTER TABLE `payment`
  MODIFY COLUMN order_id BIGINT UNSIGNED NOT NULL;

-- Add index explicitly
CREATE INDEX idx_payment_order_id ON `payment` (order_id);

-- Drop FK if exists (MySQL-compatible)
SET @fk_name := (
  SELECT CONSTRAINT_NAME
  FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'payment'
    AND COLUMN_NAME = 'order_id'
    AND REFERENCED_TABLE_NAME = 'order'
  LIMIT 1
);

SET @sql = IF(@fk_name IS NOT NULL,
              CONCAT('ALTER TABLE `payment` DROP FOREIGN KEY ', @fk_name),
              'SELECT 1');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add the proper FK
ALTER TABLE `payment`
  ADD CONSTRAINT fk_payment_order
  FOREIGN KEY (order_id) REFERENCES `order` (id)
  ON UPDATE CASCADE
  ON DELETE RESTRICT;
