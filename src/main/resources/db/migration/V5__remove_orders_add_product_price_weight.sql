-- ====================================================
-- Remove orders domain, add price/weight to products
-- ====================================================

-- Drop FK and order_id from products first
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_order_id_fkey;
ALTER TABLE products DROP COLUMN IF EXISTS order_id;

-- Drop order-related indexes
DROP INDEX IF EXISTS idx_products_order_id;
DROP INDEX IF EXISTS idx_orders_user_id;
DROP INDEX IF EXISTS idx_orders_branch_id;

-- Drop orders table (CASCADE covers any remaining FKs)
DROP TABLE IF EXISTS orders CASCADE;

-- Add price and weight to products (optional)
ALTER TABLE products ADD COLUMN IF NOT EXISTS price  NUMERIC(12, 2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS weight NUMERIC(12, 3);
