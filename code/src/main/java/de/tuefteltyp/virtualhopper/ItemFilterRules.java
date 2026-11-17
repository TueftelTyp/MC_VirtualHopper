package de.tuefteltyp.virtualhopper;

/** Pure item-ID matching rules, shared by both filters. Empty entries are ignored. */
public final class ItemFilterRules {
    private ItemFilterRules() {}

    public static boolean allows(String itemId, boolean enabled, boolean blacklist, String[] templates) {
        if (!enabled) return true;
        boolean present = false;
        if (itemId != null && templates != null) {
            for (String template : templates) {
                if (!itemId.isBlank() && itemId.equals(template)) {
                    present = true;
                    break;
                }
            }
        }
        return blacklist != present;
    }
}
