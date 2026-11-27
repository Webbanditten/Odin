-- Both catalogues side by side: the v31 tree (hh_cat_new) gets its own order among siblings, so arranging it never
-- moves a page in the old catalogue's flat index (which goes by order_id). 0 = not placed yet: after the placed pages
-- of its folder, in order_id order. Safe with an older Odin running (it ignores the column).
ALTER TABLE catalogue_pages ADD COLUMN tree_order INT NOT NULL DEFAULT 0 AFTER parent_id;
-- Rollback: ALTER TABLE catalogue_pages DROP COLUMN tree_order;
