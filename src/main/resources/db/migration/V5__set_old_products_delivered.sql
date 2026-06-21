UPDATE products
SET status     = 'DELIVERED',
    updated_at = now()
WHERE created_at < now() - INTERVAL '1 month'
  AND status != 'DELIVERED';