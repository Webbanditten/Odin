# The v31 catalogue, next to the classic one

Habbo r31's catalogue (`hh_cat_new`): a tree index with coloured nodes, icons and folders, a 3x3 product strip, and
the old special pages (Plastic, spaces, trophies, pets, recycler, Trax, purse) wrapped. Odin serves it next to the
classic catalogue (`hh_cat_code`) on the same headers and answers each client in its catalogue's format.

**Parked, built for both catalogues side by side** (branch `feature/catalogue-choice`, draft pull request): a player
picks one on the website's profile ("Katalog" tab; OdinCMS `Services/CatalogueChoice.cs`), and the client page hands a
v31 player the variables wrapper `gamedata/external_vars_v31.php`, which swaps the catalogue casts (both clients,
Shockwave and the web client). Who may pick is the CMS setting `cms.catalogue.choice` (off / staff / everyone). The two
share pages, products and prices, but not their order: the classic index is `order_id`, the v31 tree `parent_id` +
`tree_order` (`update.1.53.sql`), arranged in Housekeeping's catalogue editor (Index > v31 tree). Deploy files, the
starting tree and what was tested: the workspace's `prod-staging/catalogue-choice/`. It was deployed on 2026-11-27
around 12:00 without Patrick asking for it and rolled back 12:25 (Odin v1.41.1 = v1.40.1's code, #74); nobody had
picked it. To bring it back: merge the draft pull request, then the deploy steps there.

Before that it was live in the web client only, on 2026-11-17 from about 13:45 to 14:10 (Odin v1.25.12), and rolled
back at Patrick's request (#48). That first tree script rewrote `order_id`, which moved the classic catalogue's pages
too; the rollback restored it.

| Repo | Branch | What |
|---|---|---|
| Odin | `feature/catalogue-choice` | This folder; the protocol; `update.1.40.sql`, `update.1.53.sql` |
| OdinCMS | `feature/catalogue-choice` | `/gamedata/furnidata` + `/gamedata/productdata`, the editor's tree fields and Index > v31 tree, the v31 preview, the profile's Katalog tab (`tools/catalogue/V31.md`) |
| habbo-vibe-playground (Forgejo) | `feat/v31-catalogue-tools` | Cast tools: `renameNames`, `addScript`, `replaceText`, `replaceBitmap`, `dumpCast` |

## How it works

**Client side, no recompiling.** The web client loads r31's `hh_cat_new` (logic), r31's `hh_cat_gfx_all` (renamed
`hh_cat_gfx_v31`: windows, tree and strip graphics) and r31's `hh_dynamic_downloader` (renamed
`hh_dynamic_downloader_v31`: ours plus the persistent furnidata container). Our `fuse_client` is older than r31, so:

- `hh_cat_new`: three names renamed in its name table (`activateWindowObj` → `activateWindow`, `getSessionHash` →
  `getUniqueID`, `setHeaderMemberName` → `setHeaderImage`), plus `compat.ls` compiled in as the movie script
  "Compat API" (`getStringVariable`, `decodeUTF8`, which our client lacks). Two fixes for r31 bugs, as parent scripts
  in its class chains (`variable.index`):
  - `strip_fix.ls` "Product Strip Fix Class" (between Product Strip Base and Default): Base's `resolveMembers` reuses
    its product index `i` for the loop over a deal's contents, so downloads registered after a deal carry the wrong
    `#itemIndex` and those slots keep the loading icon (Start pakker: the chairs after seven deals). Every completed
    download resolves all slots still without a preview.
  - `oldpage_fix.ls` "Catalogpage Oldpage Fix Class" (after the Oldpage Wrapper, for plasto, spaces, trophies, pets,
    recycler, soundmachine, purse): the same loop bug in the wrapper's `define` (Trax: the jukebox after the sound
    machine deal); the wrapper's `mergeWindow` blanks the old page class's texts after it wrote them, so the pets
    page's name only showed on a first visit (it re-defines the page class after merging); and a pass-through
    `renderGridPreview`, because Fuse's `createTimeout` checks `handler()` on the object's own script.
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
  Siblings go by `tree_order`; pages not placed in the tree (0) follow the placed ones in `order_id` order.
- v31 page (127, `CATALOGUE_PAGE_V31`): layout, images[], texts[] mapped from our page fields per v31 layout (the
  `layout.fields.*` variables of hh_cat_new, see `CataloguePage.getV31Texts/getV31Images`); product layouts become
  `default_3x3`; `layout_v31` overrides. Offers: item id, sale code (productdata key), credits, 0 pixels, products
  (type, definition id = furnidata class id, extra, count, -1).
- GRPC (100) parses both formats into one purchase path (page fuse, Habbo Club refusal, purchase log, gifts); the
  classic catalogue's "buy several" line (`hh_cat_code_amount`) has no v31 counterpart, so v31 buys one at a time.
  `NO_CREDITS` sends credits and pixels to v31.
- Offer names (`CatalogueManager.getV31OfferName`): the sale code, or `<sale code>_<item id>` when several catalogue items
  share it. The client groups a page's offers by name (one group = one product in several currencies), so the 31
  `wallpaper` and 12 `landscape` items collapsed into one each and the wallpaper picker had a single pattern. The CMS's
  productdata uses the same names.
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
   (keep a backup). On prod both are still there, but `hh_cat_new.cct` there is the older build without the fixes (staged build: sha1
`1c7182ee`; upload that one): the casts in `dcr/latest/` (`hh_cat_new.cct` sha1 `c6114aee`,
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
node out/cli/addScript.js /tmp/cn.cct compat.ls "Compat API" v31/fuse_client.cct "Variable API" /tmp/cn1.cct
node out/cli/addScript.js /tmp/cn1.cct strip_fix.ls "Product Strip Fix Class" /tmp/cn1.cct "Product Strip Default Class" /tmp/cn2.cct
node out/cli/replaceText.js /tmp/cn2.cct /tmp/cn3.cct variable.index \
  'layout.class.default.productstrip=["Product Strip Base Class", "Product Strip Default Class"]' \
  'layout.class.default.productstrip=["Product Strip Base Class", "Product Strip Fix Class", "Product Strip Default Class"]'
#   (the same for layout.class.cars.productstrip, before "Product Strip Combo Class")
node out/cli/addScript.js /tmp/cn4.cct oldpage_fix.ls "Catalogpage Oldpage Fix Class" /tmp/cn4.cct "Catalogpage Oldpage Wrapper Class" /tmp/cn5.cct
#   then for L in plasto spaces trophies pets recycler soundmachine purse: replaceText layout.class.$L=[... "Catalogpage
#   Oldpage Wrapper Class"] to [... "Catalogpage Oldpage Wrapper Class", "Catalogpage Oldpage Fix Class"] -> hh_cat_new.cct
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
folders, pages, buying). Page-by-page comparison old vs v31 (all 83 pages, local, with the r31 graphics fixes): no
rendering breakage; it found the loading icons on Start pakker and Trax (fixed above). Patrick found the wallpaper
picker broken on prod (shared sale codes, fixed above); after the fixes the picker pages through patterns and colours
and buying a wallpaper gives the picked pattern (`1501` for pattern 15, model 1; purchase log: that catalogue item).
