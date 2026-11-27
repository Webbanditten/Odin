package org.alexdev.kepler.game.catalogue;

import org.alexdev.kepler.util.StringUtil;
import org.alexdev.kepler.util.config.GameConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CataloguePage {
    private int id;
    private String fuse;
    private boolean indexVisible;
    private boolean isClubOnly;
    private String nameIndex;
    private String linkList;
    private String name;
    private String layout;
    private String imageHeadline;
    private String imageTeasers;
    private String body;
    private String labelPick;
    private String labelExtra_s;
    private Map<String, String> labelExtra;

    // The v31 catalogue (hh_cat_new): a tree of pages with a colour and an icon per node.
    private int parentId = -1;
    /** Place among its siblings in the v31 tree (tree_order); 0 = not placed: after the placed ones, by order_id. The
     *  old catalogue's flat index goes by order_id alone, so arranging the tree never moves its pages. */
    private int treeOrder;
    private int icon;
    private int colour;
    private String layoutV31;

    public CataloguePage(int id, String fuse, boolean indexVisible, boolean isClubOnly, String nameIndex, String linkedList, String name, String layout, String imageHeadline, String imageTeasers, String body, String labelPick, String labelExtra_s, String labelExtra_t) {
        this.id = id;
        this.fuse = fuse;
        this.indexVisible = indexVisible;
        this.isClubOnly = isClubOnly;
        this.nameIndex = nameIndex;
        this.linkList = linkedList;
        this.name = name;
        this.layout = layout;
        this.imageHeadline = imageHeadline;
        this.imageTeasers = imageTeasers;
        this.body = body;
        this.labelPick = labelPick;
        this.labelExtra_s = labelExtra_s;
        this.labelExtra = new HashMap<>();

        if (!StringUtil.isNullOrEmpty(labelExtra_t)) {
            String[] extraTypedData = labelExtra_t.split("\r\n");

            for (String dbExtraData : extraTypedData) {
                String extraData = StringUtil.filterInput(dbExtraData, true);

                String extraId = extraData.substring(0, extraData.indexOf(':'));
                String data = extraData.substring(extraId.length() + 1);

                this.labelExtra.put("label_extra_t_" + extraId, data);
            }
        }
    }

    public void setTree(int parentId, int treeOrder, int icon, int colour, String layoutV31) {
        this.parentId = parentId;
        this.treeOrder = treeOrder;
        this.icon = icon;
        this.colour = colour;
        this.layoutV31 = layoutV31;
    }

    public int getParentId() {
        return parentId;
    }

    public int getTreeOrder() {
        return treeOrder;
    }

    /** A page without a layout is a folder in the v31 index: it opens its children, not a page. */
    public boolean isNavigatable() {
        return !StringUtil.isNullOrEmpty(this.layout) || !StringUtil.isNullOrEmpty(this.layoutV31);
    }

    public int getIcon() {
        return icon;
    }

    public int getColour() {
        return colour;
    }

    // ---- The page as the v31 catalogue reads it (CATALOGUE_PAGE: layout, images[], texts[]) ----

    /** Field order per layout, from hh_cat_new's variable index (layout.fields.*); the rest use "default". */
    private static final Map<String, List<String>> V31_TEXT_FIELDS = Map.of(
            "default", List.of("ctlg_header_text", "ctlg_description", "ctlg_special_txt", "ctlg_text_1", "ctlg_text_2"),
            "camera1", List.of("ctlg_header_text", "ctlg_text_1"),
            "club1", List.of("ctlg_text_1", "ctlg_text_2", "ctlg_text_3", "ctlg_text_4"),
            "club2", List.of("ctlg_text_1", "ctlg_text_2", "ctlg_text_3", "ctlg_text_4"),
            "presents", List.of("ctlg_header_text", "ctlg_text1"),
            "collectibles", List.of("ctlg_header_text", "ctlg_collectibles_link"),
            "purse", List.of("ctlg_header_text", "ctlg_special_txt"),
            "pets2", List.of("ctlg_header_text", "ctlg_text_1", "ctlg_text_2", "ctlg_text_3"),
            "frontpage3", List.of("ctlg_txt1", "ctlg_txt2", "ctlg_txt3", "ctlg_txt4", "ctlg_txt5", "ctlg_txt6"));
    private static final Map<String, List<String>> V31_IMAGE_FIELDS = Map.of(
            "default", List.of("ctlg_header_img", "ctlg_teaserimg_1", "ctlg_special_img", "ctlg_teaserimg_2", "ctlg_teaserimg_3"),
            "frontpage3", List.of("ctlg_header_img", "ctlg_special_img"));

    /** layout_v31 when set, else the old layout without "ctlg_"; product layouts become v31's default_3x3. */
    public String getV31Layout() {
        if (!StringUtil.isNullOrEmpty(this.layoutV31)) {
            return this.layoutV31;
        }

        String old = this.layout == null ? "" : this.layout;

        if (old.startsWith("ctlg_")) {
            old = old.substring(5);
        }

        // v31 shows products in default_3x3's product strip; its window for the old product layouts has no product
        // texts, so those pages use default_3x3 like the v31 hotels did.
        return switch (old) {
            case "layout1", "layout2", "productpage1", "productpage2", "productpage2b", "productpage3", "productpage3b" -> "default_3x3";
            default -> old;
        };
    }

    public List<String> getV31Images() {
        List<String> fields = V31_IMAGE_FIELDS.getOrDefault(this.getV31Layout(), V31_IMAGE_FIELDS.get("default"));
        String[] teasers = StringUtil.isNullOrEmpty(this.imageTeasers) ? new String[0] : this.imageTeasers.split(",");
        List<String> images = new ArrayList<>();

        for (String field : fields) {
            String value = "";

            if (field.equals("ctlg_header_img")) {
                value = libraryName(this.imageHeadline);
            } else if (field.equals("ctlg_special_img")) {
                value = StringUtil.isNullOrEmpty(this.labelExtra_s) ? "" : "catalog_special_txtbg" + this.getSpecialType();
            } else if (field.startsWith("ctlg_teaserimg_")) {
                int n = Integer.parseInt(field.substring("ctlg_teaserimg_".length()));
                value = teasers.length >= n ? libraryName(teasers[n - 1].trim()) : "";
            }

            images.add(value == null ? "" : value);
        }

        return trimTrailingEmpty(images);
    }

    public List<String> getV31Texts() {
        List<String> fields = V31_TEXT_FIELDS.getOrDefault(this.getV31Layout(), V31_TEXT_FIELDS.get("default"));
        List<String> texts = new ArrayList<>();

        for (String field : fields) {
            String value = switch (field) {
                case "ctlg_header_text" -> this.body;
                case "ctlg_description" -> this.labelPick;
                case "ctlg_special_txt" -> this.getSpecialText();
                case "ctlg_collectibles_link" -> this.labelExtra.get("label_extra_t_1");
                default -> {
                    // ctlg_text_N, ctlg_textN and ctlg_txtN are label_extra_t's numbered texts
                    String digits = field.replaceAll("\\D", "");
                    yield digits.isEmpty() ? "" : this.labelExtra.get("label_extra_t_" + digits);
                }
            };

            // The client turns a literal backslash-n into RETURN (Lingo strings have no escapes).
            texts.add(value == null ? "" : value.replace("<br>", "\\n").replace("\r\n", "\\n").replace("\r", "\\n"));
        }

        return trimTrailingEmpty(texts);
    }

    /**
     * The v31 catalogue uses a cast member with the image's name if there is one, else downloads "<name>.gif" from the
     * image library. Library images carry the language: "<name>_<language>". (The old catalogue added it itself.)
     */
    private static String libraryName(String image) {
        if (StringUtil.isNullOrEmpty(image) || !CatalogueManager.getInstance().getLibraryImages().contains(image)) {
            return image;
        }

        return image + "_" + GameConfiguration.getInstance().getString("catalogue.v31.image.language", "da");
    }

    /** label_extra_s is "type:text" (the client shows the last item, on catalog_special_txtbg<type>). */
    private int getSpecialType() {
        try {
            return Integer.parseInt(this.labelExtra_s.split(":")[0].trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private String getSpecialText() {
        if (StringUtil.isNullOrEmpty(this.labelExtra_s)) {
            return "";
        }

        String[] parts = this.labelExtra_s.split(":");
        return parts[parts.length - 1].trim();
    }

    private static List<String> trimTrailingEmpty(List<String> values) {
        while (!values.isEmpty() && values.get(values.size() - 1).isEmpty()) {
            values.remove(values.size() - 1);
        }

        return values;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFuse() {
        return fuse;
    }

    public boolean isIndexVisible() {
        return indexVisible;
    }

    public String getNameIndex() {
        return nameIndex;
    }

    public String getLinkList() {
        return linkList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLayout() {
        return layout;
    }

    public String getImageHeadline() {
        return imageHeadline;
    }

    public String getImageTeasers() {
        return imageTeasers == null ? "" : imageTeasers;
    }

    public String getBody() {
        return body == null ? "" : body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLabelPick() {
        return labelPick;
    }

    public String getLabelExtra_s() {
        return labelExtra_s;
    }

    public Map<String, String> getLabelExtra() {
        return labelExtra;
    }

    public boolean isClubOnly() {
        return isClubOnly;
    }
}
