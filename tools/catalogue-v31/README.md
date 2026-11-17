# The v31 catalogue (parked)

Habbo r31's catalogue (`hh_cat_new`): a tree index with coloured nodes, icons and folders, a 3x3 product strip, and
the old special pages (Plastic, spaces, trophies, pets, recycler, Trax, purse) wrapped. This branch lets Odin serve it
next to the old catalogue, so the web client can run it while Shockwave keeps the old one.

It was live in the web client on 2026-11-17 from about 13:45 to 14:10 (Odin v1.25.12) and rolled back at Patrick's
request. `master` has the revert (#48, released as v1.25.13 = v1.25.11's code). The work is kept here:

| Repo | Branch | What |
|---|---|---|
| Odin | `feature/catalogue-v31` | This folder; the protocol; `tools/migrations/update.1.40.sql` |
| OdinCMS | `feature/catalogue-v31` | `/gamedata/furnidata` + `/gamedata/productdata`, the editor's tree fields, an unfinished v31 preview (`tools/catalogue/V31.md`) |
| habbo-vibe-playground (Forgejo) | `feat/v31-catalogue-tools` | Cast tools: `renameNames`, `addScript`, `replaceText`, `replaceBitmap`, `dumpCast` |

## How it works

**Client side, no recompiling.** The web client loads r31's `hh_cat_new` (logic), r31's `hh_cat_gfx_all` (renamed
`hh_cat_gfx_v31`: windows, tree and strip graphics) and r31's `hh_dynamic_downloader` (renamed
`hh_dynamic_downloader_v31`: ours plus the persistent furnidata container). Our `fuse_client` is older than r31, so:

- `hh_cat_new`: three names renamed in its name table (`activateWindowObj` → `activateWindow`, `getSessionHash` →
  `getUniqueID`, `setHeaderMemberName` → `setHeaderImage`), plus `compat.ls` compiled in as the movie script
  "Compat API" (`getStringVariable`, `decodeUTF8`, which our client lacks).
- `hh_dynamic_downloader_v31`: `getSessionHash` → `getUniqueID`, `decodeUTF8` → `convertSpecialChars`.
- `hh_cat_gfx_v31`: the pets page's name box is ` ctlg_price_box2` (the coin price box drew coins and broken
  corners there); the tree icons `katalogi_ikoni.purse`/`.pets` re-encoded from JPEG with a pure white background
  (the off-white JPEG background showed as white corners under ink 36); `purse_coins_small` and ` ctlg_price_box`
  use the old catalogue's yellow coins (from `hh_cat_code`) instead of r31's pale ones.
