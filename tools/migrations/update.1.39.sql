-- Catalogue products are now shown in order_id order (then id). Until now Odin loaded them without ORDER BY, so the
-- game showed every page in id order and order_id did nothing. Setting order_id = id keeps every page exactly as
-- players see it today; the housekeeping catalogue editor reorders a page by swapping these values.
UPDATE catalogue_items SET order_id = id;
