package de.tuefteltyp.virtualhopper.client;

import de.tuefteltyp.virtualhopper.VirtualHopperBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** One authoritative nine-slot destination list for the Virtual Hopper.
 *  A legacy single-target entry is deliberately absent to avoid overwriting slot 1. */
public final class VirtualHopperTargetsScreen extends Screen {
    private final BlockPos pos;
    private final String[] targets = new String[9];
    private final Button[] targetButtons = new Button[9];
    private EditBox editor;
    private Button modeButton;
    private int selected = 0;
    private int mode = 0;
    private String status = "";

    public VirtualHopperTargetsScreen(BlockPos pos) {
        super(Component.translatable("screen.virtualhopper.targets.title"));
        this.pos = pos;
        java.util.Arrays.fill(targets, "");
    }
    private String coordinates() { return pos.getX() + " " + pos.getY() + " " + pos.getZ(); }
    private void send(String command) {
        if (minecraft != null && minecraft.getConnection() != null)
            minecraft.getConnection().sendCommand("virtualhopper " + command);
    }
    @Override protected void init() {
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof VirtualHopperBlockEntity hopper) {
            mode = hopper.getDistributionMode();
            for (int i = 0; i < 9; i++) targets[i] = hopper.getTargetName(i);
        }
        // Three rows of three slots: every configured destination stays visible.
        int panelWidth = Math.min(330, width - 20);
        int x = (width - panelWidth) / 2;
        int y = Math.max(8, (height - 228) / 2);
        int gap = 4, buttonWidth = (panelWidth - 2 * gap) / 3;
        for (int i = 0; i < 9; i++) {
            final int index = i;
            targetButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                selected = index;
                editor.setValue(targets[selected]);
                refresh();
            }).bounds(x + (i % 3) * (buttonWidth + gap), y + 23 + (i / 3) * 23,
                    buttonWidth, 20).build());
        }
        int controlY = y + 100;
        editor = addRenderableWidget(new EditBox(font, x, controlY, panelWidth - 93, 19,
                Component.translatable("screen.virtualhopper.targets.entry")));
        editor.setMaxLength(32);
        editor.setValue(targets[selected]);
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.targets.save"), b -> {
            String value = editor.getValue().trim();
            if (!value.isEmpty() && !value.matches("[a-zA-Z0-9_-]{1,32}")) {
                status = "screen.virtualhopper.invalid";
                return;
            }
            send("targetslot " + coordinates() + " " + (selected + 1) + " " + (value.isEmpty() ? "none" : value));
            // Do not optimistically claim success: only the server can approve a destination.
            status = "screen.virtualhopper.targets.verify";
            send("targets " + coordinates());
        }).bounds(x + panelWidth - 89, controlY, 89, 20).build());
        modeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            mode = (mode + 1) % 3;
            send("distribution " + coordinates() + " " + new String[]{"round_robin", "priority", "balanced"}[mode]);
            refresh();
        }).bounds(x, y + 125, panelWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.targets.filter"), b -> {
            if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperTargetFilterScreen(pos, selected));
        }).bounds(x, y + 152, panelWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.targets.status"), b -> {
            send("targets " + coordinates());
            status = "screen.virtualhopper.targets.chat";
        }).bounds(x, y + 179, (panelWidth - gap)/2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.virtualhopper.filter.back"), b -> onClose())
            .bounds(x + (panelWidth + gap)/2, y + 179, (panelWidth - gap)/2, 20).build());
        refresh();
    }
    private void refresh() {
        if (modeButton == null) return;
        for (int i=0; i<9; i++) {
            String name = targets[i];
            // Avoid clipping the names in the compact 3-column list.
            String label = (i+1) + ": " + (name.isEmpty() ? "-" : name.length() > 10 ? name.substring(0, 9) + "…" : name);
            targetButtons[i].setMessage(Component.literal((i == selected ? "[" : "") + label + (i == selected ? "]" : "")));
        }
        modeButton.setMessage(Component.translatable("screen.virtualhopper.targets.mode." + mode));
    }
    @Override public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(new VirtualHopperScreen(pos));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(330, width - 20);
        int x = (width - panelWidth) / 2;
        int y = Math.max(8, (height - 228) / 2);
        graphics.fill(x-8, y-8, x+panelWidth+8, y+229, 0xF0091722);
        graphics.outline(x-8, y-8, panelWidth+16, 237, 0xFF14CFE2);
        graphics.text(font, title, x+2, y+2, 0xFFCEFFFF, true);
        graphics.text(font, Component.translatable("screen.virtualhopper.targets.selected", selected+1),
                x, y+94, 0xFFD8F9FF, false);
        // Reflect server-confirmed changes instead of optimistically claiming that saving worked.
        if (minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof VirtualHopperBlockEntity hopper) {
            boolean changed = false;
            for (int i = 0; i < 9; i++) {
                String actual = hopper.getTargetName(i);
                if (!targets[i].equals(actual)) { targets[i] = actual; changed = true; }
            }
            if (changed) refresh();
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (!status.isEmpty()) graphics.text(font, Component.translatable(status), x, y+208, 0xFFBDF39D, false);
    }
}
