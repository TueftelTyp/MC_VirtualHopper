import de.tuefteltyp.virtualhopper.ItemFilterRules;

public final class ItemFilterRulesSmokeTest {
    private static void check(boolean actual, boolean expected, String label) {
        if (actual != expected) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        String[] slots = {"minecraft:diamond", "minecraft:iron_ingot", "", "", "", "", "", "", ""};
        check(ItemFilterRules.allows("minecraft:dirt", false, false, slots), true, "off whitelist");
        check(ItemFilterRules.allows("minecraft:dirt", false, true, slots), true, "off blacklist");
        check(ItemFilterRules.allows("minecraft:diamond", true, false, slots), true, "allowed whitelist");
        check(ItemFilterRules.allows("minecraft:dirt", true, false, slots), false, "rejected whitelist");
        check(ItemFilterRules.allows("minecraft:diamond", true, true, slots), false, "blocked blacklist");
        check(ItemFilterRules.allows("minecraft:dirt", true, true, slots), true, "allowed blacklist");
        check(ItemFilterRules.allows("minecraft:dirt", true, false, new String[9]), false, "empty whitelist");
        check(ItemFilterRules.allows("minecraft:dirt", true, true, new String[9]), true, "empty blacklist");
        check(ItemFilterRules.allows("minecraft:diamond", true, false, new String[] {"minecraft:diamond", "minecraft:diamond"}), true, "duplicate entries");
        System.out.println("All 9 filter rule checks passed.");
    }
}
