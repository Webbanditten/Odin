-- The v31 catalogue (hh_cat_new) shows the index as a tree: each page can sit under a parent page and has a colour
-- and an icon (treeview.node.bg.<colour> / treeview.node.icon.<icon> in hh_cat_new's variables). A page without a
-- layout is a folder. layout_v31 overrides the layout the v31 catalogue uses (default: the old layout without
-- "ctlg_", with layout1/layout2 shown as default_3x3). The old catalogue ignores all four.
ALTER TABLE catalogue_pages
    ADD COLUMN parent_id INT NOT NULL DEFAULT -1 AFTER order_id,
    ADD COLUMN icon INT NOT NULL DEFAULT 0 AFTER parent_id,
    ADD COLUMN colour INT NOT NULL DEFAULT 0 AFTER icon,
    ADD COLUMN layout_v31 VARCHAR(255) NULL DEFAULT NULL AFTER layout;

-- Page images that come from the image library (c_images/catalogue/<name>_<language>.gif) rather than a cast. The v31
-- catalogue downloads "<name>.gif", so Odin sends these names with "_<language>" appended (setting
-- catalogue.v31.image.language, default "da"). The CMS keeps this list current when the catalogue is published.
CREATE TABLE IF NOT EXISTS catalogue_library_images (
    name VARCHAR(255) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
