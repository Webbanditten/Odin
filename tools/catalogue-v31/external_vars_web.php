<?php
// Variables for the web client (DirPlayer): exactly external_vars.php, except
// - private rooms load hh_room_private_wc (hh_room_private with hh_window_class's two scripts moved in, built with
//   habbo-vibe-playground's `npm run move-scripts`) instead of hh_room_private + hh_window_class: one cast slot fewer;
// - hh_registrat is hh_registrat_sso: one bytecode operand changed so the in-game figure editor saves with an SSO
//   login (its "Password was reseted, abort update!" guard read the session password, which SSO never sets).
// - the v31 catalogue (Habbo r31's hh_cat_new + its hh_cat_gfx_all, as hh_cat_gfx_v31, and its dynamic downloader with
//   the furnidata container, as hh_dynamic_downloader_v31), with furnidata/productdata from the CMS and pixels off.
//   Odin answers each client in its catalogue's format. See prod-staging/catalogue-v31/README.md.
// The Shockwave client keeps using external_vars.php.
ob_start();
include dirname(__FILE__) . '/external_vars.php';
$vars = ob_get_clean();
echo str_replace(
    array(
        'room.cast.private=["hh_room_private", "hh_room_landscapes", "hh_window_class"]',
        "=hh_registrat\n",
        "=hh_registrat\r\n",
        "=hh_cat_gfx_all\n",
        "=hh_cat_gfx_all\r\n",
        "=hh_cat_code\n",
        "=hh_cat_code\r\n",
        "=hh_dynamic_downloader\n",
        "=hh_dynamic_downloader\r\n",
    ),
    array(
        'room.cast.private=["hh_room_private_wc", "hh_room_landscapes"]',
        "=hh_registrat_sso\n",
        "=hh_registrat_sso\r\n",
        "=hh_cat_gfx_v31\n",
        "=hh_cat_gfx_v31\r\n",
        "=hh_cat_new\n",
        "=hh_cat_new\r\n",
        "=hh_dynamic_downloader_v31\n",
        "=hh_dynamic_downloader_v31\r\n",
    ),
    $vars
);
// %hash% is a per-session value the client fills in (cache busting).
echo "\nproductdata.load.url=https://habbo-danmark.com/gamedata/productdata?h=%hash%\n";
echo "furnidata.load.url=https://habbo-danmark.com/gamedata/furnidata?h=%hash%\n";
echo "pixels.enabled=false\n";