- Variables (`external_vars_web.php` here = the web client's wrapper with v31 switched on): `room.cast.3/4` =
  `hh_cat_gfx_v31`/`hh_cat_new`, `cast.entry.22` = `hh_dynamic_downloader_v31`,
  `productdata.load.url`/`furnidata.load.url` = the CMS's `/gamedata/productdata?h=%hash%` / `furnidata`,
  `pixels.enabled=false`.

**Odin (this branch).** Same message headers as the old catalogue; Odin tells the two apart per player:

- GCIX (101) without a body = v31, `production/<language>` = old (an empty request only refreshes the old one).
  `Player` remembers the catalogue type; reloads go only to players whose type is known.
- v31 index (126, `CATALOGUE_INDEX_V31`): a tree; each node visible (always 1: it means "draw it"), colour, icon,
  page id (-1 for a folder), name, disabled, children. A page whose parent the player can't see goes to the top.
- v31 page (127, `CATALOGUE_PAGE_V31`): layout, images[], texts[] mapped from our page fields per v31 layout (the
  `layout.fields.*` variables of hh_cat_new, see `CataloguePage.getV31Texts/getV31Images`); product layouts become
  `default_3x3`; `layout_v31` overrides. Offers: item id, sale code (productdata key), credits, 0 pixels, products
  (type, definition id = furnidata class id, extra, count, -1).
- GRPC (100) parses both formats into one purchase path; `NO_CREDITS` sends credits and pixels to v31.
- Page images from the image library go to v31 as `name_<language>` (`catalogue_library_images`, filled by the
  CMS's Publish; setting `catalogue.v31.image.language`, default `da`). The old client adds the language itself.
- `update.1.40.sql`: `catalogue_pages.parent_id`, `icon`, `colour`, `layout_v31`, table `catalogue_library_images`.
  Pages without a layout are folders: only the v31 tree shows them. **These columns are still on prod**; master's
  code ignores them.

**Page tree** (`tree.py` → `tree.sql`, modelled on Havana's catalogue): Forside, Start pakker, Sjælden ting
(Samleobjekt), a Møbler folder with every collection in Havana's order plus ours, Kæledyr, Trax with its genres,
Habbo Exchange, Club Shop, Ecotron, Gaver, a Staff folder. `python3 tree.py check` (with `SELECT id, name FROM
catalogue_pages` on stdin) checks ids and names first. `tree_rollback.sql` restores prod's order from before
(2026-11-17 13:34 backup) and removes the two folders; it was applied at the rollback.

## Deploying it again

1. DB backup. `update.1.40.sql` is already on prod (skip it there).
2. Odin with this branch (merge it, or revert #48), release, deploy. Then `tree.sql` (after `tree.py check`), then
   fill `catalogue_library_images` (the CMS's Publish does it), then restart Odin or Publish so it loads them.
3. CMS with `feature/catalogue-v31` (revert OdinCMS #84 or merge the branch).
4. Upload the three casts to `dcr/files/dcr/latest/` and swap `gamedata/external_vars_web.php` for this folder's
   (keep a backup). On prod both are still there: the casts in `dcr/latest/` (`hh_cat_new.cct` sha1 `c6114aee`,
   `hh_cat_gfx_v31.cct` `3a5efa25`, `hh_dynamic_downloader_v31.cct` `4b212ff9`) and the v31 wrapper as
   `gamedata/external_vars_web.php.v31-2026-11-17`. Rollback images: `kepler:v1.25.12-v31-2026-11-17`,
   `keplercms:v31-2026-11-17`.

Rollback = restore the wrapper backup (the web client gets the old catalogue at its next load), `tree_rollback.sql`,
then the reverted Odin and CMS.

## Rebuilding the casts

Source: r31's `dcr/v31/` from Patrick's game file archive. With habbo-vibe-playground's cast editor
(`tools/director-cast-editor`, `npm run compile`):

```bash
node out/cli/renameNames.js v31/hh_cat_new.cct /tmp/cn.cct activateWindowObj=activateWindow getSessionHash=getUniqueID setHeaderMemberName=setHeaderImage
node out/cli/addScript.js /tmp/cn.cct compat.ls "Compat API" v31/fuse_client.cct "Variable API" hh_cat_new.cct
node out/cli/renameNames.js v31/hh_dynamic_downloader.cct hh_dynamic_downloader_v31.cct getSessionHash=getUniqueID decodeUTF8=convertSpecialChars
node out/cli/replaceText.js v31/hh_cat_gfx_all.cct /tmp/g1.cct ctlg_pets.window \
  '[#member: " ctlg_price_box", #media: #bitmap, #locH: 96, #locV: 302, #ink: 8, #blend: 100, #width: 159, #height: 34,' \
  '[#member: " ctlg_price_box2", #media: #bitmap, #locH: 81, #locV: 305, #ink: 8, #blend: 100, #width: 189, #height: 28,'
node out/cli/replaceBitmap.js /tmp/g1.cct /tmp/g2.cct katalogi_ikoni.purse purse.png
node out/cli/replaceBitmap.js /tmp/g2.cct /tmp/g3.cct katalogi_ikoni.pets pets.png
node out/cli/replaceBitmap.js /tmp/g3.cct /tmp/g4.cct purse_coins_small coins.png
node out/cli/replaceBitmap.js /tmp/g4.cct hh_cat_gfx_v31.cct " ctlg_price_box" price_box.png
```

The four PNGs (and the built casts) are in the local workspace's `prod-staging/catalogue-v31/` (not in git: they are
the game's graphics). The icons are DirPlayer's decode of r31's JPEGs with the background (all channels ≥ 0xDC) made
transparent; the coins are `hh_cat_code`'s `purse_coins_small`; the price box has r31's coins replaced by those.

## Verified (2026-11-17, locally with DirPlayer, and on prod)

Tree with colours, icons and folders; default_3x3 pages; Plastic, spaces, Trax, pets, camera, gifts, trophies,
exchange, Ecotron; buying (credits, hand, purchase log); the old catalogue against the same Odin (index without
folders, pages, buying). A page-by-page screenshot comparison old vs v31 was started but not finished.
