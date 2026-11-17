package de.tuefteltyp.virtualhopper.client;

import de.tuefteltyp.virtualhopper.VirtualHopperBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Two independent, nine-template filters. Template clicks copy the held item's ID;
 *  no items are taken from the inventory or consumed. All changes are verified server-side.
 */
public final class VirtualHopperFilterScreen extends Screen {
    private final BlockPos pos;
    private final String[][] templates = new String[2][9];
    private final boolean[] enabled = new boolean[2];
    private final boolean[] blacklist = new boolean[2];
    private final Button[] templateButtons = new Button[9];
    private Button toggleButton;
    private Button modeButton;
    private int page = 0;
    private int selected = 0;
    private String statusKey = "";
    private boolean populated;

    public VirtualHopperFilterScreen(BlockPos pos) {
        super(Component.translatable("screen.virtualhopper.filter.title"));
        this.pos = pos;
        for (String[] row : templates) java.util.Arrays.fill(row, "");
    }

    private String commandPrefix() {
        return "virtualhopper filter " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + (page == 0 ? " pickup " : " send ");
    }

    private void send(String property, String value) {
        if (minecraft != null && minecraft.getConnection() != null)
            minecraft.getConnection().sendCommand(commandPrefix() + property + " " + value);
        statusKey = "screen.virtualhopper.filter.saved";
    }

    private void readSavedState() {
        if (populated || minecraft == null || minecraft.level == null) return;
        if (minecraft.level.getBlockEntity(pos) instanceof VirtualHopperBlockEntity hopper) {
            for (int p = 0; p < 2; p++) {
                boolean pickup = p == 0;
                enabled[p] = hopper.isFilterEnabled(pickup);
                blacklist[p] = hopper.isFilterBlacklist(pickup);
                for (int i = 0; i < 9; i++) templates[p][i] = hopper.getFilterItem(pickup, i);
            }
        }
        populated = true;
    }

    @Override protected void init() {
        readSavedState();
        int x = width / 2 - 154;
        int y = height / 2 - 101;
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.pickup"), b -> {
            page = 0;
            refreshButtons();
        }).bounds(x + 6, y + 29, 145, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.send"), b -> {
            page = 1;
            refreshButtons();
        }).bounds(x + 157, y + 29, 145, 20).build());
        toggleButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            enabled[page] = !enabled[page];
            send("enabled", enabled[page] ? "on" : "off");
            refreshButtons();
        }).bounds(x + 6, y + 57, 145, 20).build());
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            blacklist[page] = !blacklist[page];
            send("mode", blacklist[page] ? "blacklist" : "whitelist");
            refreshButtons();
        }).bounds(x + 157, y + 57, 145, 20).build());
        for (int i = 0; i < 9; i++) {
            final int index = i;
            templateButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> choose(index))
                    .bounds(x + 6 + i * 33, y + 91, 30, 30).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.clear"), b -> {
            templates[page][selected] = "";
            send("slot" + (selected + 1), "none");
            refreshButtons();
        }).bounds(x + 6, y + 158, 145, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.back"), b -> onClose())
                .bounds(x + 157, y + 158, 145, 20).build());
        refreshButtons();
    }

    private void choose(int index) {
        selected = index;
        ItemStack held = minecraft == null || minecraft.player == null
                ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
        if (held.isEmpty()) {
            statusKey = "screen.virtualhopper.filter.noitem";
        } else {
            String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            templates[page][index] = id;
            send("slot" + (index + 1), id);
        }
        refreshButtons();
    }

    private void refreshButtons() {
        if (toggleButton == null || modeButton == null) return;
        toggleButton.setMessage(Component.translatable(enabled[page]
                ? "screen.virtualhopper.filter.enable_on" : "screen.virtualhopper.filter.enable_off"));
        modeButton.setMessage(Component.translatable(blacklist[page]
                ? "screen.virtualhopper.filter.blacklist" : "screen.virtualhopper.filter.whitelist"));
        for (int i = 0; i < 9; i++) {
            String id = templates[page][i];
            String shortName = id.isBlank() ? "+" : id.substring(id.indexOf(':') + 1);
            if (shortName.length() > 3) shortName = shortName.substring(0, 3);
            templateButtons[i].setMessage(Component.literal(shortName));
        }
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperScreen(pos));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = width / 2 - 154;
        int y = height / 2 - 101;
        graphics.fill(x - 8, y - 8, x + 310, y + 205, 0xF0091722);
        graphics.outline(x - 8, y - 8, 318, 213, 0xFF14CFE2);
        graphics.text(font, title, x + 6, y + 5, 0xFFCEFFFF, true);
        graphics.text(font, Component.translatable("screen.virtualhopper.filter.selected").append(" " + (selected + 1)),
                x + 6, y + 131, 0xFFD9FBFF, false);
        String id = templates[page][selected];
        graphics.text(font, Component.literal(id.isBlank() ? "-" : id), x + 110, y + 131, 0xFFB4D3DF, false);
        graphics.text(font, Component.translatable("screen.virtualhopper.filter.help"), x + 6, y + 147, 0xFFACC2CF, false);
        if (!statusKey.isBlank()) graphics.text(font, Component.translatable(statusKey),
                x + 6, y + 187, 0xFFAFE8CE, false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
